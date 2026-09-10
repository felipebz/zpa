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

import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolver
import com.felipebz.zpa.project.ProjectRecordMemberPathResolver
import com.felipebz.zpa.project.ProjectRecordMemberResolver
import com.felipebz.zpa.project.ProjectTypeResolver
import com.felipebz.zpa.project.TypeRefSemanticResolver
import com.felipebz.zpa.symbols.DefaultTypeSolver
import com.felipebz.zpa.symbols.ProjectRecordFieldTypeResolutionVisitor
import com.felipebz.zpa.symbols.ProjectRecordMemberPathResolutionVisitor
import com.felipebz.zpa.symbols.ProjectRecordMemberResolutionVisitor
import com.felipebz.zpa.symbols.ProjectTypeResolutionVisitor
import com.felipebz.zpa.symbols.ScopeImpl
import com.felipebz.zpa.symbols.SymbolVisitor
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.symbols.Scope

/**
 * Creates the semantic visitors in the one order required by the analysis pipeline.
 *
 * This is deliberately an internal implementation detail. It is shared by production
 * analysis and the diagnostic bridge so that tooling cannot accidentally drift from the
 * semantic decoration order used by ZPA.
 */
internal class SemanticAnalysisPipeline(
    private val projectAnalysisContext: ProjectAnalysisContext,
    private val globalScope: Scope
) {

    data class Visitors(
        val symbolVisitor: SymbolVisitor,
        val all: List<PlSqlVisitor>
    )

    fun create(fileId: FileId): Visitors {
        val symbolVisitor = SymbolVisitor(DefaultTypeSolver(), globalScope)
        val visitors = mutableListOf<PlSqlVisitor>(symbolVisitor)

        if (projectAnalysisContext.state !is ProjectAnalysisContext.State.NotPrepared) {
            val projectTypeResolver = ProjectTypeResolver(projectAnalysisContext)
            val projectRecordMemberResolver = ProjectRecordMemberResolver()
            val projectRecordFieldTypeResolver = ProjectRecordFieldTypeResolver(
                TypeRefSemanticResolver(projectTypeResolver)
            )
            visitors.add(ProjectTypeResolutionVisitor(projectTypeResolver, fileId))
            visitors.add(ProjectRecordMemberResolutionVisitor(projectRecordMemberResolver))
            visitors.add(ProjectRecordFieldTypeResolutionVisitor(projectRecordFieldTypeResolver))
            visitors.add(
                ProjectRecordMemberPathResolutionVisitor(
                    ProjectRecordMemberPathResolver(projectRecordMemberResolver, projectRecordFieldTypeResolver)
                )
            )
        }

        return Visitors(symbolVisitor, visitors)
    }

    companion object {
        fun forTooling(projectAnalysisContext: ProjectAnalysisContext): SemanticAnalysisPipeline =
            SemanticAnalysisPipeline(projectAnalysisContext, ScopeImpl())
    }
}
