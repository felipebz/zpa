//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ExplicitInParameterCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        if (!node.hasDirectChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
