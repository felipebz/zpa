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

class NotExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.NOT_EXPRESSION)
    }

    @Test
    fun matchesSimpleNotExpression() {
        assertThat(p).matches("not x")
    }

    @Test
    fun matchesNotExpression() {
        assertThat(p).matches("not x = a")
    }

    @Test
    fun matchesNotExpressionWithParenthesis() {
        assertThat(p).matches("not (x = a)")
    }

}
