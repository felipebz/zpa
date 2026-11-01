//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utplsql

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.utils.log.Loggers
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.notifications.AnalysisWarnings
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
