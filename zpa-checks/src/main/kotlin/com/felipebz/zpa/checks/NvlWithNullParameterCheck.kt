//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.matchers.MethodMatcher
import com.felipebz.zpa.api.syntax.MethodCall
import com.felipebz.zpa.api.syntax.SyntaxViews

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class NvlWithNullParameterCheck : AbstractBaseCheck() {

    private val nvl = MethodMatcher.create().name("nvl").addParameters(2)

    override fun init() {
        subscribeTo(SyntaxViews.METHOD_CALL, ::visitMethodCall)
    }

    private fun visitMethodCall(call: MethodCall) {
        if (!nvl.matches(call)) {
            return
        }

        for (argument in call.argumentLists.firstOrNull().orEmpty()) {
            val expression = argument.expressionAstNode

            if (CheckUtils.isNullLiteralOrEmptyString(expression)) {
                addIssue(call, getLocalizedMessage(), expression.tokenValue)
            }
        }
    }

}
