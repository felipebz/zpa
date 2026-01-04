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

class JsonArrayTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonArrayWithOneElement() {
        assertThat(p).matches("json_array(1)")
    }

    @Test
    fun matchesJsonArrayWithManyElement() {
        assertThat(p).matches("json_array(1, 'test', sysdate)")
    }

    @Test
    fun matchesJsonArrayWithFormat() {
        assertThat(p).matches("json_array('{}' format json)")
    }

    @Test
    fun matchesJsonArrayWithNullOnNull() {
        assertThat(p).matches("json_array(foo null on null)")
    }

    @Test
    fun matchesJsonArrayWithAbsentOnNull() {
        assertThat(p).matches("json_array(foo absent on null)")
    }

    @Test
    fun matchesJsonArrayWithReturning() {
        assertThat(p).matches("json_array(foo returning json)")
    }

    @Test
    fun matchesJsonArrayWithStrict() {
        assertThat(p).matches("json_array(foo strict)")
    }

    @Test
    fun matchesLongJsonArray() {
        assertThat(p).matches("json_array(json_array(1,2,3), 100, 'test', null null on null returning json strict)")
    }

    @Test
    fun matchesJsonArrayFromQuery() {
        assertThat(p).matches("json_array(select * from tab null on null returning json strict)")
    }

    @Test
    fun matchesAlternativeSyntaxOfJsonArray() {
        assertThat(p).matches("json[1]  ")
        assertThat(p).matches("json[json[1,2,3], 100, 'test', null null on null returning json strict]")
        assertThat(p).matches("json[select * from tab]")
    }

}
