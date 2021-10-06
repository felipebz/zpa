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

import com.felipebz.flr.api.AstNode
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.matchers.MethodMatcher
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
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
            addIssue(node, getLocalizedMessage())
        }

        if (expression.type === PlSqlGrammar.LITERAL && semantic(expression).plSqlType === PlSqlType.NUMERIC) {
            val index = Integer.parseInt(expression.tokenOriginalValue)

            val selectExpression = node.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION)
            val queryBlock = selectExpression.getFirstChild(DmlGrammar.QUERY_BLOCK)
            val columns = queryBlock.getChildren(DmlGrammar.SELECT_COLUMN)

            if (columns.size >= index) {
                val selectColumn = columns[index - 1].firstChild

                if (toChar.matches(selectColumn)) {
                    addIssue(node, getLocalizedMessage())
                }
            }
        }
    }

    companion object {
        private val toChar = MethodMatcher.create().name("to_char").withNoParameterConstraint()
    }

}
