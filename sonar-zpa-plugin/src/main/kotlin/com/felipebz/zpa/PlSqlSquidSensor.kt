/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa

import com.felipebz.zpa.api.ZpaRulesDefinition
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalContext
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.oracle.JdbcOracleMetadataAcquirer
import com.felipebz.zpa.oracle.OracleMetadataAcquirer
import com.felipebz.zpa.oracle.OracleMetadataSettings
import com.felipebz.zpa.project.*
import com.felipebz.zpa.rules.SonarQubeActiveRulesAdapter
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlAstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.squid.ProgressReport
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

    private fun conditionalCompilationSettings(): ConditionalCompilationSettings {
        fun read(key: String) = configuration.get(key).orElse(null)
        val flags = try {
            read(PlSqlPlugin.CCFLAGS_KEY)?.takeIf { it.isNotBlank() }?.let { ConditionalCompilationSettings.parseFlags(it) }.orEmpty()
        } catch (e: IllegalArgumentException) {
            throw MessageException.of("Invalid value of ${PlSqlPlugin.CCFLAGS_KEY}: ${e.message}")
        }
        val version = try {
            read(PlSqlPlugin.DATABASE_VERSION_KEY)?.takeIf { it.isNotBlank() }?.let { ConditionalCompilationSettings.parseVersion(it) }
        } catch (e: IllegalArgumentException) {
            throw MessageException.of("Invalid value of ${PlSqlPlugin.DATABASE_VERSION_KEY}: ${e.message}")
        }
        val settings = ConditionalCompilationSettings(flags, version)
        if (settings.isActive) logger.info("ZPA conditional compilation: $settings")
        return settings
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.name("ZPA").onlyOnLanguage(PlSql.Companion.KEY)
    }

    override fun execute(context: SensorContext) {
        val fs = context.fileSystem()
        val inputFiles = fs.inputFiles(fs.predicates().hasLanguage(PlSql.Companion.KEY)).toList()

        // Read before any work, so that an invalid value fails the analysis up front. Lexing for the project index and
        // for the AST must select the same conditional compilation branches, so both get the same settings.
        val conditionalCompilation = conditionalCompilationSettings()
        val lexerConfiguration = PlSqlConfiguration(fs.encoding(), isErrorRecoveryEnabled, conditionalCompilation)

        // Configured explicitly or not at all: without sonar.zpa.oracle.url nothing connects. Acquired once, in the
        // scanner, before preparation and scanning; failures (MessageException, sanitized) fail the analysis.
        val oracleSettings = OracleMetadataSettings.from(configuration, environment, fs.baseDir().toPath())
        val compilationSchema = OracleMetadataSettings.compilationSchema(configuration)
        val externalMetadata = oracleSettings?.let { settings ->
            oracleMetadataAcquirer.acquire(settings).also { logger.info("ZPA Oracle metadata: ${it.summary}") }.metadata
        }

        val progressReport = ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10))
        val preparedContext = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(
                ProjectDeclarationExtractor(lexerConfiguration)
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
        val projectAnalysisContext = if (externalMetadata == null && compilationSchema == null) {
            preparedContext
        } else {
            preparedContext.withExternalContext(
                ProjectExternalContext.of(externalMetadata ?: ProjectExternalMetadata.none(), compilationSchema)
            )
        }
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
