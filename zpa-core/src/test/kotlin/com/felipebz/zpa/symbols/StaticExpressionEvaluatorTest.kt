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
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.nio.charset.StandardCharsets

class StaticExpressionEvaluatorTest {

    private val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val expressionParser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val evaluator = StaticExpressionEvaluator()
    private lateinit var scope: ScopeImpl

    @BeforeEach
    fun setup() {
        scope = ScopeImpl()
    }

    @Test
    fun evaluatesSupportedNumericExpression() {
        assertThat(evaluate("2 + 3 * 4")).isEqualTo(BigInteger.valueOf(14))
        assertThat(evaluate("-(-5)")).isEqualTo(BigInteger.valueOf(5))
        assertThat(evaluate("(10 / 2) - 1")).isEqualTo(BigInteger.valueOf(4))
    }

    @Test
    fun rejectsUnsupportedOrNonIntegralExpression() {
        assertThat(evaluate("1 / 2")).isNull()
        assertThat(evaluate("abs(-5)")).isNull()
        assertThat(evaluate("unknown_name")).isNull()
    }

    @Test
    fun protectsCyclicConstantEvaluation() {
        val declarationA = parseVariableDeclaration("cycle_a constant number := cycle_b;")
        val declarationB = parseVariableDeclaration("cycle_b constant number := cycle_a;")
        addConstant(declarationA)
        addConstant(declarationB)

        assertThat(evaluate("cycle_a")).isNull()
    }

    private fun evaluate(expression: String): BigInteger? {
        expressionParser.setRootRule(expressionParser.grammar.rule(PlSqlGrammar.EXPRESSION))
        return evaluator.evaluateInteger(expressionParser.parse(expression), scope)
    }

    private fun parseVariableDeclaration(declaration: String) =
        parser.apply { setRootRule(parser.grammar.rule(PlSqlGrammar.VARIABLE_DECLARATION)) }
            .parse(declaration)

    private fun addConstant(declaration: AstNode) {
        val identifier = declaration.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val symbol = Symbol(identifier, Symbol.Kind.VARIABLE, scope, NumericDatatype())
        symbol.addModifiers(declaration.getChildren(PlSqlKeyword.CONSTANT))
        scope.addSymbol(symbol)
    }
}
