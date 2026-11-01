//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
