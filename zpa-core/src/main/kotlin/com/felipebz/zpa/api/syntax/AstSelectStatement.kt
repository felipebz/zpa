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
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

@OptIn(ZpaExperimentalApi::class)
internal class AstSelectStatement(private val node: AstNode) : SelectStatement {

    override val astNode: AstNode
        get() = node

    override val selectExpressionAstNode: AstNode by lazy {
        node.getFirstChild(DmlGrammar.SELECT_EXPRESSION)
    }

    override val queryBlocks: List<SelectQueryBlock> by lazy {
        logicalQueryBlocksInExpression(selectExpressionAstNode)
            .map(::AstSelectQueryBlock)
    }

    override val hasSetOperation: Boolean by lazy {
        hasTopLevelSetOperation(selectExpressionAstNode)
    }
}

@OptIn(ZpaExperimentalApi::class)
private class AstSelectQueryBlock(private val node: AstNode) : SelectQueryBlock {

    override val astNode: AstNode
        get() = node

    override val selectColumnAstNodes: List<AstNode> by lazy {
        node.getChildren(DmlGrammar.SELECT_COLUMN)
    }

    override val intoClause: SelectIntoClause? by lazy {
        node.getFirstChildOrNull(DmlGrammar.INTO_CLAUSE)?.let(::AstSelectIntoClause)
    }

    override val fromClauseAstNode: AstNode?
        get() = node.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE)

    override val whereClauseAstNode: AstNode?
        get() = node.getFirstChildOrNull(DmlGrammar.WHERE_CLAUSE)

    override val groupByClauseAstNode: AstNode?
        get() = node.getFirstChildOrNull(DmlGrammar.GROUP_BY_CLAUSE)

    override val havingClauseAstNode: AstNode?
        get() = node.getFirstChildOrNull(DmlGrammar.HAVING_CLAUSE)

    override val modelClauseAstNode: AstNode?
        get() = node.getFirstChildOrNull(DmlGrammar.MODEL_CLAUSE)
}

@OptIn(ZpaExperimentalApi::class)
private class AstSelectIntoClause(private val node: AstNode) : SelectIntoClause {

    override val astNode: AstNode
        get() = node

    override val isBulkCollect: Boolean
        get() = node.hasDirectChildren(PlSqlKeyword.BULK)
}

private fun logicalQueryBlocksInExpression(selectExpression: AstNode): List<AstNode> {
    return selectExpression.getChildren(DmlGrammar.QUERY_BLOCK).flatMap(::logicalQueryBlocksInQueryBlock)
}

private fun logicalQueryBlocksInQueryBlock(queryBlock: AstNode): List<AstNode> {
    if (queryBlock.getChildren(DmlGrammar.SELECT_COLUMN).isNotEmpty()) {
        return listOf(queryBlock)
    }

    val nestedSelectExpression = queryBlock.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION)
    return nestedSelectExpression?.let(::logicalQueryBlocksInExpression).orEmpty()
}

private fun hasTopLevelSetOperation(selectExpression: AstNode): Boolean {
    if (selectExpression.hasDirectChildren(
            PlSqlKeyword.UNION,
            PlSqlKeyword.INTERSECT,
            PlSqlKeyword.EXCEPT,
            PlSqlKeyword.MINUS_KEYWORD
        )
    ) {
        return true
    }

    return selectExpression.getChildren(DmlGrammar.QUERY_BLOCK).any { queryBlock ->
        queryBlock.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION)
            ?.let(::hasTopLevelSetOperation) == true
    }
}
