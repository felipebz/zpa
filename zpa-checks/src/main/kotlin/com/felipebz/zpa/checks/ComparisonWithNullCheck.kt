//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ComparisonWithNullCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val children = node.getChildren(PlSqlGrammar.LITERAL)
        for (child in children) {
            if (CheckUtils.isNullLiteralOrEmptyString(child)) {
                val operator = node.getFirstChild(ConditionsGrammar.RELATIONAL_OPERATOR)
                val suggestion = if (operator.firstChild.typeIs(PlSqlGrammar.EQUALS_OPERATOR)) {
                    "IS NULL"
                } else {
                    "IS NOT NULL"
                }

                addIssue(node, getLocalizedMessage(), suggestion)
            }
        }
    }

}
