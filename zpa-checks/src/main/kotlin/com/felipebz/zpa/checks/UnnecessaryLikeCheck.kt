//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR, tags = [Tags.CONFUSING])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnnecessaryLikeCheck : AbstractBaseCheck() {

    private val escapeRegex = Regex("[%_]")

    override fun init() {
        subscribeTo(ConditionsGrammar.LIKE_CONDITION)
    }

    override fun visitNode(node: AstNode) {
        val escapeChar = if (node.hasDescendant(PlSqlKeyword.ESCAPE) &&
            node.lastChild.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL))
            node.lastChild.tokenValue.trim('\'')
        else
            ""

        val regex = if (escapeChar.isEmpty()) escapeRegex else Regex("[^${Regex.escape(escapeChar)}][%_]")

        val likeNode = node.getFirstChild(PlSqlKeyword.LIKE).nextSibling
        if (likeNode.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL) && !likeNode.tokenValue.contains(regex)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}

