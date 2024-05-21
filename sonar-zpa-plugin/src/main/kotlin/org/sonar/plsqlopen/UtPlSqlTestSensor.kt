/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen

import org.simpleframework.xml.core.Persister
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.WildcardPattern
import org.sonar.plsqlopen.squid.SonarQubePlSqlFile
import org.sonar.plsqlopen.symbols.FileLocator
import org.sonar.plsqlopen.utils.log.Logger
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.plsqlopen.utplsql.TestExecutions
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File
import java.io.Serializable


class UtPlSqlTestSensor(private val conf: Configuration,
                        private val fileLocator: FileLocator,
                        private val analysisWarnings: AnalysisWarnings) : Sensor {

    private val logger: Logger = Loggers.getLogger(UtPlSqlTestSensor::class.java)

    override fun describe(descriptor: SensorDescriptor) {
        descriptor.name("Z PL/SQL Analyzer - utPLSQL Test Report Importer").onlyOnLanguage(PlSql.KEY)
    }

    override fun execute(context: SensorContext) {
        val reports = conf.getStringArray(REPORT_PATH_KEY).flatMap {
            getReports(context.fileSystem().baseDir().path, it)
        }

        for (report in reports) {
            val testExecutions = readTestExecutionsReport(report)
            logger.info("Processing test report {}", report)
            processTestExecutions(context, testExecutions)
        }
    }

    private fun getReports(baseDirPath: String, reportPath: String): List<File> {
        val pattern = WildcardPattern.create(reportPath)
        val baseDir = File(baseDirPath).absoluteFile
        val matchingFiles = baseDir
                .walkTopDown()
                .filter { it.isFile && pattern.match(baseDir.relativeTo(it).invariantSeparatorsPath) }
                .toMutableList()

        if (matchingFiles.isEmpty()) {
            if (conf.hasKey(REPORT_PATH_KEY)) {
                val file = File(reportPath)
                if (!file.exists()) {
                    val formattedMessage =
                        String.format("No utPLSQL test report was found for %s using pattern %s", REPORT_PATH_KEY, reportPath)
                    logger.warn(formattedMessage)
                    analysisWarnings.addUnique(formattedMessage)
                } else {
                    matchingFiles.add(file)
                }
            } else {
                logger.info("No utPLSQL test report was found for {} using default pattern {}", REPORT_PATH_KEY, reportPath)
            }
        }
        return matchingFiles
    }

    private fun readTestExecutionsReport(file: File): TestExecutions {
        val serializer = Persister()
        return serializer.read(TestExecutions::class.java, file)
    }

    private fun processTestExecutions(context: SensorContext, testExecutions: TestExecutions) {
        testExecutions.files?.forEach { file ->
            val mappedTest = fileLocator.declaredScopes.firstOrNull {
                it.plSqlFile != null &&
                it.plSqlFile?.type() === PlSqlFile.Type.TEST &&
                it.identifier?.equals(file.path, ignoreCase = true) == true &&
                it.type === PlSqlGrammar.CREATE_PACKAGE_BODY
            }

            if (mappedTest != null) {
                var testCount = 0
                var failureCount = 0
                var errorCount = 0
                var skippedCount = 0
                var duration = 0L

                file.testCases?.forEach { testCase ->
                    testCount++
                    if (testCase.failure != null) {
                        failureCount++
                    } else if (testCase.error != null) {
                        errorCount++
                    } else if (testCase.skipped != null) {
                        skippedCount++
                    }
                    duration += testCase.duration
                }

                val plSqlFile = (mappedTest.plSqlFile as SonarQubePlSqlFile).inputFile
                saveMetricOnFile(context, plSqlFile, CoreMetrics.TESTS, testCount)
                saveMetricOnFile(context, plSqlFile, CoreMetrics.TEST_FAILURES, failureCount)
                saveMetricOnFile(context, plSqlFile, CoreMetrics.TEST_ERRORS, errorCount)
                saveMetricOnFile(context, plSqlFile, CoreMetrics.SKIPPED_TESTS, skippedCount)
                saveMetricOnFile(context, plSqlFile, CoreMetrics.TEST_EXECUTION_TIME, duration)
            }
        }
    }
    private fun <T : Serializable> saveMetricOnFile(context: SensorContext, inputFile: InputFile, metric: Metric<T>, value: T) {
        context.newMeasure<T>()
               .on(inputFile)
               .forMetric(metric)
               .withValue(value)
               .save()
    }

    companion object {
        const val REPORT_PATH_KEY = "sonar.zpa.utplsql.reportPaths"
        const val DEFAULT_REPORT_PATH = "utplsql-test.xml"
    }

}
