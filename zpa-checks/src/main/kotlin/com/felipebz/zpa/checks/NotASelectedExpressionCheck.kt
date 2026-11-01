//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

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
