/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.checks

import com.sonar.sslr.api.AstNode
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*

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
            node.typeIs(STATEMENT_SECTION) && !node.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLER)
        }
    }

    companion object {
        val STATEMENT_OR_CALL = arrayOf(PlSqlGrammar.STATEMENT, PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.CALL_STATEMENT)
        val STATEMENT_SECTION = arrayOf(PlSqlGrammar.STATEMENTS_SECTION, PlSqlGrammar.STATEMENTS)
    }

}
