/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
import org.sonar.plsqlopen.checks.ToCharInOrderByCheck.Companion.CHECK_KEY
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.matchers.MethodMatcher
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

@Rule(key = CHECK_KEY, priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class ToCharInOrderByCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.ORDER_BY_ITEM)
    }

    override fun visitNode(node: AstNode) {
        val expression = node.firstChild

        if (toChar.matches(expression)) {
            addIssue(node, getLocalizedMessage(CHECK_KEY))
        }

        if (expression.type === PlSqlGrammar.LITERAL && semantic(expression).plSqlType === PlSqlType.NUMERIC) {
            val index = Integer.parseInt(expression.tokenOriginalValue)

            val selectExpression = node.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION)
            val columns = selectExpression.getChildren(DmlGrammar.SELECT_COLUMN)

            if (columns.size >= index) {
                val selectColumn = columns[index - 1].firstChild

                if (toChar.matches(selectColumn)) {
                    addIssue(node, getLocalizedMessage(CHECK_KEY))
                }
            }
        }
    }

    companion object {
        internal const val CHECK_KEY = "ToCharInOrderBy"
        private val toChar = MethodMatcher.create().name("to_char").withNoParameterConstraint()
    }

}
