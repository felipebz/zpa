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
@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.*

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. There is no replacement: it was never
 * meant as API.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. There is no replacement: it " +
    "was never meant as API."
)
open class DefaultTypeSolver {

    private val staticExpressionEvaluator = StaticExpressionEvaluator()

    fun solve(node: AstNode, scope: Scope?): PlSqlDatatype {
        if (node.type === PlSqlGrammar.DATATYPE) {
            return solveDatatype(node, scope)
        } else if (node.type === PlSqlGrammar.LITERAL) {
            return solveLiteral(node)
        }
        return UnknownDatatype
    }

    open fun solveDatatype(node: AstNode, scope: Scope?): PlSqlDatatype = when {
        node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE) -> CharacterDatatype(evaluateCharacterLength(node, scope))
        node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE) -> {
            val constraint = node.firstChild.getFirstChildOrNull(PlSqlGrammar.NUMERIC_DATATYPE_CONSTRAINT)
            val precision = constraint
                ?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_PRECISION)
                ?.let { staticExpressionEvaluator.evaluateIntegerAsInt(it, scope) }
            val scale = constraint
                ?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_SCALE)
                ?.let { staticExpressionEvaluator.evaluateIntegerAsInt(it, scope) }
            NumericDatatype(precision, scale)
        }
        node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE) -> DateDatatype()
        node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE) -> LobDatatype()
        node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE) -> BooleanDatatype()
        node.hasDirectChildren(PlSqlGrammar.ANCHORED_DATATYPE) ->
            if (node.firstChild.lastChild.type === PlSqlKeyword.ROWTYPE) RowtypeDatatype() else UnknownDatatype
        node.hasDirectChildren(PlSqlGrammar.JSON_DATATYPE) -> JsonDatatype()
        else -> {
            val datatype = node.firstChild
            val member = datatype.getFirstChildOrNull(PlSqlGrammar.MEMBER_EXPRESSION)
            if (member != null) {
                val parts = member.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
                val path = parts.dropLast(1).map { it.tokenValue }.reversed()
                scope?.getSymbol(parts.last().tokenValue, path, Symbol.Kind.TYPE)?.datatype ?: UnknownDatatype
            } else {
                scope?.getSymbol(datatype.tokenValue, Symbol.Kind.TYPE)?.datatype ?: UnknownDatatype
            }
        }
    }

    private fun evaluateCharacterLength(node: AstNode, scope: Scope?): Int? {
        val constraint = node.firstChild.getFirstChildOrNull(PlSqlGrammar.CHARACTER_DATATYPE_CONSTRAINT)
            ?: return null
        val lengthExpression = constraint.children.firstOrNull {
            it.type !== PlSqlPunctuator.LPARENTHESIS &&
                it.type !== PlSqlPunctuator.RPARENTHESIS &&
                it.type !== PlSqlKeyword.BYTE &&
                it.type !== PlSqlKeyword.CHAR &&
                it.type !== PlSqlGrammar.CHARACTER_SET_CLAUSE
        }
        return staticExpressionEvaluator.evaluateIntegerAsInt(lengthExpression, scope)
    }

    open fun solveLiteral(node: AstNode): PlSqlDatatype = when {
        node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node) -> NullDatatype()
        node.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL) -> CharacterDatatype(node)
        node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL) -> NumericDatatype(node)
        node.hasDirectChildren(PlSqlTokenType.DATE_LITERAL) -> DateDatatype()
        node.hasDirectChildren(PlSqlGrammar.BOOLEAN_LITERAL) -> BooleanDatatype()
        else -> UnknownDatatype
    }

    private fun isEmptyString(node: AstNode): Boolean {
        val characterLiteral = node.getFirstChildOrNull(PlSqlGrammar.CHARACTER_LITERAL) ?: return false
        val value = characterLiteral.tokenValue
        val openingQuote = value.indexOf('\'')
        val closingQuote = value.lastIndexOf('\'')
        if (openingQuote < 0 || closingQuote <= openingQuote) {
            return false
        }

        val quotedContent = value.substring(openingQuote + 1, closingQuote)
        return if (value.substring(0, openingQuote).endsWith('q', ignoreCase = true)) {
            // Alternative-quoted literals include one opening and one closing delimiter.
            quotedContent.length == 2
        } else {
            // This includes regular and national character literals such as N''.
            quotedContent.isEmpty()
        }
    }

}
