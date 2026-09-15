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
class SelectStatementSyntaxViewTest {

    @Test
    fun exposesTopLevelQueryStructure() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.statements).hasSize(8)

        val simple = visitor.statements[0]
        assertThat(simple.selectExpressionAstNode.type).isEqualTo(DmlGrammar.SELECT_EXPRESSION)
        assertThat(simple.queryBlocks).hasSize(1)
        assertThat(simple.hasSetOperation).isFalse
        assertThat(simple.queryBlocks.single().selectColumnAstNodes).hasSize(1)
        assertThat(simple.queryBlocks.single().intoClause).isNotNull
        assertThat(simple.queryBlocks.single().intoClause!!.isBulkCollect).isFalse
        assertThat(simple.queryBlocks.single().fromClauseAstNode).isNotNull
        assertThat(simple.queryBlocks.single().whereClauseAstNode).isNull()

        val bulkCollect = visitor.statements[1].queryBlocks.single().intoClause
        assertThat(bulkCollect).isNotNull
        assertThat(bulkCollect!!.isBulkCollect).isTrue

        val withWhere = visitor.statements[2].queryBlocks.single()
        assertThat(withWhere.selectColumnAstNodes).hasSize(2)
        assertThat(withWhere.whereClauseAstNode).isNotNull

        val setQuery = visitor.statements[3]
        assertThat(setQuery.queryBlocks).hasSize(2)
        assertThat(setQuery.queryBlocks.map { astTokens(it.astNode) })
            .containsExactly("select value into result from table_a", "select value from table_b")
        assertThat(setQuery.hasSetOperation).isTrue
        assertThat(setQuery.queryBlocks[0].intoClause).isNotNull
        assertThat(setQuery.queryBlocks[1].intoClause).isNull()
    }

    @Test
    fun excludesNestedQueryBlocksFromTopLevelView() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        val inlineView = visitor.statements[4]
        assertThat(inlineView.queryBlocks).hasSize(1)
        assertThat(inlineView.queryBlocks.single().selectColumnAstNodes).hasSize(1)

        val withClause = visitor.statements[5]
        assertThat(withClause.queryBlocks).hasSize(1)
        assertThat(withClause.queryBlocks.single().selectColumnAstNodes).hasSize(1)
        assertThat(astTokens(withClause.selectExpressionAstNode)).startsWith("with cte")

        val parenthesized = visitor.statements[6]
        assertThat(parenthesized.queryBlocks).hasSize(2)
        assertThat(parenthesized.hasSetOperation).isTrue
    }

    @Test
    fun labelsDoNotAffectSelectExtraction() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        val labeled = visitor.statements[7]
        assertThat(labeled.queryBlocks).hasSize(1)
        assertThat(labeled.queryBlocks.single().selectColumnAstNodes).hasSize(1)
        assertThat(labeled.queryBlocks.single().intoClause).isNotNull
    }

    @Test
    fun typedOnlySubscriptionDoesNotInvokeRawCallbacks() {
        val visitor = TypedOnlyVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.statements).hasSize(8)
        assertThat(visitor.rawVisitedTypes).isEmpty()
        assertThat(visitor.rawLeftTypes).isEmpty()
    }

    @Test
    fun selectAndTableViewsUseSeparateTypedChannels() {
        val visitor = MultiViewVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.statements).hasSize(8)
        assertThat(visitor.tables.map { it.name }).contains("table_a", "table_b", "inner_table", "cte")
        assertThat(visitor.statements).allMatch { it.astNode.type === PlSqlGrammar.SELECT_STATEMENT }
    }

    private fun fixture() = File("src/test/resources/syntax/select-statements.sql")

    private class RecordingVisitor : PlSqlVisitor() {
        val statements = mutableListOf<SelectStatement>()

        override fun init() {
            subscribeTo(SyntaxViews.SELECT_STATEMENT) { statements += it }
        }
    }

    private class TypedOnlyVisitor : PlSqlVisitor() {
        val statements = mutableListOf<SelectStatement>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(SyntaxViews.SELECT_STATEMENT) { statements += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }

    private class MultiViewVisitor : PlSqlVisitor() {
        val statements = mutableListOf<SelectStatement>()
        val tables = mutableListOf<TableReference>()

        override fun init() {
            subscribeTo(SyntaxViews.SELECT_STATEMENT) { statements += it }
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { tables += it }
        }
    }
}

private fun astTokens(node: AstNode) = node.tokens.joinToString(" ") { it.originalValue }
