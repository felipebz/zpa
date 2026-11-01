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

class ExtractDatetimeExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesExtractDay() {
        assertThat(p).matches("extract(day from foo)")
    }

    @Test
    fun matchesExtractMonth() {
        assertThat(p).matches("extract(month from foo)")
    }

    @Test
    fun matchesExtractYear() {
        assertThat(p).matches("extract(year from foo)")
    }

    @Test
    fun matchesExtractYour() {
        assertThat(p).matches("extract(hour from foo)")
    }

    @Test
    fun matchesExtractMinute() {
        assertThat(p).matches("extract(minute from foo)")
    }

    @Test
    fun matchesExtractSecond() {
        assertThat(p).matches("extract(second from foo)")
    }

    @Test
    fun matchesExtractTimezoneHour() {
        assertThat(p).matches("extract(timezone_hour from foo)")
    }

    @Test
    fun matchesExtractTimezoneMinute() {
        assertThat(p).matches("extract(timezone_minute from foo)")
    }

    @Test
    fun matchesExtractTimezoneRegion() {
        assertThat(p).matches("extract(timezone_region from foo)")
    }

    @Test
    fun matchesExtractTimezoneAbbreviation() {
        assertThat(p).matches("extract(timezone_abbr from foo)")
    }

}
