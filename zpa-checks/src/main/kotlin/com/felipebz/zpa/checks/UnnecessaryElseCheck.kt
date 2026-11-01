//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnnecessaryElseCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.ELSE_CLAUSE)
    }

    override fun visitNode(node: AstNode) {
        val ifStatement = node.parent

        if (!hasElsifClause(ifStatement) && hasTerminationStatement(ifStatement)) {
            addLineIssue(getLocalizedMessage(), node.tokenLine)
        }
    }

    private fun hasElsifClause(node: AstNode): Boolean {
        return node.hasDirectChildren(PlSqlGrammar.ELSIF_CLAUSE)
    }

    private fun hasTerminationStatement(ifStatement: AstNode): Boolean {
        for (statement in ifStatement.getFirstChild(PlSqlGrammar.STATEMENTS).children) {
            val internal = statement.firstChild
            if (CheckUtils.isTerminationStatement(internal)) {
                return true
            }
        }
        return false
    }

}
