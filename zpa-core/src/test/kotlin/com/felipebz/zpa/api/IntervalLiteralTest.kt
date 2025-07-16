/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntervalLiteralTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.INTERVAL_LITERAL)
    }

    @Test
    fun intervalYearToMonthLiteral() {
        assertThat(p).matches("INTERVAL '4' YEAR")
        assertThat(p).matches("interval '4' year")
        assertThat(p).matches("INTERVAL '4' MONTH")
        assertThat(p).matches("INTERVAL '4' YEAR(3)")
        assertThat(p).matches("INTERVAL '4-2' YEAR TO MONTH")
    }

    @Test
    fun intervalDayToSecondLiteral() {
        assertThat(p).matches("INTERVAL '4' DAY")
        assertThat(p).matches("interval '4' day")
        assertThat(p).matches("INTERVAL '25' HOUR")
        assertThat(p).matches("INTERVAL '40' MINUTE")
        assertThat(p).matches("INTERVAL '120' HOUR(3)")
        assertThat(p).matches("INTERVAL '10' MINUTE")
        assertThat(p).matches("INTERVAL '10' HOUR")
        assertThat(p).matches("INTERVAL '10:22' MINUTE TO SECOND")
        assertThat(p).matches("INTERVAL '30.12345' SECOND(2,4)")
        assertThat(p).matches("INTERVAL '11:20' HOUR TO MINUTE")
        assertThat(p).matches("INTERVAL '11:12:10.2222222' HOUR TO SECOND(7)")
        assertThat(p).matches("INTERVAL '400' DAY(3)")
        assertThat(p).matches("INTERVAL '400 5' DAY(3) TO HOUR")
        assertThat(p).matches("INTERVAL '4 5:12' DAY TO MINUTE")
        assertThat(p).matches("INTERVAL '4 5:12:10.222' DAY TO SECOND(3)")
    }

}
