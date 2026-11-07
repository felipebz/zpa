//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.squid

import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.symbols.ProjectSubprogramDeclarationResolutionVisitor
import com.felipebz.zpa.symbols.SymbolVisitor
import com.felipebz.zpa.symbols.local.LocalSemanticPrepass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticAnalysisPipelineTest {
    @Test
    fun theProjectVisitorFollowsLegacySymbolVisitorInProductionOrder() {
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(
                listOf(ProjectSource(FileId("current.sql")) { "CREATE PACKAGE p AS END p;" }),
                concurrent = false
            )
        )

        val visitors = SemanticAnalysisPipeline(context).create(FileId("current.sql")).all

        assertThat(visitors.map { it::class }).containsExactly(
            LocalSemanticPrepass::class,
            SymbolVisitor::class,
            ProjectSubprogramDeclarationResolutionVisitor::class
        )
    }

    @Test
    fun theProjectVisitorIsOmittedWhenTheContextIsNotPrepared() {
        val visitors = SemanticAnalysisPipeline(ProjectAnalysisContext.NOT_PREPARED)
            .create(FileId("current.sql")).all

        assertThat(visitors.map { it::class }).containsExactly(LocalSemanticPrepass::class, SymbolVisitor::class)
    }
}
