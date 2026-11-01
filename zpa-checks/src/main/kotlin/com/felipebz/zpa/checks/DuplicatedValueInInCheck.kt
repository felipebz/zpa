//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.BLOCKER, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class DuplicatedValueInInCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.IN_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val values = getInValue(node)
        findSameValues(values)
    }

    private fun getInValue(inExpression: AstNode): List<AstNode> {
        val values = ArrayList<AstNode>()
        var current = inExpression.getFirstChildOrNull(PlSqlPunctuator.LPARENTHESIS)
        while (current != null) {
            current = current.nextSibling

            if (current.typeIs(PlSqlPunctuator.RPARENTHESIS)) {
                current = null
            } else if (!current.typeIs(PlSqlPunctuator.COMMA)) {
                values.add(current)
            }
        }
        return values
    }

    private fun findSameValues(values: List<AstNode>) {
        for (i in 1 until values.size) {
            checkValue(values, i)
        }
    }

    private fun checkValue(values: List<AstNode>, index: Int) {
        val current = values[index]

        for (j in 0 until index) {
            val other = values[j]

            if (CheckUtils.equalNodes(current, other)) {
                addIssue(current, getLocalizedMessage(), current.tokenOriginalValue)
                        .secondary(other, "Original")
                return
            }
        }
    }

}
