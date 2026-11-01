//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class RelationalExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesEqualTo() {
        assertThat(p).matches("1 = 1")
    }

    @Test
    fun matchesNotEqualTo() {
        assertThat(p).matches("1 <> 2")
        assertThat(p).matches("1 != 2")
        assertThat(p).matches("1 ~= 2")
        assertThat(p).matches("1 ^= 2")
    }

    @Test
    fun matchesLessThan() {
        assertThat(p).matches("1 < 2")
    }

    @Test
    fun matchesGreaterThen() {
        assertThat(p).matches("2 > 1")
    }

    @Test
    fun matchesLessThanOrEqualTo() {
        assertThat(p).matches("1 <= 2")
    }

    @Test
    fun matchesGreaterThenOrEqualTo() {
        assertThat(p).matches("2 >= 1")
    }

}
