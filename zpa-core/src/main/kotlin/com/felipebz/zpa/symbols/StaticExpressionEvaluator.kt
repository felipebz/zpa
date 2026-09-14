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
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Conservatively evaluates numeric expressions whose value is needed by a
 * compile-time datatype constraint. Unsupported or unresolved expressions
 * produce no value.
 */
internal class StaticExpressionEvaluator {

    fun evaluateInteger(node: AstNode?, scope: Scope?): BigInteger? {
        return evaluate(node, scope, mutableSetOf())?.toBigIntegerExactOrNull()
    }

    fun evaluateIntegerAsInt(node: AstNode?, scope: Scope?): Int? {
        return evaluateInteger(node, scope)?.let {
            try {
                it.intValueExact()
            } catch (_: ArithmeticException) {
                null
            }
        }
    }

    private fun evaluate(node: AstNode?, scope: Scope?, activeConstants: MutableSet<Symbol>): BigDecimal? {
        if (node == null) return null

        return when (node.type) {
            PlSqlGrammar.LITERAL -> evaluateLiteral(node)
            PlSqlGrammar.VARIABLE_NAME,
            PlSqlGrammar.MEMBER_EXPRESSION -> evaluateConstantReference(node, scope, activeConstants)
            PlSqlGrammar.BRACKED_EXPRESSION -> evaluateParenthesized(node, scope, activeConstants)
            PlSqlGrammar.UNARY_EXPRESSION -> evaluateUnary(node, scope, activeConstants)
            PlSqlGrammar.ADDITIVE_EXPRESSION -> evaluateBinary(node, scope, activeConstants, additive = true)
            PlSqlGrammar.MULTIPLICATIVE_EXPRESSION -> evaluateBinary(node, scope, activeConstants, additive = false)
            else -> evaluateTransparentWrapper(node, scope, activeConstants)
        }
    }

    private fun evaluateLiteral(node: AstNode): BigDecimal? {
        if (!node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) return null
        return try {
            BigDecimal(node.tokenValue)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun evaluateParenthesized(
        node: AstNode,
        scope: Scope?,
        activeConstants: MutableSet<Symbol>
    ): BigDecimal? {
        val expression = node.children.singleOrNull {
            it.type !== PlSqlPunctuator.LPARENTHESIS && it.type !== PlSqlPunctuator.RPARENTHESIS
        }
        return evaluate(expression, scope, activeConstants)
    }

    private fun evaluateUnary(
        node: AstNode,
        scope: Scope?,
        activeConstants: MutableSet<Symbol>
    ): BigDecimal? {
        if (node.children.size == 2) {
            val operand = evaluate(node.children[1], scope, activeConstants) ?: return null
            return when (node.children[0].type) {
                PlSqlPunctuator.PLUS -> operand
                PlSqlPunctuator.MINUS -> operand.negate()
                else -> null
            }
        }

        return evaluateParenthesized(node, scope, activeConstants)
    }

    private fun evaluateBinary(
        node: AstNode,
        scope: Scope?,
        activeConstants: MutableSet<Symbol>,
        additive: Boolean
    ): BigDecimal? {
        if (node.children.size < 3 || node.children.size % 2 == 0) return null

        var result = evaluate(node.children[0], scope, activeConstants) ?: return null
        var index = 1
        while (index < node.children.size) {
            val right = evaluate(node.children[index + 1], scope, activeConstants) ?: return null
            result = when {
                additive && node.children[index].type === PlSqlPunctuator.PLUS -> result.add(right)
                additive && node.children[index].type === PlSqlPunctuator.MINUS -> result.subtract(right)
                !additive && node.children[index].type === PlSqlPunctuator.MULTIPLICATION -> result.multiply(right)
                !additive && node.children[index].type === PlSqlPunctuator.DIVISION -> try {
                    result.divide(right)
                } catch (_: ArithmeticException) {
                    return null
                }
                else -> return null
            }
            index += 2
        }

        return result
    }

    private fun evaluateTransparentWrapper(
        node: AstNode,
        scope: Scope?,
        activeConstants: MutableSet<Symbol>
    ): BigDecimal? {
        return if (node.children.size == 1) {
            evaluate(node.firstChild, scope, activeConstants)
        } else {
            null
        }
    }

    private fun evaluateConstantReference(
        node: AstNode,
        scope: Scope?,
        activeConstants: MutableSet<Symbol>
    ): BigDecimal? {
        val symbol = resolveSymbol(node, scope) ?: return null
        if (!symbol.hasModifier("constant") || !activeConstants.add(symbol)) return null

        return try {
            val declaration = symbol.node ?: return null
            val initializer = declaration.parentOrNull
                ?.takeIf { it.type === PlSqlGrammar.VARIABLE_DECLARATION }
                ?.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
                ?.lastChildOrNull
            evaluate(initializer, symbol.scope, activeConstants)
        } finally {
            activeConstants.remove(symbol)
        }
    }

    private fun resolveSymbol(node: AstNode, scope: Scope?): Symbol? {
        scope ?: return null

        return if (node.type === PlSqlGrammar.VARIABLE_NAME) {
            scope.getSymbol(node.tokenValue, Symbol.Kind.VARIABLE)
        } else {
            val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
            if (parts.isEmpty()) {
                null
            } else {
                val path = parts.dropLast(1).map { it.tokenValue }.reversed()
                scope.getSymbol(parts.last().tokenValue, path, Symbol.Kind.VARIABLE)
            }
        }
    }

    private fun BigDecimal.toBigIntegerExactOrNull(): BigInteger? {
        return try {
            toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }
    }
}
