//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.syntax.IfStatement
import com.felipebz.zpa.api.syntax.SyntaxViews

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class DuplicateConditionIfElsifCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(SyntaxViews.IF_STATEMENT, ::visitIfStatement)
    }

    private fun visitIfStatement(statement: IfStatement) {
        val conditions = collectConditionsFromBranches(statement)
        findSameConditions(conditions)
    }

    private fun findSameConditions(branches: List<AstNode>) {
        for (i in 1 until branches.size) {
            checkCondition(branches, i)
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

    private fun collectConditionsFromBranches(statement: IfStatement) = buildList {
        add(statement.conditionAstNode)
        statement.elsifBranches.forEach { add(it.conditionAstNode) }
    }

}
