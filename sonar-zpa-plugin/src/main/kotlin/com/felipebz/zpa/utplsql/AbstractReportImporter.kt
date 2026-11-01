//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utplsql

import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.WildcardPattern
import java.io.File

abstract class AbstractReportImporter(private val analysisWarnings: AnalysisWarnings) {

    private val logger: Logger = Loggers.getLogger(AbstractReportImporter::class.java)

    abstract val reportType: String
    abstract val reportKey: String

    abstract fun processReport(context: SensorContext, report: File)

    fun execute(context: SensorContext) {
        val reports = files(context)

        for (report in reports) {
            logger.info("Processing $reportType report {}", report)
            processReport(context, report)
        }
    }

    protected fun files(context: SensorContext) =
        context.config().getStringArray(reportKey).flatMap {
            getReports(context, it)
        }

    private fun getReports(context: SensorContext, reportPath: String): List<File> {
        val pattern = WildcardPattern.create(reportPath)
        val baseDir = context.fileSystem().baseDir()
        val matchingFiles = baseDir
                .walkTopDown()
                .filter { it.isFile && pattern.match(it.relativeTo(baseDir).invariantSeparatorsPath) }
                .toMutableList()

        if (matchingFiles.isEmpty()) {
            if (context.config().hasKey(reportKey)) {
                val file = File(reportPath)
                if (!file.exists()) {
                    val formattedMessage =
                        "No utPLSQL $reportType report was found for ${reportKey} using pattern $reportPath"
                    logger.warn(formattedMessage)
                    analysisWarnings.addUnique(formattedMessage)
                } else {
                    matchingFiles.add(file)
                }
            } else {
                logger.info("No utPLSQL $reportType report was found for {} using default pattern {}",
                    reportKey, reportPath)
            }
        }
        return matchingFiles
    }

}
