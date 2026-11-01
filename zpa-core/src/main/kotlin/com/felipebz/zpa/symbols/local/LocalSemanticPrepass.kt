//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.utils.log.Loggers

/**
 * Computes the file's [LocalSemanticFacts] before any other visitor runs and leaves them on the visitor context.
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
        context.fields = null
        (node as? SemanticAstNode)?.localSemanticFacts = null
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
            LocalSemanticFacts(model, references, resolutions, calls, held, coverage?.specificationValues().orEmpty(),
                coverage?.sqlSpecificationMembers.orEmpty().toMap(), LocalMemberCollector.collect(node, fileId)).also {
                context.valueCategories = LocalValueCategories(it, LocalTypeWalk(model, analysis.snapshot, analysis.externalContext, outcomes))
                val prepared = analysis.snapshot.preparationState != ProjectPreparationState.NOT_PREPARED
                context.fields = LocalFields(
                    it, analysis.snapshot.projectIndex.takeIf { prepared }, coverage, analysis.projectAnalysisContext.takeIf { prepared }
                )
            }
        }
        (node as? SemanticAstNode)?.localSemanticFacts = context.localSemanticFacts
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

    private companion object {
        val LOG = Loggers.getLogger(LocalSemanticPrepass::class.java)
    }
}
