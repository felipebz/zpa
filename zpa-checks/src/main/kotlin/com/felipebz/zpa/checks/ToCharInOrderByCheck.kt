/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.matchers.MethodMatcher
import java.math.BigDecimal
import java.math.RoundingMode

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

        val position = position(expression) ?: return
        val selectExpression = node.getFirstAncestor(DmlGrammar.SELECT_EXPRESSION)
        val queryBlock = selectExpression.getFirstChild(DmlGrammar.QUERY_BLOCK)
        val columns = queryBlock.getChildren(DmlGrammar.SELECT_COLUMN)

        if (position >= BigDecimal.ONE && position <= columns.size.toBigDecimal() && toChar.matches(columns[position.toInt() - 1].firstChild)) {
            addIssue(node, getLocalizedMessage())
        }
    }

    private fun position(expression: AstNode): BigDecimal? = when (expression.type) {
        PlSqlGrammar.BRACKED_EXPRESSION ->
            expression.children.takeIf { it.size == 3 && it[0].type === PlSqlPunctuator.LPARENTHESIS && it[2].type === PlSqlPunctuator.RPARENTHESIS }
                ?.let { position(it[1]) }
        PlSqlGrammar.UNARY_EXPRESSION ->
            expression.children.takeIf { it.size == 2 && it[0].type === PlSqlPunctuator.PLUS }?.let { position(it[1]) }
        PlSqlGrammar.EXPRESSION, PlSqlGrammar.PRIMARY_EXPRESSION -> expression.children.singleOrNull()?.let(::position)
        PlSqlGrammar.LITERAL -> expression.getFirstChildOrNull(PlSqlGrammar.NUMERIC_LITERAL)?.tokenValue
            ?.takeUnless { it.endsWith('f', ignoreCase = true) || it.endsWith('d', ignoreCase = true) }
            ?.toBigDecimalOrNull()
            ?.let(::truncated)
        else -> null
    }

    private fun truncated(value: BigDecimal): BigDecimal? {
        val integerDigits = value.precision().toLong() - value.scale()
        return if (integerDigits in 1..MAX_POSITION_DIGITS) value.setScale(0, RoundingMode.DOWN) else null
    }

    companion object {
        private val toChar = MethodMatcher.create().name("to_char").withNoParameterConstraint()

        /** More than the columns a select list can have (1000): any longer integer is out of range. */
        private const val MAX_POSITION_DIGITS = 10L
    }

}
