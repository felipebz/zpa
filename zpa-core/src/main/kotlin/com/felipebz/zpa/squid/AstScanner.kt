//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.api.RecognitionException
import com.felipebz.flr.impl.Parser
import com.felipebz.zpa.FormsMetadataAwareCheck
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.dynamicsql.DynamicSqlSupport
import com.felipebz.zpa.dynamicsql.DynamicSqlTools
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.metrics.ComplexityVisitor
import com.felipebz.zpa.metrics.FunctionComplexityVisitor
import com.felipebz.zpa.metrics.MetricsVisitor
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectSymbolIndex
import com.felipebz.zpa.symbols.local.LocalHighlighting
import com.felipebz.zpa.symbols.local.LocalPassFailures
import com.felipebz.zpa.symbols.local.LocalSemanticFacts
import com.felipebz.zpa.utils.getAnnotation
import com.felipebz.zpa.utils.log.Loggers
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@OptIn(ZpaExperimentalApi::class)
class AstScanner @JvmOverloads constructor(private val checks: Collection<PlSqlVisitor>,
                 private val formsMetadata: FormsMetadata?,
                 isErrorRecoveryEnabled: Boolean,
                 charset: Charset = StandardCharsets.UTF_8,
                 private val projectAnalysisContext: ProjectAnalysisContext = ProjectAnalysisContext.NOT_PREPARED,
                 conditionalCompilation: ConditionalCompilationSettings = ConditionalCompilationSettings.NONE,
                 private val highlightingPairs: Boolean = false) {

    private val configuration = PlSqlConfiguration(charset, isErrorRecoveryEnabled, conditionalCompilation)
    private val parser: Parser<Grammar> = PlSqlParser.create(configuration)
    private val dynamicSqlTools = DynamicSqlTools(configuration)
    private val semanticAnalysisPipeline = SemanticAnalysisPipeline(projectAnalysisContext)
    @OptIn(ZpaExperimentalApi::class)
    private val semanticScanCapabilities = SemanticScanCapabilities(
        createProjectAnalysis(projectAnalysisContext)
    )

    /** Java-friendly variant of [scanFile]: [FileId] is an inline class, so Java cannot call it directly. */
    fun scanFile(inputFile: PlSqlFile, extraVisitors: List<PlSqlVisitor>, fileId: String): AstScannerResult =
        scanFile(inputFile, extraVisitors, FileId(fileId))

    /**
     * Parses [inputFile] and walks it with the semantic visitors, the rule checks given to the constructor that apply to
     * the file's type, the metrics visitors and [extraVisitors].
     *
     * The rule checks are shared by every file of this scanner and keep their issues and per-file state on the
     * instance, so a walk that runs any of them holds a lock shared by every scanner: such walks run one at a time.
     * Parsing and the file's local resolution run before that walk and take no lock.
     * A walk that runs no rule check (none was given, or none applies to the file's type) takes no lock, and walks of
     * other files may run at the same time on other threads. [extraVisitors] then must not share mutable state between
     * calls: create them for each call.
     */
    @JvmOverloads
    fun scanFile(
        inputFile: PlSqlFile,
        extraVisitors: List<PlSqlVisitor> = emptyList(),
        fileId: FileId = FileId(inputFile.path().toString())
    ): AstScannerResult {
        val newVisitorContext = getPlSqlVisitorContext(inputFile)
        newVisitorContext.dynamicSqlSupport =
            DynamicSqlSupport(dynamicSqlTools, semanticAnalysisPipeline, fileId, semanticScanCapabilities)

        val metricsVisitor = MetricsVisitor()
        val complexityVisitor = ComplexityVisitor()
        val functionComplexityVisitor = FunctionComplexityVisitor()
        val semanticVisitors = semanticAnalysisPipeline.create(fileId)
        val symbolVisitor = semanticVisitors.symbolVisitor

        val checksToRun = mutableListOf<PlSqlVisitor>()
        checksToRun.addAll(semanticVisitors.afterLocal)

        val ruleChecks = if (inputFile.type() == PlSqlFile.Type.MAIN) {
            checks.filter { check -> formsMetadata != null || check !is FormsMetadataAwareCheck }
                .filterIsInstance<PlSqlCheck>()
                .filter { check -> ruleHasScope(check, RuleInfo.Scope.MAIN) }
        } else {
            checks.filterIsInstance<PlSqlCheck>()
                .filter { check -> ruleHasScope(check, RuleInfo.Scope.TEST) }
        }
        checksToRun.addAll(ruleChecks)

        checksToRun.add(metricsVisitor)
        checksToRun.add(complexityVisitor)
        checksToRun.add(functionComplexityVisitor)
        checksToRun.addAll(extraVisitors)

        // The file's local resolution keeps no state between files: it runs before the lock, so that files are
        // resolved in parallel and only the shared rule checks run one at a time.
        PlSqlAstWalker(listOf(semanticVisitors.local)).walk(newVisitorContext, semanticScanCapabilities)

        val walk = {
            val newWalker = PlSqlAstWalker(checksToRun)
            newWalker.walk(newVisitorContext, semanticScanCapabilities)

            checksToRun.flatMap {
                (it as PlSqlCheck).issues().map { issue -> ZpaIssue(inputFile, it, issue) }
            }
        }
        // Shared rule checks keep per-file state on the instance: their walks run one at a time.
        val issues = if (ruleChecks.isEmpty()) walk() else lock.withLock(walk)

        return AstScannerResult(
            executedChecks = listOf(semanticVisitors.local) + checksToRun,
            linesWithNoSonar = metricsVisitor.linesWithNoSonar,
            symbols = symbolVisitor.symbols,
            numberOfStatements = metricsVisitor.numberOfStatements,
            linesOfCode = metricsVisitor.getLinesOfCode().size,
            linesOfComments = metricsVisitor.getLinesOfComments().size,
            complexity = complexityVisitor.complexity,
            numberOfFunctions = functionComplexityVisitor.numberOfFunctions,
            executableLines = metricsVisitor.getExecutableLines(),
            issues = issues,
            highlightingPairs = newVisitorContext.localSemanticFacts?.takeIf { highlightingPairs }?.let { highlightingPairs(it, inputFile) }.orEmpty()
        )
    }

    /** The project index a name proven absent may be found in, when the project is prepared. */
    private val highlightingIndex: ProjectSymbolIndex? by lazy {
        LocalHighlighting.indexOf(projectAnalysisContext)
    }

    @OptIn(ZpaExperimentalApi::class)
    private fun highlightingPairs(facts: LocalSemanticFacts, inputFile: PlSqlFile): List<HighlightingPair> =
        try {
            LocalHighlighting.pairs(facts, highlightingIndex, inputFile.contents())
        } catch (e: Exception) {
            LocalPassFailures.report(LOG, "Highlighting pairs failed for $inputFile", e)
            emptyList()
        }

    private fun ruleHasScope(check: PlSqlVisitor, scope: RuleInfo.Scope): Boolean {
        val ruleInfo = getAnnotation(check, RuleInfo::class.java)
        if (ruleInfo != null) {
            return ruleInfo.scope === RuleInfo.Scope.ALL || ruleInfo.scope === scope
        }
        return scope === RuleInfo.Scope.MAIN
    }

    private fun getPlSqlVisitorContext(inputFile: PlSqlFile): PlSqlVisitorContext {
        var visitorContext: PlSqlVisitorContext
        try {
            val root = parser.parse(inputFile.contents())
            visitorContext = PlSqlVisitorContext(root, inputFile, formsMetadata)
        } catch (e: RecognitionException) {
            visitorContext = PlSqlVisitorContext(inputFile, e, formsMetadata)
            LOG.error("Unable to parse file: $inputFile\n${e.message}")
        } catch (e: Exception) {
            checkInterrupted(e)
            throw AnalysisException("Unable to analyze file: $inputFile", e)
        } catch (e: Throwable) {
            throw AnalysisException("Unable to analyze file: $inputFile", e)
        }

        return visitorContext
    }

    private fun checkInterrupted(e: Exception) {
        val cause = getRootCause(e)
        if (cause is InterruptedException || cause is InterruptedIOException) {
            throw AnalysisException("Analysis cancelled", e)
        }
    }

    private fun getRootCause(exception: Throwable?): Throwable? {
        var rootException = exception
        while (rootException?.cause != null) {
            rootException = rootException.cause
        }
        return rootException
    }

    companion object {
        private val LOG = Loggers.getLogger(AstScanner::class.java)
        private val lock = ReentrantLock()
    }

}
