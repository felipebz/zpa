/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

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
