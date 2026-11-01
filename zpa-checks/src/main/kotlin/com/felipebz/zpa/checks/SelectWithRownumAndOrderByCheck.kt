//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("20min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SelectWithRownumAndOrderByCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        if (!hasOrderByClause(node)) {
            return
        }

        val queryBlock = node.getFirstChild(DmlGrammar.QUERY_BLOCK)

        val whereClause = queryBlock.getChildren(DmlGrammar.WHERE_CLAUSE)
        if (whereClause.isEmpty()) {
            return
        }

        val whereComparisonConditions = whereClause[0].getDescendants(PlSqlGrammar.COMPARISON_EXPRESSION)
        if (whereComparisonConditions.isEmpty()) {
            return
        }

        for (comparison in whereComparisonConditions) {
            for (child in comparison.getChildren(PlSqlGrammar.VARIABLE_NAME)) {
                if ("rownum".equals(child.tokenValue, ignoreCase = true) && node == child.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION)) {
                    addIssue(comparison, getLocalizedMessage())
                }
            }
        }
    }

    private fun hasOrderByClause(node: AstNode): Boolean {
        return node.hasDirectChildren(DmlGrammar.ORDER_BY_CLAUSE)
    }

}
