/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.typeIs
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class NotASelectedExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val firstQueryBlock = node.getFirstChild(DmlGrammar.QUERY_BLOCK)
        if (!firstQueryBlock.children[1].typeIs(PlSqlKeyword.DISTINCT) || !node.hasDirectChildren(DmlGrammar.ORDER_BY_CLAUSE)) {
            return
        }

        val columns = firstQueryBlock.getChildren(DmlGrammar.SELECT_COLUMN)
        val orderByItems = node.getFirstChild(DmlGrammar.ORDER_BY_CLAUSE).getChildren(DmlGrammar.ORDER_BY_ITEM)

        for (orderByItem in orderByItems) {
            checkOrderByItem(orderByItem, columns)
        }
    }

    private fun checkOrderByItem(orderByItem: AstNode, columns: List<AstNode>) {
        val orderByItemValue = skipVariableName(orderByItem.firstChild)

        if (orderByItemValue.typeIs(PlSqlGrammar.LITERAL)) {
            return
        }

        var found = false
        for (column in columns) {
            val candidates = extractAcceptableValuesFromColumn(column)

            for (candidate in candidates) {
                if (CheckUtils.containsNode(candidate, orderByItemValue)) {
                    found = true
                }
            }
        }

        if (!found) {
            addIssue(orderByItemValue, getLocalizedMessage())
        }
    }

    private fun skipVariableName(node: AstNode): AstNode {
        return if (node.typeIs(PlSqlGrammar.VARIABLE_NAME)) {
            node.firstChild
        } else node
    }

    private fun extractAcceptableValuesFromColumn(column: AstNode): List<AstNode> {
        val values = ArrayList<AstNode>()

        val selectedExpression = skipVariableName(column.firstChild)
        values.add(selectedExpression)

        // if the value is "table.column", "column" can be used in order by
        if (selectedExpression.typeIs(PlSqlGrammar.MEMBER_EXPRESSION)) {
            values.add(selectedExpression.lastChild)
        }

        if (column.numberOfChildren > 1) {
            val alias = skipVariableName(column.lastChild)
            values.add(alias)
        }

        return values
    }

}
