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

@Rule(priority = Priority.MINOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class NotFoundCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val parent = node.parent
        if (parent.typeIs(PlSqlGrammar.NOT_EXPRESSION) && node.numberOfChildren == 3) {

            val foundCandidate = node.lastChild
            val percentCandidate = foundCandidate.previousAstNode

            if (percentCandidate.typeIs(PlSqlPunctuator.MOD) && "FOUND" == foundCandidate.tokenValue) {
                addIssue(node, getLocalizedMessage())
            }
        }
    }

}
