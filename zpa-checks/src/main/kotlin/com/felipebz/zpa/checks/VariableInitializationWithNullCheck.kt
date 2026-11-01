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
class VariableInitializationWithNullCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
    }

    override fun visitNode(node: AstNode) {
        if (node.hasParent(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.RECORD_FIELD_DECLARATION)) {
            val expression = node.lastChild
            if (CheckUtils.isNullLiteralOrEmptyString(expression)) {
                addIssue(node, getLocalizedMessage())
            }
        }
    }

}
