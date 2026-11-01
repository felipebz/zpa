//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("1h")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class FunctionWithOutParameterCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        if (node.parent.hasParent(PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.CREATE_FUNCTION) && node.hasDirectChildren(PlSqlKeyword.OUT)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
