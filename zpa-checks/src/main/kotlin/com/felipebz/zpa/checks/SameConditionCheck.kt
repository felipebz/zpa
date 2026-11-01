//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class SameConditionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.AND_EXPRESSION)
        subscribeTo(PlSqlGrammar.OR_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val expressions = getExpressions(node)
        findSameConditions(expressions)
    }

    private fun findSameConditions(conditions: List<AstNode>) {
        for (i in 1 until conditions.size) {
            checkCondition(conditions, i)
        }
    }

    private fun checkCondition(conditions: List<AstNode>, index: Int) {
        val condition = conditions[index]
        for (j in 0 until index) {
            val otherCondition = conditions[j]
            if (CheckUtils.equalNodes(otherCondition, condition)) {
                addIssue(condition, getLocalizedMessage(), otherCondition.token.line)
                        .secondary(otherCondition, "Original")
                return
            }
        }
    }

    private fun getExpressions(node: AstNode): List<AstNode> {
        val expressions = ArrayList<AstNode>()
        for (subnode in node.children) {
            if (!subnode.typeIs(PlSqlKeyword.AND) && !subnode.typeIs(PlSqlKeyword.OR)) {
                expressions.add(subnode)
            }
        }
        return expressions
    }

}
