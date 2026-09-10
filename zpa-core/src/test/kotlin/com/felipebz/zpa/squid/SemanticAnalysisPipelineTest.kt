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
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.symbols.ProjectRecordFieldTypeResolutionVisitor
import com.felipebz.zpa.symbols.ProjectRecordMemberPathResolutionVisitor
import com.felipebz.zpa.symbols.ProjectRecordMemberResolutionVisitor
import com.felipebz.zpa.symbols.ProjectTypeResolutionVisitor
import com.felipebz.zpa.symbols.ScopeImpl
import com.felipebz.zpa.symbols.SymbolVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticAnalysisPipelineTest {
    @Test
    fun projectVisitorsFollowLegacySymbolVisitorInProductionOrder() {
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(
                listOf(ProjectSource(FileId("current.sql")) { "CREATE PACKAGE p AS END p;" }),
                concurrent = false
            )
        )

        val visitors = SemanticAnalysisPipeline(context, ScopeImpl()).create(FileId("current.sql")).all

        assertThat(visitors.map { it::class }).containsExactly(
            SymbolVisitor::class,
            ProjectTypeResolutionVisitor::class,
            ProjectRecordMemberResolutionVisitor::class,
            ProjectRecordFieldTypeResolutionVisitor::class,
            ProjectRecordMemberPathResolutionVisitor::class
        )
    }

    @Test
    fun projectVisitorsAreOmittedWhenTheContextIsNotPrepared() {
        val visitors = SemanticAnalysisPipeline(
            ProjectAnalysisContext.NOT_PREPARED,
            ScopeImpl()
        ).create(FileId("current.sql")).all

        assertThat(visitors.map { it::class }).containsExactly(SymbolVisitor::class)
    }
}
