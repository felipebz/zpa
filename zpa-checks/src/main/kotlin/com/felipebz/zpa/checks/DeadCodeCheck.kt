//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR, tags = [Tags.UNUSED])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class DeadCodeCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(*CheckUtils.terminationStatements)
        subscribeTo(PlSqlGrammar.METHOD_CALL)
    }

    override fun visitNode(node: AstNode) {
        if (CheckUtils.isTerminationStatement(node)) {
            var parent = node.parent
            while (!checkNode(parent)) {
                parent = parent.parent
            }
        }
    }

    private fun checkNode(node: AstNode?): Boolean {
        if (!shouldCheckNode(node) || node == null) {
            return true
        }
        val nextSibling = node.nextSiblingOrNull
        if (nextSibling != null && nextSibling.typeIs(PlSqlGrammar.STATEMENT)) {
            addIssue(nextSibling, getLocalizedMessage())
            return true
        }
        return false
    }

    private fun shouldCheckNode(node: AstNode?): Boolean {
        if (node == null || CheckUtils.isProgramUnit(node)) {
            return false
        }

        return if (node.typeIs(STATEMENT_OR_CALL)) {
            true
        } else {
            node.typeIs(STATEMENT_SECTION) && !node.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLERS)
        }
    }

    companion object {
        val STATEMENT_OR_CALL = arrayOf(PlSqlGrammar.STATEMENT, PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.CALL_STATEMENT)
        val STATEMENT_SECTION = arrayOf(PlSqlGrammar.STATEMENTS_SECTION, PlSqlGrammar.STATEMENTS)
    }

}
