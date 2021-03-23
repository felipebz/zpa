/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = QueryWithoutExceptionHandlingCheck.CHECK_KEY, priority = Priority.CRITICAL)
@ConstantRemediation("20min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
class QueryWithoutExceptionHandlingCheck : AbstractBaseCheck() {

    @RuleProperty(key = "strict", defaultValue = "true")
    var strictMode = true

    override fun init() {
        subscribeTo(PlSqlGrammar.SELECT_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val intoClause = node.getFirstDescendant(DmlGrammar.INTO_CLAUSE)
        if (intoClause != null  && intoClause.firstChild.typeIs(PlSqlKeyword.BULK)) {
            return
        }

        if (strictMode) {
            val parentBlock = node.getFirstAncestor(PlSqlGrammar.STATEMENTS_SECTION)

            if (!parentBlock.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLER)) {
                addIssue(node, getLocalizedMessage(CHECK_KEY))
            }
        } else {
            val hasExceptionHandler = context.currentScope?.hasExceptionHandler() ?: false
            if (!hasExceptionHandler) {
                addIssue(node, getLocalizedMessage(CHECK_KEY))
            }
        }
    }

    companion object {
        internal const val CHECK_KEY = "QueryWithoutExceptionHandling"
    }

}
