/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.squid

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.NewIssue
import org.sonar.api.batch.sensor.issue.NewIssueLocation
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.plsqlopen.PlSqlChecks
import org.sonar.plsqlopen.checks.IssueLocation
import org.sonar.plsqlopen.highlight.PlSqlHighlighterVisitor
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.metrics.CpdVisitor
import org.sonar.plsqlopen.rules.SonarQubeRuleKeyAdapter
import org.sonar.plsqlopen.symbols.SonarQubeSymbolTable
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck.PreciseIssue
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import java.io.Serializable

class PlSqlAstScanner(private val context: SensorContext,
                      checks: Collection<PlSqlVisitor>,
                      private val noSonarFilter: NoSonarFilter,
                      formsMetadata: FormsMetadata?,
                      isErrorRecoveryEnabled: Boolean,
                      private val fileLinesContextFactory: FileLinesContextFactory?) {

    private val astScanner: AstScanner = AstScanner(checks, formsMetadata, isErrorRecoveryEnabled, context.fileSystem().encoding())

    private lateinit var plsqlChecks: PlSqlChecks

    constructor(context: SensorContext,
                checks: PlSqlChecks,
                noSonarFilter: NoSonarFilter,
                formsMetadata: FormsMetadata?,
                isErrorRecoveryEnabled: Boolean,
                fileLinesContextFactory: FileLinesContextFactory) : this(context, checks.all(), noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory) {
        this.plsqlChecks = checks
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
        val inputFile = plSqlFile.inputFile

        val result = astScanner.scanFile(plSqlFile, listOf(PlSqlHighlighterVisitor(context, inputFile), CpdVisitor(context, inputFile)))

        noSonarFilter.noSonarInFile(inputFile, result.linesWithNoSonar)

        for (check in result.executedChecks) {
            val issues = (check as PlSqlCheck).issues()
            if (issues.isNotEmpty()) {
                saveIssues(inputFile, check, issues)
            }
        }

        val symbolSaver = SonarQubeSymbolTable(context, inputFile)
        symbolSaver.save(result.symbols)

        saveMetricOnFile(inputFile, CoreMetrics.STATEMENTS, result.numberOfStatements)
        saveMetricOnFile(inputFile, CoreMetrics.NCLOC, result.linesOfCode)
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

    private fun scanTestFile(plSqlFile: SonarQubePlSqlFile) {
        val inputFile = plSqlFile.inputFile

        val result = astScanner.scanFile(plSqlFile, listOf(PlSqlHighlighterVisitor(context, inputFile)))

        noSonarFilter.noSonarInFile(inputFile, result.linesWithNoSonar)

        for (check in result.executedChecks) {
            val issues = (check as PlSqlCheck).issues()
            if (issues.isNotEmpty()) {
                saveIssues(inputFile, check, issues)
            }
        }

        val symbolTable = SonarQubeSymbolTable(context, inputFile)
        symbolTable.save(result.symbols)
    }

    private fun saveIssues(inputFile: InputFile, check: PlSqlVisitor, issues: List<PreciseIssue>) {
        val rule = plsqlChecks.ruleKey(check) as SonarQubeRuleKeyAdapter
        for (preciseIssue in issues) {

            val newIssue = context.newIssue().forRule(rule.ruleKey)

            val cost = preciseIssue.cost()
            if (cost != null) {
                newIssue.gap(cost.toDouble())
            }

            newIssue.at(newLocation(inputFile, newIssue, preciseIssue.primaryLocation()))

            for (secondaryLocation in preciseIssue.secondaryLocations()) {
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

}
