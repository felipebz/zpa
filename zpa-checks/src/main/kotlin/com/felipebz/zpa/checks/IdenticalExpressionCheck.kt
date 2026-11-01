//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class IdenticalExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val operator = node.getFirstChildOrNull(ConditionsGrammar.RELATIONAL_OPERATOR)
        if (operator != null) {

            val leftSide = node.firstChild
            val rightSide = node.lastChild

            if (CheckUtils.equalNodes(leftSide, rightSide)) {
                addIssue(leftSide, getLocalizedMessage(), operator.tokenValue)
                        .secondary(rightSide, "Original")
            }

        }
    }

}
