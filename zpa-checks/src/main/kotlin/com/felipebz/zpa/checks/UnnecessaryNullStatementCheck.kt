//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR)
@ConstantRemediation("1min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnnecessaryNullStatementCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.NULL_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        if (node.hasDirectChildren(PlSqlGrammar.LABEL)) return

        val parent = node.parent
        if (parent.previousSiblingOrNull != null || parent.nextSiblingOrNull != null) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
