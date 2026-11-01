//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

@OptIn(ZpaExperimentalApi::class)
internal class AstIfStatement(private val node: AstNode) : IfStatement {

    override val astNode: AstNode
        get() = node

    override val conditionAstNode: AstNode by lazy {
        conditionNode(node)
    }

    override val statementsAstNode: AstNode by lazy {
        node.getFirstChild(PlSqlGrammar.STATEMENTS)
    }

    override val statementAstNodes: List<AstNode> by lazy {
        statementsAstNode.getChildren(PlSqlGrammar.STATEMENT)
    }

    override val elsifBranches: List<IfElsifBranch> by lazy {
        node.getChildren(PlSqlGrammar.ELSIF_CLAUSE).map(::AstIfElsifBranch)
    }

    override val elseBranch: IfElseBranch? by lazy {
        node.getFirstChildOrNull(PlSqlGrammar.ELSE_CLAUSE)?.let(::AstIfElseBranch)
    }
}

@OptIn(ZpaExperimentalApi::class)
private class AstIfElsifBranch(private val node: AstNode) : IfElsifBranch {

    override val astNode: AstNode
        get() = node

    override val conditionAstNode: AstNode by lazy {
        conditionNode(node)
    }

    override val statementsAstNode: AstNode by lazy {
        node.getFirstChild(PlSqlGrammar.STATEMENTS)
    }

    override val statementAstNodes: List<AstNode> by lazy {
        statementsAstNode.getChildren(PlSqlGrammar.STATEMENT)
    }
}

private fun conditionNode(node: AstNode): AstNode {
    node.getFirstChildOrNull(PlSqlGrammar.EXPRESSION)?.let { return it }

    // EXPRESSION is skipped when it has one child. In that case, the condition
    // is the direct child immediately before the required THEN token. This
    // also remains correct when the IF statement has a leading LABEL.
    return node.children
        .takeWhile { it.type !== PlSqlKeyword.THEN }
        .last()
}

@OptIn(ZpaExperimentalApi::class)
private class AstIfElseBranch(private val node: AstNode) : IfElseBranch {

    override val astNode: AstNode
        get() = node

    override val statementsAstNode: AstNode by lazy {
        node.getFirstChild(PlSqlGrammar.STATEMENTS)
    }

    override val statementAstNodes: List<AstNode> by lazy {
        statementsAstNode.getChildren(PlSqlGrammar.STATEMENT)
    }
}
