//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.symbols.DefaultTypeSolver
import com.felipebz.zpa.symbols.ProjectSubprogramDeclarationResolutionVisitor
import com.felipebz.zpa.symbols.SymbolVisitor
import com.felipebz.zpa.symbols.local.LocalSemanticPrepass

/**
 * Creates the semantic visitors in the one order required by the analysis pipeline.
 *
 * This is deliberately an internal implementation detail. It is shared by production
 * analysis and the diagnostic bridge so that tooling cannot accidentally drift from the
 * semantic decoration order used by ZPA.
 */
internal class SemanticAnalysisPipeline(
    private val projectAnalysisContext: ProjectAnalysisContext
) {

    @Suppress("DEPRECATION")
    data class Visitors(
        val symbolVisitor: SymbolVisitor,
        val all: List<PlSqlVisitor>
    )

    @Suppress("DEPRECATION")
    fun create(fileId: FileId): Visitors {
        val symbolVisitor = SymbolVisitor(DefaultTypeSolver(), isGlobalContext = true)
        // The local resolution first: the project visitor reads it.
        val visitors = mutableListOf<PlSqlVisitor>(LocalSemanticPrepass(fileId), symbolVisitor)

        if (projectAnalysisContext.state !is ProjectAnalysisContext.State.NotPrepared) {
            val projectIndex = (projectAnalysisContext.state as ProjectAnalysisContext.State.Prepared).result.index
            visitors.add(ProjectSubprogramDeclarationResolutionVisitor(projectIndex, fileId))
        }

        return Visitors(symbolVisitor, visitors)
    }

    companion object {
        fun forTooling(projectAnalysisContext: ProjectAnalysisContext): SemanticAnalysisPipeline =
            SemanticAnalysisPipeline(projectAnalysisContext)
    }
}
