/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.utplsql

import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.api.utils.WildcardPattern
import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
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
