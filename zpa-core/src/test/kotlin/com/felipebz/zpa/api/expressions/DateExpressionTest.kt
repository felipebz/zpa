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

class DateExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesDateLiteralWithoutLeadingZeros() {
        assertThat(p).matches("date '1-1-1'")
        assertThat(p).matches("date '2024-2-22'")
    }

    @Test
    fun matchesDateLiteralAddition() {
        assertThat(p).matches("DATE '2015-01-01' + 1")
    }

    @Test
    fun matchesDateLiteralSubtraction() {
        assertThat(p).matches("DATE '2015-01-01' - 1")
    }


    @Test
    fun matchesDateLiteralWithPadding() {
        assertThat(p).matches("date' 2023-12-31'")
        assertThat(p).matches("date '2023-12-31 '")
    }

    @Test
    fun doesNotMatchDateLiteralWithPaddingInsideTheValue() {
        assertThat(p).notMatches("date '2023- 12-31'")
    }
}
