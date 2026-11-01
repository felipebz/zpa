//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MINOR, tags = [Tags.UNUSED])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class EmptyBlockCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.STATEMENTS_SECTION)
    }

    override fun visitNode(node: AstNode) {
        if (node.parentOrNull?.let(PlSqlStructure::isOverridingMember) == true) {
            return
        }

        val statements = node.getFirstChild(PlSqlGrammar.STATEMENTS).getChildren(PlSqlGrammar.STATEMENT)
        if (statements.size == 1) {
            val statement = statements[0]
            if (statement.hasDirectChildren(PlSqlGrammar.NULL_STATEMENT)) {
                addIssue(statement, getLocalizedMessage())
            }
        }
    }

}
