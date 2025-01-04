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
package org.sonar.plugins.plsqlopen.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class JsonArrayAggTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonArrayAgg() {
        assertThat(p).matches("json_arrayagg(1)")
    }

    @Test
    fun matchesJsonArrayAggWithFormat() {
        assertThat(p).matches("json_arrayagg('{}' format json)")
    }

    @Test
    fun matchesJsonArrayAggWithOrderBy() {
        assertThat(p).matches("json_arrayagg(foo order by col)")
    }

    @Test
    fun matchesJsonArrayAggWithNullOnNull() {
        assertThat(p).matches("json_arrayagg(foo null on null)")
    }

    @Test
    fun matchesJsonArrayAggWithAbsentOnNull() {
        assertThat(p).matches("json_arrayagg(foo absent on null)")
    }

    @Test
    fun matchesJsonArrayAggWithReturning() {
        assertThat(p).matches("json_arrayagg(foo returning json)")
    }

    @Test
    fun matchesJsonArrayAggWithStrict() {
        assertThat(p).matches("json_arrayagg(foo strict)")
    }

    @Test
    fun matchesLongJsonArrayAgg() {
        assertThat(p).matches("json_arrayagg(foo order by col null on null returning json strict)")
    }

}
