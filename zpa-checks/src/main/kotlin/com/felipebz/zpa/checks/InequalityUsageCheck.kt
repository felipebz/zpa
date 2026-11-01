//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR, tags = [Tags.OBSOLETE])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class InequalityUsageCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.NOTEQUALS_NONSTANDARD_OPERATOR)
    }

    override fun visitNode(node: AstNode) {
        addLineIssue(getLocalizedMessage(), node.tokenLine, node.tokens.joinToString("") { it.value })
    }

}
