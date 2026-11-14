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
import com.felipebz.zpa.api.squid.SemanticFileNode
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.utils.log.Loggers

/**
 * Computes the file's [LocalSemanticFacts] before any other visitor runs and leaves them on the visitor context.
 *
 * Forwarded calls use the scan's own project analysis: its snapshot (a not-prepared one answers them with no evidence)
 * and its external metadata.
 *
 * It never fails the analysis: an exception, or a stack overflow from a deeply nested file, is logged once for the
 * file and leaves the facts absent. Under zpa's own tests it fails the scan instead ([LocalPassFailures]).
 */
internal class LocalSemanticPrepass(private val fileId: FileId) : PlSqlCheck() {

    override fun visitFile(node: AstNode) {
        context.localSemanticFacts = null
        context.valueCategories = null
        context.fields = null
        context.cursors = null
        (node as? SemanticFileNode)?.localAnalysis = null
        context.localSemanticFacts = guarded("computing") {
            val model = LocalSemanticModelBuilder.build(node, fileId, context.plSqlFile()?.contents(), context.tokensAreWholeText)
            val references = LocalReferenceCollector.collect(node, model)
            val analysis = projectAnalysis()
            val coverage = LocalSpecificationCoverage.of(model, analysis.snapshot)
            val outcomes = LocalReferenceOutcomes(model, coverage)
            val resolutions = references.map(outcomes::steps)
            val forward = LocalForwardCalls(model, references, LocalCallCollector.collect(node), analysis.snapshot, analysis.externalContext, outcomes, context.formsMetadata, node)
            val calls = forward.analyze()
            LocalSemanticFacts(model, references, resolutions, calls, coverage?.specificationValues().orEmpty(),
                coverage?.specificationMembers.orEmpty().toMap(), LocalMemberCollector.collect(node, fileId), forward.itemActualTypes(),
                LocalNames.sqlRelationAlternatives(references, resolutions, outcomes)).also {
                val walk = LocalTypeWalk(model, analysis.snapshot, analysis.externalContext, outcomes)
                val categories = LocalValueCategories(it, walk, context.formsMetadata)
                context.valueCategories = categories
                val prepared = analysis.snapshot.preparationState != ProjectPreparationState.NOT_PREPARED
                context.fields = LocalFields(
                    it, analysis.snapshot.projectIndex.takeIf { prepared }, coverage, analysis.projectAnalysisContext.takeIf { prepared }, walk, categories
                )
                context.cursors = LocalCursors(it, walk)
            }
        }
        (node as? SemanticFileNode)?.localAnalysis = context.localSemanticFacts?.let { LocalFileAnalysis(it, context.valueCategories) }
    }

    private inline fun <T> guarded(step: String, block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            failed(step, e)
        } catch (e: StackOverflowError) {
            failed(step, e)
        }

    private fun failed(step: String, e: Throwable): Nothing? =
        LocalPassFailures.report(LOG, "Local semantic resolution failed while $step ${context.plSqlFile()?.fileName() ?: fileId.value}", e)

    private companion object {
        val LOG = Loggers.getLogger(LocalSemanticPrepass::class.java)
    }
}
