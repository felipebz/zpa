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
package com.felipebz.zpa.squid

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.NewIssue
import org.sonar.api.batch.sensor.issue.NewIssueLocation
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContextFactory
import com.felipebz.zpa.PlSqlChecks
import com.felipebz.zpa.checks.IssueLocation
import com.felipebz.zpa.highlight.PlSqlHighlighterVisitor
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.metrics.CpdVisitor
import com.felipebz.zpa.rules.SonarQubeRuleKeyAdapter
import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.symbols.SonarQubeSymbolTable
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.checks.PlSqlVisitor
import java.io.Serializable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PlSqlAstScanner(private val context: SensorContext,
                      checks: Collection<PlSqlVisitor>,
                      private val noSonarFilter: NoSonarFilter,
                      formsMetadata: FormsMetadata?,
                      isErrorRecoveryEnabled: Boolean,
                      private val fileLinesContextFactory: FileLinesContextFactory?) {

    private val astScanner: AstScanner =
        AstScanner(checks, formsMetadata, isErrorRecoveryEnabled, context.fileSystem().encoding())

    private lateinit var plsqlChecks: PlSqlChecks

    constructor(context: SensorContext,
                checks: PlSqlChecks,
                noSonarFilter: NoSonarFilter,
                formsMetadata: FormsMetadata?,
                isErrorRecoveryEnabled: Boolean,
                fileLinesContextFactory: FileLinesContextFactory,
                objectLocator: ObjectLocator) : this(context, checks.all(), noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory) {
        this.plsqlChecks = checks
        objectLocator.setScope(astScanner.globalScope)
    }

    fun scanFile(inputFile: InputFile) {
        val plSqlFile = SonarQubePlSqlFile(inputFile)
        if (plSqlFile.type() == PlSqlFile.Type.MAIN) {
            scanMainFile(plSqlFile)
        } else {
            scanTestFile(plSqlFile)
        }
    }

    private fun scanMainFile(plSqlFile: SonarQubePlSqlFile) {
        println("Scanning file ${plSqlFile.fileName()}")
        val inputFile = plSqlFile.inputFile
        val result = try {
            astScanner.scanFile(
                plSqlFile,
                listOf(PlSqlHighlighterVisitor(context, inputFile), CpdVisitor(context, inputFile))
            )
        } catch (e: Exception) {
            println("Error scanning file ${plSqlFile.fileName()}: ${e.message}")
            e.printStackTrace()
            throw e
        }
        println("File ${plSqlFile.fileName()} scanned")

        noSonarFilter.noSonarInFile(inputFile, result.linesWithNoSonar)

        lock.withLock { // needed because SonarQube 7.6 save this data in a non thread-safe way
            saveIssues(inputFile, result.issues)

            val symbolSaver = SonarQubeSymbolTable(context, inputFile)
            symbolSaver.save(result.symbols)

            saveMetricOnFile(inputFile, CoreMetrics.STATEMENTS, result.numberOfStatements)
            saveMetricOnFile(inputFile, CoreMetrics.NCLOC, result.linesOfCode)
            println("NCLOC: ${result.linesOfCode}")
            saveMetricOnFile(inputFile, CoreMetrics.COMMENT_LINES, result.linesOfComments)
            saveMetricOnFile(inputFile, CoreMetrics.COMPLEXITY, result.complexity)
            saveMetricOnFile(inputFile, CoreMetrics.FUNCTIONS, result.numberOfFunctions)


            if (fileLinesContextFactory != null) {
                val fileLinesContext = fileLinesContextFactory.createFor(inputFile)
                for (line in result.executableLines) {
                    fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1)
                }
                fileLinesContext.save()
            }
        }
    }

    private fun scanTestFile(plSqlFile: SonarQubePlSqlFile) {
        val inputFile = plSqlFile.inputFile
        val result = astScanner.scanFile(plSqlFile, listOf(PlSqlHighlighterVisitor(context, inputFile)))

        noSonarFilter.noSonarInFile(inputFile, result.linesWithNoSonar)

        lock.withLock { // needed because SonarQube 7.6 save this data in a non thread-safe way
            saveIssues(inputFile, result.issues)

            val symbolTable = SonarQubeSymbolTable(context, inputFile)
            symbolTable.save(result.symbols)
        }
    }

    private fun saveIssues(inputFile: InputFile, issues: List<ZpaIssue>) {
        for (issue in issues) {
            val rule = plsqlChecks.ruleKey(issue.check) as SonarQubeRuleKeyAdapter

            val newIssue = context.newIssue().forRule(rule.ruleKey)

            val cost = issue.cost
            if (cost != null) {
                newIssue.gap(cost.toDouble())
            }

            newIssue.at(newLocation(inputFile, newIssue, issue.primaryLocation))

            for (secondaryLocation in issue.secondaryLocations) {
                newIssue.addLocation(newLocation(inputFile, newIssue, secondaryLocation))
            }

            newIssue.save()
        }
    }

    private fun <T : Serializable> saveMetricOnFile(inputFile: InputFile, metric: Metric<T>, value: T) {
        context.newMeasure<T>()
                .on(inputFile)
                .forMetric(metric)
                .withValue(value)
                .save()
    }

    private fun newLocation(inputFile: InputFile, issue: NewIssue, location: IssueLocation): NewIssueLocation {
        val newLocation = issue.newLocation().on(inputFile)
        if (location.startLine() != IssueLocation.UNDEFINED_LINE) {
            val range: TextRange = if (location.startLineOffset() == IssueLocation.UNDEFINED_OFFSET) {
                inputFile.selectLine(location.startLine())
            } else {
                inputFile.newRange(location.startLine(), location.startLineOffset(), location.endLine(),
                        location.endLineOffset())
            }
            newLocation.at(range)
        }

        newLocation.message(location.message())
        return newLocation
    }

    companion object {
        private val lock = ReentrantLock()
    }

}
