//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.INFO)
@ConstantRemediation("1min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class DeclareSectionWithoutDeclarationsCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.BLOCK_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        if (node.hasDirectChildren(PlSqlKeyword.DECLARE) && !node.hasDescendant(PlSqlGrammar.DECLARE_SECTION)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
