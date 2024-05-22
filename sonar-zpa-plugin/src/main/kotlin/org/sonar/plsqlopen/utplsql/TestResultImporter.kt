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
package org.sonar.plsqlopen.utplsql

import org.simpleframework.xml.core.Persister
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.config.Configuration
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.WildcardPattern
import org.sonar.plsqlopen.symbols.ObjectLocator
import org.sonar.plsqlopen.utils.log.Logger
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File
import java.io.Serializable

class TestResultImporter(private val conf: Configuration,
                         private val objectLocator: ObjectLocator,
                         private val analysisWarnings: AnalysisWarnings) {

    private val logger: Logger = Loggers.getLogger(TestResultImporter::class.java)

    fun execute(context: SensorContext) {
        val reports = conf.getStringArray(UtPlSqlSensor.TEST_REPORT_PATH_KEY).flatMap {
            getReports(context.fileSystem().baseDir(), it)
        }

        for (report in reports) {
            val testExecutions = readTestExecutionsReport(report)
            logger.info("Processing test report {}", report)
            processTestExecutions(context, testExecutions)
        }
    }

    private fun getReports(baseDir: File, reportPath: String): List<File> {
        val pattern = WildcardPattern.create(reportPath)
        val matchingFiles = baseDir
                .walkTopDown()
                .filter { it.isFile && pattern.match(it.relativeTo(baseDir).invariantSeparatorsPath) }
                .toMutableList()

        if (matchingFiles.isEmpty()) {
            if (conf.hasKey(UtPlSqlSensor.TEST_REPORT_PATH_KEY)) {
                val file = File(reportPath)
                if (!file.exists()) {
                    val formattedMessage =
                        String.format("No utPLSQL test report was found for %s using pattern %s", UtPlSqlSensor.TEST_REPORT_PATH_KEY, reportPath)
                    logger.warn(formattedMessage)
                    analysisWarnings.addUnique(formattedMessage)
                } else {
                    matchingFiles.add(file)
                }
            } else {
                logger.info("No utPLSQL test report was found for {} using default pattern {}", UtPlSqlSensor.TEST_REPORT_PATH_KEY, reportPath)
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
            val mappedTest = objectLocator.findTestObject(file.path, PlSqlGrammar.CREATE_PACKAGE_BODY)
            val inputFile = mappedTest?.inputFile ?:
                context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(file.path))

            if (inputFile != null) {
                file.testCases?.let { testCase ->
                    val testCount = testCase.count { it.status != TestCaseStatus.SKIPPED }
                    val failureCount = testCase.count { it.status == TestCaseStatus.FAILED }
                    val errorCount = testCase.count { it.status == TestCaseStatus.ERROR }
                    val skippedCount = testCase.count { it.status == TestCaseStatus.SKIPPED }
                    val duration = testCase.sumOf { it.duration }

                    saveMetricOnFile(context, inputFile, CoreMetrics.TESTS, testCount)
                    saveMetricOnFile(context, inputFile, CoreMetrics.TEST_FAILURES, failureCount)
                    saveMetricOnFile(context, inputFile, CoreMetrics.TEST_ERRORS, errorCount)
                    saveMetricOnFile(context, inputFile, CoreMetrics.SKIPPED_TESTS, skippedCount)
                    saveMetricOnFile(context, inputFile, CoreMetrics.TEST_EXECUTION_TIME, duration)
                }
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

}
