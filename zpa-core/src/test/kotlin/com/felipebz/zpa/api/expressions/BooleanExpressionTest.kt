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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class BooleanExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleAndExpression() {
        assertThat(p).matches("true and true")
    }

    @Test
    fun matchesSimpleOrExpression() {
        assertThat(p).matches("true or true")
    }

    @Test
    fun matchesMultipleExpression() {
        assertThat(p).matches("true and true or true")
    }

    @Test
    fun matchesMultipleExpressionWithNotOperator() {
        assertThat(p).matches("true and not false")
    }

    @Test
    fun matchesExpressionWithVariables() {
        assertThat(p).matches("var and var")
    }

    @Test
    fun matchesExpressionWithFunctionCall() {
        assertThat(p).matches("func(var) and func(var)")
    }

}
