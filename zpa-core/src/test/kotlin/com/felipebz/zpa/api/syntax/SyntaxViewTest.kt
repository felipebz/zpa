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
package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ZpaExperimentalApi::class)
class SyntaxViewTest {

    @Test
    fun exposesTableReferenceComponents() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.references.take(5).map { Triple(it.schema, it.name, it.databaseLink) })
            .containsExactly(
                Triple(null, "tab", null),
                Triple("sch", "tab", null),
                Triple(null, "tab", "link.domain.com"),
                Triple("sch", "tab", "link"),
                Triple("\"Sch\"", "\"Tab\"", "\"Link\".\"Domain\".\"Com\"")
            )
    }

    @Test
    fun dispatchesOnlyActualTableReferencesAcrossSqlStatements() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.references.map { it.name })
            .containsExactly("tab", "tab", "tab", "tab", "\"Tab\"", "tab", "tab", "tab", "target", "source", "inner_table")
        assertThat(visitor.rawTableReferenceCount).isEqualTo(visitor.references.size)
        assertThat(visitor.rawLeftTableReferenceCount).isEqualTo(visitor.references.size)
    }

    @Test
    fun typedOnlySubscriptionDoesNotInvokeRawCallbacks() {
        val visitor = TypedOnlyVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.references.map { it.name })
            .containsExactly("tab", "tab", "tab", "tab", "\"Tab\"", "tab", "tab", "tab", "target", "source", "inner_table")
        assertThat(visitor.rawVisitedTypes).isEmpty()
        assertThat(visitor.rawLeftTypes).isEmpty()
    }

    @Test
    fun rawAndTypedSubscriptionsUseIndependentDispatchChannels() {
        val visitor = MixedVisitor()
        TestPlSqlVisitorRunner.scanFile(mixedFixture(), null, visitor)

        assertThat(visitor.references.map { it.name }).containsExactly("tab")
        assertThat(visitor.rawVisitedTypes).containsExactly(PlSqlGrammar.VARIABLE_DECLARATION)
        assertThat(visitor.rawLeftTypes).containsExactly(PlSqlGrammar.VARIABLE_DECLARATION)
    }

    @Test
    fun typedAndRawSubscriptionsRemainIndependentAcrossScans() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)
        val firstScanCount = visitor.references.size

        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.references).hasSize(firstScanCount * 2)
        assertThat(visitor.rawTableReferenceCount).isEqualTo(firstScanCount * 2)
        assertThat(visitor.rawLeftTableReferenceCount).isEqualTo(firstScanCount * 2)
    }

    private fun fixture() = File("src/test/resources/syntax/table-references.sql")

    private fun mixedFixture() = File("src/test/resources/syntax/mixed-subscriptions.sql")

    private class RecordingVisitor : PlSqlVisitor() {
        val references = mutableListOf<TableReference>()
        var rawTableReferenceCount = 0
        var rawLeftTableReferenceCount = 0

        override fun init() {
            subscribeTo(DmlGrammar.TABLE_REFERENCE)
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { references += it }
        }

        override fun visitNode(node: AstNode) {
            if (node.type === DmlGrammar.TABLE_REFERENCE) {
                rawTableReferenceCount++
            }
        }

        override fun leaveNode(node: AstNode) {
            if (node.type === DmlGrammar.TABLE_REFERENCE) {
                rawLeftTableReferenceCount++
            }
        }
    }

    private class TypedOnlyVisitor : PlSqlVisitor() {
        val references = mutableListOf<TableReference>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { references += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }

    private class MixedVisitor : PlSqlVisitor() {
        val references = mutableListOf<TableReference>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION)
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { references += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }
}
