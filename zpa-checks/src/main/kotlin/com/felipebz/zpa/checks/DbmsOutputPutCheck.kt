//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.matchers.MethodMatcher
import com.felipebz.zpa.api.syntax.MethodCall
import com.felipebz.zpa.api.syntax.SyntaxViews

@Rule(priority = Priority.MINOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class DbmsOutputPutCheck : AbstractBaseCheck() {

    private val putMatcher = MethodMatcher.create()
        .schema("sys").schemaIsOptional()
        .packageName("dbms_output")
        .name("put")
        .addParameter()

    private val putLineMatcher = MethodMatcher.create()
        .schema("sys").schemaIsOptional()
        .packageName("dbms_output")
        .name("put_line")
        .addParameter()

    override fun init() {
        subscribeTo(SyntaxViews.METHOD_CALL, ::visitMethodCall)
    }

    private fun visitMethodCall(call: MethodCall) {
        if (!putMatcher.matches(call) && !putLineMatcher.matches(call)) {
            return
        }

        addIssue(call, getLocalizedMessage())
    }

}
