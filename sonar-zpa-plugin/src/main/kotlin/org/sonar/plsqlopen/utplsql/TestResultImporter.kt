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

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.plsqlopen.symbols.ObjectLocator
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File
import java.io.Serializable

class TestResultImporter(private val objectLocator: ObjectLocator,
                         analysisWarnings: AnalysisWarnings) : AbstractReportImporter(analysisWarnings) {

    private val logger = Loggers.getLogger(TestResultImporter::class.java)
    override val reportType = "test"
    override val reportKey = UtPlSqlSensor.TEST_REPORT_PATH_KEY

    override fun processReport(context: SensorContext, report: File) {
        val serializer = XmlMapper()
        val testExecutions = serializer.readValue(report, TestExecutions::class.java)

        testExecutions.files?.forEach { file ->
            val packageName = file.path.substringAfterLast('.')
            val mappedTest = objectLocator.findTestObject(packageName, PlSqlGrammar.CREATE_PACKAGE)
            val inputFile = mappedTest?.inputFile ?: context.fileSystem()
                .inputFile(context.fileSystem().predicates().hasPath(file.path))

            if (inputFile != null) {
                logger.debug("The path ${file.path} was mapped to ${inputFile}")
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
            } else {
                logger.warn("The path ${file.path} was not found in the project")
            }
        }
    }

    private fun <T : Serializable> saveMetricOnFile(
        context: SensorContext,
        inputFile: InputFile,
        metric: Metric<T>,
        value: T
    ) {
        context.newMeasure<T>()
            .on(inputFile)
            .forMetric(metric)
            .withValue(value)
            .save()
    }

}
