//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class VariableInitializationWithFunctionCallCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
    }

    override fun visitNode(node: AstNode) {
        if (node.hasParent(PlSqlGrammar.VARIABLE_DECLARATION)) {
            val datatype = node.parent.getFirstChild(PlSqlGrammar.DATATYPE)

            val expression = node.lastChild
            if (expression.typeIs(PlSqlGrammar.METHOD_CALL) && datatype.tokenValue != expression.tokenValue) {
                addIssue(node, getLocalizedMessage())
            }
        }
    }

}
