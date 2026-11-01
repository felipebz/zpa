//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ToDateWithoutFormatCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(SingleRowSqlFunctionsGrammar.TO_DATE_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val arguments = node.getChildren(PlSqlGrammar.ARGUMENT)
        if (arguments.size == 1 && !CheckUtils.isNullLiteralOrEmptyString(arguments.first().lastChild)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
