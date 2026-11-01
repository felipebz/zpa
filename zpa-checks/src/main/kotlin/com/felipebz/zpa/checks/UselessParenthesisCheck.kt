//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MINOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UselessParenthesisCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.BRACKED_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val parent = node.parent
        if (parent.typeIs(PlSqlGrammar.BRACKED_EXPRESSION) && parent.numberOfChildren == 3) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
