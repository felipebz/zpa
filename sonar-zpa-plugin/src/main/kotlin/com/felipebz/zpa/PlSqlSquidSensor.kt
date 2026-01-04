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

import org.sonar.api.batch.rule.ActiveRules
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.rules.SonarQubeActiveRulesAdapter
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import com.felipebz.zpa.squid.PlSqlAstScanner
import com.felipebz.zpa.squid.ProgressReport
import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
import com.felipebz.zpa.api.ZpaRulesDefinition
import java.util.concurrent.TimeUnit

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

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.name("ZPA").onlyOnLanguage(PlSql.Companion.KEY)
    }

    override fun execute(context: SensorContext) {
        val fs = context.fileSystem()
        val inputFiles = fs.inputFiles(fs.predicates().hasLanguage(PlSql.Companion.KEY)).toList()

        val progressReport = ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10))
        val scanner = PlSqlAstScanner(context, checks, noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory, objectLocator)

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

        progressReport.stop()
    }

}
