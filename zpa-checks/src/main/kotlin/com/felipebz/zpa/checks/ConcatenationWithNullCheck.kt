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
class ConcatenationWithNullCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CONCATENATION_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        for (child in node.children) {
            if (CheckUtils.isNullLiteralOrEmptyString(child)) {
                addIssue(child, getLocalizedMessage())
            }
        }
    }

}
