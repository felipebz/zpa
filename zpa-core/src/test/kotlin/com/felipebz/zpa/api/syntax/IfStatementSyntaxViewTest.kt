//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ZpaExperimentalApi::class)
class IfStatementSyntaxViewTest {

    @Test
    fun exposesBranchesWithoutDependingOnLabelsOrChildPositions() {
        val visitor = RecordingVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.statements).hasSize(5)

        val simple = visitor.statement("a = 1")
        assertThat(simple.statementsAstNode.type).isEqualTo(PlSqlGrammar.STATEMENTS)
        assertThat(simple.statementAstNodes).hasSize(1)
        assertThat(simple.statementAstNodes).allMatch { it.type === PlSqlGrammar.STATEMENT }
        assertThat(simple.elsifBranches).isEmpty()
        assertThat(simple.elseBranch).isNull()

        val multipleBranches = visitor.statement("b = 1")
        assertThat(multipleBranches.elsifBranches).hasSize(2)
        assertThat(multipleBranches.elsifBranches.map { astTokens(it.conditionAstNode) })
            .containsExactly("b = 2", "b = 3")
        multipleBranches.elsifBranches.forEach {
            assertThat(it.statementsAstNode.type).isEqualTo(PlSqlGrammar.STATEMENTS)
            assertThat(it.statementAstNodes).hasSize(1)
            assertThat(it.statementAstNodes).allMatch { statement -> statement.type === PlSqlGrammar.STATEMENT }
        }
        assertThat(multipleBranches.elseBranch).isNotNull
        assertThat(multipleBranches.elseBranch!!.statementsAstNode.type).isEqualTo(PlSqlGrammar.STATEMENTS)
        assertThat(multipleBranches.elseBranch!!.statementAstNodes).hasSize(1)
        assertThat(multipleBranches.elseBranch!!.statementAstNodes)
            .allMatch { it.type === PlSqlGrammar.STATEMENT }

        val nestedOuter = visitor.statement("outer_condition")
        assertThat(nestedOuter.statementAstNodes).hasSize(1)
        assertThat(nestedOuter.statementAstNodes.single().getFirstChild(PlSqlGrammar.IF_STATEMENT))
            .isNotNull
        assertThat(visitor.statement("inner_condition").statementAstNodes).hasSize(1)

        val labeled = visitor.statement("labeled_condition")
        assertThat(astTokens(labeled.conditionAstNode)).isEqualTo("labeled_condition")
        assertThat(labeled.statementsAstNode.type).isEqualTo(PlSqlGrammar.STATEMENTS)
    }

    @Test
    fun typedOnlySubscriptionDoesNotInvokeRawCallbacks() {
        val visitor = TypedOnlyVisitor()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, visitor)

        assertThat(visitor.statements).hasSize(5)
        assertThat(visitor.rawVisitedTypes).isEmpty()
        assertThat(visitor.rawLeftTypes).isEmpty()
    }

    @Test
    fun multipleTypedViewsReceiveOnlyTheirOwnNodes() {
        val visitor = AllViewsVisitor()
        TestPlSqlVisitorRunner.scanFile(dispatchFixture(), null, visitor)

        assertThat(visitor.variables.map { it.name }).containsExactly("local_value")
        assertThat(visitor.calls.map { it.name }).containsExactly("function_call")
        assertThat(visitor.tables.map { it.name }).containsExactly("table_name")
        assertThat(visitor.ifStatements).hasSize(1)
    }

    private fun fixture() = File("src/test/resources/syntax/if-statements.sql")

    private fun dispatchFixture() = File("src/test/resources/syntax/if-view-dispatch.sql")

    private class RecordingVisitor : PlSqlVisitor() {
        val statements = mutableListOf<IfStatement>()

        override fun init() {
            subscribeTo(SyntaxViews.IF_STATEMENT) { statements += it }
        }

        fun statement(condition: String) = statements.first { astTokens(it.conditionAstNode) == condition }
    }

    private class TypedOnlyVisitor : PlSqlVisitor() {
        val statements = mutableListOf<IfStatement>()
        val rawVisitedTypes = mutableListOf<AstNodeType>()
        val rawLeftTypes = mutableListOf<AstNodeType>()

        override fun init() {
            subscribeTo(SyntaxViews.IF_STATEMENT) { statements += it }
        }

        override fun visitNode(node: AstNode) {
            rawVisitedTypes += node.type
        }

        override fun leaveNode(node: AstNode) {
            rawLeftTypes += node.type
        }
    }

    private class AllViewsVisitor : PlSqlVisitor() {
        val tables = mutableListOf<TableReference>()
        val calls = mutableListOf<MethodCall>()
        val variables = mutableListOf<VariableDeclaration>()
        val ifStatements = mutableListOf<IfStatement>()

        override fun init() {
            subscribeTo(SyntaxViews.TABLE_REFERENCE) { tables += it }
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
            subscribeTo(SyntaxViews.VARIABLE_DECLARATION) { variables += it }
            subscribeTo(SyntaxViews.IF_STATEMENT) { ifStatements += it }
        }
    }
}

private fun astTokens(node: AstNode) = node.tokens.joinToString(" ") { it.originalValue }
