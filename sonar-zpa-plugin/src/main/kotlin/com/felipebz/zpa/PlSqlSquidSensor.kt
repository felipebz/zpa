//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.api.ZpaRulesDefinition
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalContext
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.api.project.ProjectSchemaPaths
import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.oracle.JdbcOracleMetadataAcquirer
import com.felipebz.zpa.oracle.OracleMetadataAcquirer
import com.felipebz.zpa.oracle.OracleMetadataSettings
import com.felipebz.zpa.project.*
import com.felipebz.zpa.rules.SonarQubeActiveRulesAdapter
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import com.felipebz.zpa.squid.*
import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
import org.sonar.api.batch.rule.ActiveRules
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.utils.MessageException
import java.util.concurrent.TimeUnit

@OptIn(ZpaExperimentalApi::class)
class PlSqlSquidSensor @JvmOverloads constructor(activeRules: ActiveRules, settings: Configuration, private val noSonarFilter: NoSonarFilter,
                                                 private val fileLinesContextFactory: FileLinesContextFactory,
                                                 customRulesDefinition: Array<ZpaRulesDefinition>? = null,
                                                 private val objectLocator: ObjectLocator) : Sensor {

    private val logger: Logger = Loggers.getLogger(PlSqlSquidSensor::class.java)

    private val checks = PlSqlChecks.createPlSqlCheck(SonarQubeActiveRulesAdapter(activeRules), SonarQubeRuleMetadataLoader())
            .addChecks(PlSqlRuleRepository.Companion.KEY, CheckList.checks)
            .addCustomChecks(customRulesDefinition)

    private val isErrorRecoveryEnabled = settings.getBoolean(PlSqlPlugin.Companion.ERROR_RECOVERY_KEY).orElse(false)
    private val isConcurrentModeEnabled = settings.getBoolean(PlSqlPlugin.Companion.CONCURRENT_EXECUTION_KEY).orElse(true)

    private val formsMetadata = FormsMetadata.loadFromFile(settings.get(PlSqlPlugin.Companion.FORMS_METADATA_KEY)
        .orElse(null))

    private val configuration = settings

    /** Scanner process environment, from which the database password is read. Replaced by tests only. */
    internal var environment: (String) -> String? = System::getenv

    /** Database access, used only when Oracle metadata is configured. Replaced by tests only. */
    internal var oracleMetadataAcquirer: OracleMetadataAcquirer = JdbcOracleMetadataAcquirer()

    private class CompilationProperties(val flags: Map<String, CcValue>?, val version: DatabaseVersion?)

    private fun compilationProperties(): CompilationProperties {
        fun read(key: String) = configuration.get(key).orElse(null)?.takeIf { it.isNotBlank() }
        val flags = try {
            read(PlSqlPlugin.CCFLAGS_KEY)?.let { ConditionalCompilationSettings.parseFlags(it) }
        } catch (e: IllegalArgumentException) {
            throw MessageException.of("Invalid value of ${PlSqlPlugin.CCFLAGS_KEY}: ${e.message}")
        }
        val version = try {
            read(PlSqlPlugin.DATABASE_VERSION_KEY)?.let { ConditionalCompilationSettings.parseVersion(it) }
        } catch (e: IllegalArgumentException) {
            throw MessageException.of("Invalid value of ${PlSqlPlugin.DATABASE_VERSION_KEY}: ${e.message}")
        }
        return CompilationProperties(flags, version)
    }

    private fun conditionalCompilationSettings(properties: CompilationProperties, database: DatabaseVersion?): ConditionalCompilationSettings {
        if (properties.version != null && database != null && properties.version != database) {
            logger.warn("ZPA conditional compilation: ${PlSqlPlugin.DATABASE_VERSION_KEY} is ${show(properties.version)}, but the " +
                "database's DBMS_DB_VERSION is ${show(database)}; using ${show(properties.version)}")
        }
        val version = properties.version ?: database
        val configured = properties.flags != null || properties.version != null
        val settings = ConditionalCompilationSettings(properties.flags.orEmpty(), version, unsetFlagsAreNull = configured)
        if (settings.isActive) {
            val source = when {
                version == null -> ""
                properties.version != null -> " (database version from ${PlSqlPlugin.DATABASE_VERSION_KEY})"
                else -> " (database version from the database)"
            }
            logger.info("ZPA conditional compilation: $settings$source")
        }
        return settings
    }

    private fun show(version: DatabaseVersion) = "${version.version}.${version.release}"

    /** `sonar.zpa.schemaPaths`, relative to the project base directory; an invalid value fails the analysis. */
    private fun schemaPaths(baseDirectory: java.nio.file.Path): ProjectSchemaPaths {
        val text = configuration.get(PlSqlPlugin.SCHEMA_PATHS_KEY).orElse("")
        val paths = try {
            ProjectSchemaPaths.parse(baseDirectory, text)
        } catch (e: IllegalArgumentException) {
            throw MessageException.of("Invalid value of ${PlSqlPlugin.SCHEMA_PATHS_KEY}: ${e.message}")
        }
        if (paths.entries.isNotEmpty()) logger.info("ZPA schema paths: $paths")
        return paths
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.name("ZPA").onlyOnLanguage(PlSql.Companion.KEY)
    }

    override fun execute(context: SensorContext) {
        val fs = context.fileSystem()
        val inputFiles = fs.inputFiles(fs.predicates().hasLanguage(PlSql.Companion.KEY)).toList()

        // Read before any work, so that an invalid value fails the analysis up front.
        val compilationProperties = compilationProperties()

        // Configured explicitly or not at all: without sonar.zpa.oracle.url nothing connects. Acquired once, in the
        // scanner, before preparation and scanning; failures (MessageException, sanitized) fail the analysis.
        val oracleSettings = OracleMetadataSettings.from(configuration, environment, fs.baseDir().toPath())
        val compilationSchema = OracleMetadataSettings.compilationSchema(configuration)
        val schemaPaths = schemaPaths(fs.baseDir().toPath())
        val acquired = oracleSettings?.let { settings ->
            oracleMetadataAcquirer.acquire(settings).also {
                logger.info("ZPA Oracle metadata: ${it.summary}")
                it.databaseVersionFailure?.let { reason -> logger.info("ZPA Oracle metadata: DBMS_DB_VERSION could not be read: $reason") }
            }
        }
        val externalMetadata = acquired?.metadata

        val conditionalCompilation = conditionalCompilationSettings(compilationProperties, acquired?.databaseVersion)
        val lexerConfiguration = PlSqlConfiguration(fs.encoding(), isErrorRecoveryEnabled, conditionalCompilation)

        val progressReport = ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10))
        // The schemas each file compiles in: the prepared result carries them, so the scan's lookups use the same ones.
        val schemas = if (compilationSchema == null && schemaPaths.entries.isEmpty()) ProjectExternalContext.none()
        else ProjectExternalContext.of(ProjectExternalMetadata.none(), compilationSchema, schemaPaths)
        val preparedContext = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(
                ProjectDeclarationExtractor(lexerConfiguration), schemas
            ).prepare(
                inputFiles.map { inputFile ->
                    ProjectSource(
                        FileId(inputFile.uri().toString()),
                        ProjectSourceReader { inputFile.contents() }
                    )
                },
                concurrent = isConcurrentModeEnabled
            )
        )
        // Only the detached, immutable metadata reaches the scanner workers; the prepared index is shared as is.
        val projectAnalysisContext = externalMetadata?.let(preparedContext::withMetadata) ?: preparedContext
        val scanner = PlSqlAstScanner(
            context,
            checks,
            noSonarFilter,
            formsMetadata,
            isErrorRecoveryEnabled,
            fileLinesContextFactory,
            projectAnalysisContext,
            conditionalCompilation
        )

        progressReport.start(inputFiles.map { it.toString() })

        val files = if (isConcurrentModeEnabled) {
            logger.info("Concurrent mode enabled")
            inputFiles.parallelStream()
        } else {
            inputFiles.stream()
        }

        files.forEach {
            try {
                scanner.scanFile(it)
                progressReport.nextFile()
            } catch (e: Exception) {
                logger.error("Error during analysis of file $it", e)
            }
        }

        objectLocator.setObjects(scanner.objectLocations())

        progressReport.stop()
    }

}
