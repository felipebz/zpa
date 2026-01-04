/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.api.RecognitionException
import com.felipebz.flr.impl.Parser
import com.felipebz.zpa.FormsMetadataAwareCheck
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.metrics.ComplexityVisitor
import com.felipebz.zpa.metrics.FunctionComplexityVisitor
import com.felipebz.zpa.metrics.MetricsVisitor
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.symbols.DefaultTypeSolver
import com.felipebz.zpa.symbols.ScopeImpl
import com.felipebz.zpa.symbols.SymbolVisitor
import com.felipebz.zpa.utils.getAnnotation
import com.felipebz.zpa.utils.log.Loggers
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.checks.PlSqlVisitor
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AstScanner(private val checks: Collection<PlSqlVisitor>,
                 private val formsMetadata: FormsMetadata?,
                 isErrorRecoveryEnabled: Boolean,
                 charset: Charset = StandardCharsets.UTF_8) {

    private val parser: Parser<Grammar> = PlSqlParser.create(PlSqlConfiguration(charset, isErrorRecoveryEnabled))
    val globalScope = ScopeImpl()

    fun scanFile(inputFile: PlSqlFile, extraVisitors: List<PlSqlVisitor> = emptyList()) : AstScannerResult {
        val newVisitorContext = getPlSqlVisitorContext(inputFile)

        val metricsVisitor = MetricsVisitor()
        val complexityVisitor = ComplexityVisitor()
        val functionComplexityVisitor = FunctionComplexityVisitor()
        val symbolVisitor = SymbolVisitor(DefaultTypeSolver(), globalScope)

        val checksToRun = mutableListOf<PlSqlVisitor>()
        checksToRun.add(symbolVisitor)

        if (inputFile.type() == PlSqlFile.Type.MAIN) {
            checksToRun.addAll(
                checks.filter { check -> formsMetadata != null || check !is FormsMetadataAwareCheck }
                    .filterIsInstance<PlSqlCheck>()
                    .filter { check -> ruleHasScope(check, RuleInfo.Scope.MAIN) }
                    .toList())
        } else {
            checksToRun.addAll(
                checks.filterIsInstance<PlSqlCheck>()
                    .filter { check -> ruleHasScope(check, RuleInfo.Scope.TEST) }
                    .toList())
        }

        checksToRun.add(metricsVisitor)
        checksToRun.add(complexityVisitor)
        checksToRun.add(functionComplexityVisitor)
        checksToRun.addAll(extraVisitors)

        val issues = lock.withLock {
            val newWalker = PlSqlAstWalker(checksToRun)
            newWalker.walk(newVisitorContext)

            checksToRun.flatMap {
                (it as PlSqlCheck).issues().map { issue -> ZpaIssue(inputFile, it, issue) }
            }
        }

        return AstScannerResult(
            executedChecks = checksToRun,
            linesWithNoSonar = metricsVisitor.linesWithNoSonar,
            symbols = symbolVisitor.symbols,
            numberOfStatements = metricsVisitor.numberOfStatements,
            linesOfCode = metricsVisitor.getLinesOfCode().size,
            linesOfComments = metricsVisitor.getLinesOfComments().size,
            complexity = complexityVisitor.complexity,
            numberOfFunctions = functionComplexityVisitor.numberOfFunctions,
            executableLines = metricsVisitor.getExecutableLines(),
            issues = issues
        )
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
