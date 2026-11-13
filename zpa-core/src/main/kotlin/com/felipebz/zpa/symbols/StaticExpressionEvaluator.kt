//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.project.toIntOrNull
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

    fun evaluateIntegerAsInt(node: AstNode?, scope: Scope?): Int? = evaluateInteger(node, scope)?.toIntOrNull()

    private fun evaluate(node: AstNode?, scope: Scope?, activeConstants: MutableSet<Symbol>): BigDecimal? =
        StaticArithmetic.evaluate(node) { name -> evaluateConstantReference(name, scope, activeConstants) }

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
}

/**
 * Literals, parentheses, unary signs and `+ - * /` over numbers. A `VARIABLE_NAME` or `MEMBER_EXPRESSION` is valued by
 * the caller's [name] function; anything else has no value.
 */
internal object StaticArithmetic {
    fun evaluate(node: AstNode?, name: (AstNode) -> BigDecimal?): BigDecimal? {
        if (node == null) return null
        return when (node.type) {
            PlSqlGrammar.LITERAL -> literal(node)
            PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION -> name(node)
            PlSqlGrammar.BRACKED_EXPRESSION -> parenthesized(node, name)
            PlSqlGrammar.UNARY_EXPRESSION -> unary(node, name)
            PlSqlGrammar.ADDITIVE_EXPRESSION -> binary(node, name, additive = true)
            PlSqlGrammar.MULTIPLICATIVE_EXPRESSION -> binary(node, name, additive = false)
            else -> if (node.children.size == 1) evaluate(node.firstChild, name) else null
        }
    }

    fun integer(node: AstNode?, name: (AstNode) -> BigDecimal?): BigInteger? = evaluate(node, name)?.toBigIntegerExactOrNull()

    private fun literal(node: AstNode): BigDecimal? {
        if (!node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) return null
        return try {
            BigDecimal(node.tokenValue)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun parenthesized(node: AstNode, name: (AstNode) -> BigDecimal?): BigDecimal? {
        val expression = node.children.singleOrNull {
            it.type !== PlSqlPunctuator.LPARENTHESIS && it.type !== PlSqlPunctuator.RPARENTHESIS
        }
        return evaluate(expression, name)
    }

    private fun unary(node: AstNode, name: (AstNode) -> BigDecimal?): BigDecimal? {
        if (node.children.size != 2) return parenthesized(node, name)
        val operand = evaluate(node.children[1], name) ?: return null
        return when (node.children[0].type) {
            PlSqlPunctuator.PLUS -> operand
            PlSqlPunctuator.MINUS -> operand.negate()
            else -> null
        }
    }

    private fun binary(node: AstNode, name: (AstNode) -> BigDecimal?, additive: Boolean): BigDecimal? {
        if (node.children.size < 3 || node.children.size % 2 == 0) return null
        var result = evaluate(node.children[0], name) ?: return null
        var index = 1
        while (index < node.children.size) {
            val right = evaluate(node.children[index + 1], name) ?: return null
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
}

private fun BigDecimal.toBigIntegerExactOrNull(): BigInteger? = try {
    toBigIntegerExact()
} catch (_: ArithmeticException) {
    null
}
