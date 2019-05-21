/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import com.sonar.sslr.api.Grammar
import com.sonar.sslr.api.RecognitionException
import com.sonar.sslr.impl.Parser
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.measure.Metric
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.NewIssue
import org.sonar.api.batch.sensor.issue.NewIssueLocation
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.utils.AnnotationUtils
import org.sonar.plsqlopen.FormsMetadataAwareCheck
import org.sonar.plsqlopen.PlSqlChecks
import org.sonar.plsqlopen.checks.IssueLocation
import org.sonar.plsqlopen.getSemanticNode
import org.sonar.plsqlopen.highlight.PlSqlHighlighterVisitor
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.metrics.ComplexityVisitor
import org.sonar.plsqlopen.metrics.CpdVisitor
import org.sonar.plsqlopen.metrics.FunctionComplexityVisitor
import org.sonar.plsqlopen.metrics.MetricsVisitor
import org.sonar.plsqlopen.parser.PlSqlParser
import org.sonar.plsqlopen.rules.SonarQubeRuleKeyAdapter
import org.sonar.plsqlopen.symbols.DefaultTypeSolver
import org.sonar.plsqlopen.symbols.SonarQubeSymbolTable
import org.sonar.plsqlopen.symbols.SymbolVisitor
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck.PreciseIssue
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import java.io.InterruptedIOException
import java.io.Serializable
import java.util.*
import kotlin.streams.toList

class PlSqlAstScanner(private val context: SensorContext, private val checks: Collection<PlSqlVisitor>, private val noSonarFilter: NoSonarFilter,
                      private val formsMetadata: FormsMetadata?, isErrorRecoveryEnabled: Boolean, private val fileLinesContextFactory: FileLinesContextFactory?) {

    private val parser: Parser<Grammar> = PlSqlParser.create(PlSqlConfiguration(context.fileSystem().encoding(), isErrorRecoveryEnabled))

    private lateinit var plsqlChecks: PlSqlChecks

    constructor(context: SensorContext, checks: PlSqlChecks, noSonarFilter: NoSonarFilter,
                formsMetadata: FormsMetadata?, isErrorRecoveryEnabled: Boolean, fileLinesContextFactory: FileLinesContextFactory) : this(context, checks.all(), noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory) {
        this.plsqlChecks = checks
    }

    @VisibleForTesting
    fun scanFile(inputFile: InputFile) {
        if (inputFile.type() == InputFile.Type.MAIN) {
            scanMainFile(inputFile)
        } else {
            scanTestFile(inputFile)
        }
    }

    private fun scanMainFile(inputFile: InputFile) {
        val metricsVisitor = MetricsVisitor()
        val complexityVisitor = ComplexityVisitor()
        val functionComplexityVisitor = FunctionComplexityVisitor()

        val newVisitorContext = getPlSqlVisitorContext(inputFile)

        val symbolVisitor = SymbolVisitor(DefaultTypeSolver())

        val checksToRun = ArrayList<PlSqlVisitor>()
        checksToRun.add(symbolVisitor)

        checksToRun.addAll(
                checks.stream()
                        .filter { check -> formsMetadata != null || check !is FormsMetadataAwareCheck }
                        .filter { check -> check is PlSqlCheck }
                        .filter { check -> ruleHasScope(check, RuleInfo.Scope.MAIN) }
                        .toList())

        checksToRun.add(PlSqlHighlighterVisitor(context, inputFile))
        checksToRun.add(metricsVisitor)
        checksToRun.add(complexityVisitor)
        checksToRun.add(functionComplexityVisitor)
        checksToRun.add(CpdVisitor(context, inputFile))

        val newWalker = PlSqlAstWalker(checksToRun)
        newWalker.walk(newVisitorContext)

        noSonarFilter.noSonarInFile(inputFile, metricsVisitor.linesWithNoSonar)

        for (check in checksToRun) {
            val issues = (check as PlSqlCheck).issues()
            if (issues.isNotEmpty()) {
                saveIssues(inputFile, check, issues)
            }
        }

        val symbolSaver = SonarQubeSymbolTable(context, inputFile)
        symbolSaver.save(symbolVisitor.symbols)

        saveMetricOnFile(inputFile, CoreMetrics.STATEMENTS, metricsVisitor.numberOfStatements)
        saveMetricOnFile(inputFile, CoreMetrics.NCLOC, metricsVisitor.getLinesOfCode().size)
        saveMetricOnFile(inputFile, CoreMetrics.COMMENT_LINES, metricsVisitor.getLinesOfComments().size)
        saveMetricOnFile(inputFile, CoreMetrics.COMPLEXITY, complexityVisitor.complexity)
        saveMetricOnFile(inputFile, CoreMetrics.FUNCTIONS, functionComplexityVisitor.numberOfFunctions)

        if (fileLinesContextFactory != null) {
            val fileLinesContext = fileLinesContextFactory.createFor(inputFile)
            for (line in metricsVisitor.getExecutableLines()) {
                fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1)
            }
            fileLinesContext.save()
        }
    }

    private fun scanTestFile(inputFile: InputFile) {
        val newVisitorContext = getPlSqlVisitorContext(inputFile)
        val metricsVisitor = MetricsVisitor()

        val symbolVisitor = SymbolVisitor(DefaultTypeSolver())

        val checksToRun = ArrayList<PlSqlVisitor>()
        checksToRun.add(symbolVisitor)
        checksToRun.add(PlSqlHighlighterVisitor(context, inputFile))
        checksToRun.add(metricsVisitor)

        checksToRun.addAll(
                checks.stream()
                        .filter { check -> check is PlSqlCheck }
                        .filter { check -> ruleHasScope(check, RuleInfo.Scope.TEST) }
                        .toList())

        val newWalker = PlSqlAstWalker(checksToRun)
        newWalker.walk(newVisitorContext)

        noSonarFilter.noSonarInFile(inputFile, metricsVisitor.linesWithNoSonar)

        for (check in checksToRun) {
            val issues = (check as PlSqlCheck).issues()
            if (issues.isNotEmpty()) {
                saveIssues(inputFile, check, issues)
            }
        }

        val symbolTable = SonarQubeSymbolTable(context, inputFile)
        symbolTable.save(symbolVisitor.symbols)
    }

    private fun ruleHasScope(check: PlSqlVisitor, scope: RuleInfo.Scope): Boolean {
        val ruleInfo = AnnotationUtils.getAnnotation(check, RuleInfo::class.java)
        return if (ruleInfo != null) {
            ruleInfo.scope === RuleInfo.Scope.ALL || ruleInfo.scope === scope
        } else scope === RuleInfo.Scope.MAIN
    }

    private fun getPlSqlVisitorContext(inputFile: InputFile): PlSqlVisitorContext {
        val plSqlFile = SonarQubePlSqlFile.create(inputFile)

        var visitorContext: PlSqlVisitorContext
        try {
            val root = getSemanticNode(parser.parse(plSqlFile.contents()))
            visitorContext = PlSqlVisitorContext(root, plSqlFile, formsMetadata)
        } catch (e: RecognitionException) {
            visitorContext = PlSqlVisitorContext(plSqlFile, e, formsMetadata)
            LOG.error("Unable to parse file: $inputFile\n${e.message}")
        } catch (e: Exception) {
            checkInterrupted(e)
            throw AnalysisException("Unable to analyze file: $inputFile", e)
        } catch (e: Throwable) {
            throw AnalysisException("Unable to analyze file: $inputFile", e)
        }

        return visitorContext
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

    private fun checkInterrupted(e: Exception) {
        val cause = Throwables.getRootCause(e)
        if (cause is InterruptedException || cause is InterruptedIOException) {
            throw AnalysisException("Analysis cancelled", e)
        }
    }

    companion object {
        private val LOG = Loggers.getLogger(PlSqlAstScanner::class.java)
    }

}
