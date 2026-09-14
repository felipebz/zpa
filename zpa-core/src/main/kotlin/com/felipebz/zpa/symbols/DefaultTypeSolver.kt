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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.*

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

    open fun solveDatatype(node: AstNode, scope: Scope?): PlSqlDatatype {
        var type: PlSqlDatatype = UnknownDatatype
        if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE)) {
            type = CharacterDatatype(evaluateCharacterLength(node, scope))
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE)) {
            val constraint = node.firstChild.getFirstChildOrNull(PlSqlGrammar.NUMERIC_DATATYPE_CONSTRAINT)
            val precision = constraint
                ?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_PRECISION)
                ?.let { staticExpressionEvaluator.evaluateIntegerAsInt(it, scope) }
            val scale = constraint
                ?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_SCALE)
                ?.let { staticExpressionEvaluator.evaluateIntegerAsInt(it, scope) }
            type = NumericDatatype(precision, scale)
        } else if (node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE)) {
            type = DateDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE)) {
            type = LobDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE)) {
            type = BooleanDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.ANCHORED_DATATYPE)) {
            val anchoredDatatype = node.firstChild
            if (anchoredDatatype.lastChild.type === PlSqlKeyword.ROWTYPE) {
                type = RowtypeDatatype()
            }
        } else if (node.hasDirectChildren(PlSqlGrammar.JSON_DATATYPE)) {
            type = JsonDatatype()
        } else {
            val datatype = node.firstChild
            type = scope?.getSymbol(datatype.tokenValue, Symbol.Kind.TYPE)?.datatype ?: UnknownDatatype
        }
        return type
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

    open fun solveLiteral(node: AstNode): PlSqlDatatype {
        var type: PlSqlDatatype = UnknownDatatype
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node)) {
            type = NullDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL)) {
            type = CharacterDatatype(node)
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) {
            type = NumericDatatype(node)
        } else if (node.hasDirectChildren(PlSqlTokenType.DATE_LITERAL)) {
            type = DateDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_LITERAL)) {
            type = BooleanDatatype()
        }
        return type
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
