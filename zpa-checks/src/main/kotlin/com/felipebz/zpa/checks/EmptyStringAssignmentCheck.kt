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
class EmptyStringAssignmentCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.ASSIGNMENT_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val value = node.getLastChildOrNull(PlSqlGrammar.LITERAL)

        if (value != null && CheckUtils.isEmptyString(value)) {
            addIssue(node, getLocalizedMessage())
        }
    }

}
