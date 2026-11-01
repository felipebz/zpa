//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.CRITICAL, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class TooManyRowsHandlerCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.EXCEPTION_HANDLER)
    }

    override fun visitNode(node: AstNode) {
        // is a TOO_MANY_ROWS handler
        val exceptions = node.getChildren(PlSqlGrammar.VARIABLE_NAME)

        for (exception in exceptions) {
            val child = exception.firstChild

            if (child.typeIs(PlSqlGrammar.IDENTIFIER_NAME) && "TOO_MANY_ROWS".equals(child.tokenValue, ignoreCase = true)) {
                // and have only one NULL_STATEMENT
                val children = node.getFirstChild(PlSqlGrammar.STATEMENTS).children
                if (children.size == 1 && children[0].hasDirectChildren(PlSqlGrammar.NULL_STATEMENT)) {
                    addIssue(node, getLocalizedMessage())
                }
            }
        }
    }

}
