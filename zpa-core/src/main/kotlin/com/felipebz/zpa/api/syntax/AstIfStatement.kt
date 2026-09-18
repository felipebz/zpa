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
