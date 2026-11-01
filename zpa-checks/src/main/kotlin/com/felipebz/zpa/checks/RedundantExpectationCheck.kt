//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.matchers.MethodMatcher

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class RedundantExpectationCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL)
    }

    override fun visitNode(node: AstNode) {
        if (expectMatcher.matches(node)) {
            val expectationNode = node.nextSibling.nextSibling

            val actualValue = expectMatcher.getArgumentsValues(node)[0]

            val matcherArguments = expectMatcher.getArgumentsValues(expectationNode)
            if (matcherArguments.isEmpty()) {
                return
            }

            val expectedValue = matcherArguments[0]
            if (CheckUtils.equalNodes(actualValue, expectedValue)) {
                addIssue(actualValue, getLocalizedMessage()).secondary(expectedValue, "Expected value")
            }
        }
    }

    companion object {
        private val expectMatcher = MethodMatcher.create()
            .packageName("UT")
            .name("EXPECT")
            .withNoParameterConstraint()
    }

}
