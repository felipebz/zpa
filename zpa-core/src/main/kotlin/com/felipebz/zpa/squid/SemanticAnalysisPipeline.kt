//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.*
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
        // The local resolution first: the project visitors read it.
        val visitors = mutableListOf<PlSqlVisitor>(LocalSemanticPrepass(fileId), symbolVisitor)

        if (projectAnalysisContext.state !is ProjectAnalysisContext.State.NotPrepared) {
            val projectTypeResolver = ProjectTypeResolver(projectAnalysisContext)
            val projectRecordMemberResolver = ProjectRecordMemberResolver(projectAnalysisContext)
            val projectRecordFieldTypeResolver = ProjectRecordFieldTypeResolver(
                TypeRefSemanticResolver(projectTypeResolver)
            )
            val projectIndex = (projectAnalysisContext.state as ProjectAnalysisContext.State.Prepared).result.index
            visitors.add(ProjectSubprogramDeclarationResolutionVisitor(projectIndex, fileId))
            visitors.add(ProjectTypeResolutionVisitor(projectTypeResolver, fileId))
            val selfBase = ProjectSelfMemberBase(projectTypeResolver, projectRecordMemberResolver, projectRecordFieldTypeResolver, fileId)
            val memberVisitor = ProjectRecordMemberResolutionVisitor(projectRecordMemberResolver, selfBase)
            visitors.add(memberVisitor)
            visitors.add(ProjectRecordFieldTypeResolutionVisitor(projectRecordFieldTypeResolver))
            val pathResolver = ProjectRecordMemberPathResolver(projectRecordMemberResolver, projectRecordFieldTypeResolver)
            val pathVisitor = ProjectRecordMemberPathResolutionVisitor(pathResolver, selfBase)
            visitors.add(pathVisitor)
            visitors.add(ProjectMethodCallVisitor(projectTypeResolver, projectRecordMemberResolver, projectIndex, fileId, memberVisitor))
            visitors.add(
                ProjectReceiverMemberVisitor(
                    TypeRefSemanticResolver(projectTypeResolver), projectRecordMemberResolver, projectRecordFieldTypeResolver,
                    pathResolver, projectIndex, memberVisitor, pathVisitor, selfBase
                )
            )
        }

        return Visitors(symbolVisitor, visitors)
    }

    companion object {
        fun forTooling(projectAnalysisContext: ProjectAnalysisContext): SemanticAnalysisPipeline =
            SemanticAnalysisPipeline(projectAnalysisContext)
    }
}
