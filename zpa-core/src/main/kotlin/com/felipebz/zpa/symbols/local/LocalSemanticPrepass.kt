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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.utils.log.Loggers

/**
 * Computes the file's [LocalSemanticFacts] before any other visitor runs and leaves them on the visitor context. Shadow
 * mode: nothing reads them to decide anything. At the end of the walk it records, for each call and qualified name,
 * whether the legacy lookup found its first segment, for comparison with the local outcome.
 *
 * Forwarded calls use the scan's own project analysis: its snapshot (a not-prepared one answers them with no evidence)
 * and its external metadata.
 *
 * It never fails the analysis: an exception, or a stack overflow from a deeply nested file, is logged once for the
 * file and leaves the facts absent.
 */
internal class LocalSemanticPrepass(private val fileId: FileId) : PlSqlCheck() {

    override fun visitFile(node: AstNode) {
        context.localSemanticFacts = null
        context.valueCategories = null
        context.localSemanticFacts = guarded("computing") {
            val model = LocalSemanticModelBuilder.build(node, fileId, context.plSqlFile()?.contents())
            val references = LocalReferenceCollector.collect(node, model)
            val analysis = projectAnalysis()
            val coverage = LocalSpecificationCoverage.of(model, analysis.snapshot)
            val outcomes = LocalReferenceOutcomes(model, coverage)
            val resolutions = references.map(outcomes::steps)
            val calls = LocalForwardCalls(model, references, LocalCallCollector.collect(node), analysis.snapshot, analysis.externalContext, outcomes).analyze()
            val held = if (analysis.snapshot.preparationState == ProjectPreparationState.NOT_PREPARED) emptySet()
            else analysis.snapshot.projectIndex.declarationsFor(fileId).mapTo(HashSet()) { LocalRangeKey.of(it.sourceRange) }
            LocalSemanticFacts(model, references, resolutions, calls, held, coverage?.mismatches.orEmpty(), coverage?.specificationValues().orEmpty(),
                coverage?.sqlSpecificationMembers.orEmpty().toMap(), LocalMemberCollector.collect(node, fileId)).also {
                context.valueCategories = LocalValueCategories(it, LocalTypeWalk(model, analysis.snapshot, analysis.externalContext, outcomes))
            }
        }
    }

    override fun leaveFile(node: AstNode) {
        val facts = context.localSemanticFacts ?: return
        val recorded = guarded("comparing") {
            val identifiers = HashMap<Long, AstNode>()
            for (identifier in node.getDescendants(PlSqlGrammar.IDENTIFIER_NAME)) identifiers.putIfAbsent(key(identifier.token.line, identifier.token.column), identifier)
            for (reference in facts.references) {
                if (reference.context !in FIRST_SEGMENT_CONTEXTS || isTypeName(reference)) continue
                val identifier = identifiers[key(reference.range.startLine, reference.range.startColumn)] ?: continue
                facts.recordFirstSegment(reference.range.startLine, reference.range.startColumn, legacyHit(identifier))
            }
        }
        if (recorded == null) context.localSemanticFacts = null
    }

    /** The legacy symbol of the name, of its VARIABLE_NAME, or of the whole qualified name it starts. */
    private fun legacyHit(identifier: AstNode): Boolean {
        var current: AstNode? = identifier
        while (current != null) {
            if ((current as? SemanticAstNode)?.symbol != null) return true
            val parent = current.parentOrNull ?: return false
            if (parent.type !== PlSqlGrammar.VARIABLE_NAME && parent.type !== PlSqlGrammar.MEMBER_EXPRESSION) return false
            if (parent.firstChild !== current) return false
            current = parent
        }
        return false
    }

    private inline fun <T> guarded(step: String, block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            failed(step, e)
        } catch (e: StackOverflowError) {
            failed(step, e)
        }

    private fun failed(step: String, e: Throwable): Nothing? {
        LOG.warn("Local semantic resolution failed while $step ${context.plSqlFile()?.fileName() ?: fileId.value}: $e")
        return null
    }

    private fun key(line: Int, column: Int) = line.toLong() shl 32 or column.toLong()

    private companion object {
        val LOG = Loggers.getLogger(LocalSemanticPrepass::class.java)
        val FIRST_SEGMENT_CONTEXTS = setOf(LocalReferenceContext.QUALIFIER, LocalReferenceContext.CALL_STATEMENT, LocalReferenceContext.CALL_EXPRESSION)
    }
}
