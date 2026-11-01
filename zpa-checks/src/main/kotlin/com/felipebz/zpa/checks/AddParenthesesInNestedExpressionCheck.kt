//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class AddParenthesesInNestedExpressionCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.OR_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val andExpressions = node.getChildren(PlSqlGrammar.AND_EXPRESSION)
        for (andExpression in andExpressions) {
            addIssue(andExpression, getLocalizedMessage())
        }
    }

}
