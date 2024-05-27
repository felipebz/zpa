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
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.notifications.AnalysisWarnings
import org.sonar.plsqlopen.symbols.ObjectLocator
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import java.io.File


class CoverageResultImporter(private val objectLocator: ObjectLocator,
                             analysisWarnings: AnalysisWarnings) : AbstractReportImporter(analysisWarnings) {

    private val logger = Loggers.getLogger(CoverageResultImporter::class.java)
    override val reportType = "coverage"
    override val reportKey = UtPlSqlSensor.COVERAGE_REPORT_PATH_KEY

    override fun processReport(context: SensorContext, report: File) {
        val serializer = Persister()
        val coverage = serializer.read(Coverage::class.java, report)

        coverage.files?.forEach { file ->
            val filePath = file.path
            var inputFile = context.fileSystem()
                .inputFile(context.fileSystem().predicates().hasPath(filePath))

            var lineOffset = 0

            if (inputFile == null) {
                val objectType = when (filePath.substringBeforeLast(' ')) {
                    "package body" -> PlSqlGrammar.CREATE_PACKAGE_BODY
                    "procedure" -> PlSqlGrammar.CREATE_PROCEDURE
                    "function" -> PlSqlGrammar.CREATE_FUNCTION
                    "trigger" -> PlSqlGrammar.CREATE_TRIGGER
                    "type body" -> PlSqlGrammar.CREATE_TYPE_BODY
                    else -> error("Unknown object type for file \"$filePath\"")
                }
                val objectName = filePath.substringAfterLast('.')

                val mappedObject = objectLocator.findMainObject(objectName, objectType)
                if (mappedObject != null) {
                    inputFile = mappedObject.inputFile
                    lineOffset = mappedObject.firstLine - 1
                }
            }

            if (inputFile != null) {
                logger.debug("The path ${file.path} was mapped to ${inputFile}")
                saveCoverage(context, inputFile, file, lineOffset, filePath)
            } else {
                logger.warn("The path ${file.path} was not found in the project")
            }
        }
    }

    private fun saveCoverage(
        context: SensorContext,
        inputFile: InputFile,
        file: CoveredFile,
        lineOffset: Int,
        filePath: String
    ) {
        val linesToCover = file.linesToCover
        if (linesToCover != null) {
            if (linesToCover.all { !it.covered }) {
                // No need to save coverage for files with no covered lines
                return
            }

            val newCoverage = context.newCoverage().onFile(inputFile)

            file.linesToCover?.forEach { line ->
                val lineNumber = line.lineNumber + lineOffset
                newCoverage.lineHits(lineNumber, if (line.covered) 1 else 0)

                val branchesToCover = line.branchesToCover
                val coveredBranches = line.coveredBranches ?: 0
                if (branchesToCover != null) {
                    check(coveredBranches <= branchesToCover) {
                        "\"coveredBranches\" should not be greater than \"branchesToCover\" on line " +
                            "${line.lineNumber} for file \"$filePath\""
                    }

                    newCoverage.conditions(lineNumber, branchesToCover, coveredBranches)
                }
            }

            newCoverage.save()
        }
    }


}
