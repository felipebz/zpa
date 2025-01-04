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

class JsonObjectExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleJsonObject() {
        assertThat(p).matches("json_object(foo)")
    }

    @Test
    fun matchesJsonObjectWithKey() {
        assertThat(p).matches("json_object(k value 'v')")
    }

    @Test
    fun matchesJsonObjectWithExplicitKey() {
        assertThat(p).matches("json_object(key k value 'v')")
    }

    @Test
    fun matchesJsonObjectWithStringKeys() {
        assertThat(p).matches("json_object('k': 'v')")
    }

    @Test
    fun matchesJsonObjectWithFormatJson() {
        assertThat(p).matches("json_object('k': '{}' format json)")
    }

    @Test
    fun matchesJsonObjectWithReturning() {
        assertThat(p).matches("json_object(foo returning clob)")
    }

    @Test
    fun matchesJsonObjectWithAbsentOnNull() {
        assertThat(p).matches("json_object(* absent on null)")
    }

    @Test
    fun matchesLongJsonObject() {
        assertThat(p).matches("""json_object('k' value 'v',
            'a': '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys)""")
    }

    @Test
    fun matchesAlternativeSyntaxOfJsonObject() {
        assertThat(p).matches("json { foo }")
        assertThat(p).matches("json { 'k': 'v' }")
        assertThat(p).matches(
            """json { 'k' value 'v',
            'a': '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys }"""
        )
    }

}
