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
package org.sonar.plugins.plsqlopen.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.ConditionsGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class IsJsonConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleIsJson() {
        assertThat(p).matches("foo is json")
    }

    @Test
    fun matchesSimpleIsNotJson() {
        assertThat(p).matches("foo is not json")
    }

    @Test
    fun matchesIsJsonFormatJson() {
        assertThat(p).matches("foo is json format json")
    }

    @Test
    fun matchesIsJsonStrictLax() {
        assertThat(p).matches("foo is json strict")
        assertThat(p).matches("foo is json lax")
    }

    @Test
    fun matchesIsJsonStrictAllowDisallowScalars() {
        assertThat(p).matches("foo is json allow scalars")
        assertThat(p).matches("foo is json disallow scalars")
    }

    @Test
    fun matchesIsJsonStrictWithWithoutUniqueKeys() {
        assertThat(p).matches("foo is json with unique keys")
        assertThat(p).matches("foo is json without unique keys")
    }

    @Test
    fun matchesIsJsonValidate() {
        assertThat(p).matches("foo is json validate '{}'")
    }

    @Test
    fun matchesIsJsonValidateCast() {
        assertThat(p).matches("foo is json validate cast '{}'")
    }

    @Test
    fun matchesIsJsonValidateUsing() {
        assertThat(p).matches("foo is json validate cast '{}'")
    }

    @Test
    fun matchesIsJsonWithColumnModifier() {
        assertThat(p).matches("foo is json object")
    }

    @Test
    fun matchesIsJsonWithColumnModifierInParenthesis() {
        assertThat(p).matches("foo is json (object)")
    }

    @Test
    fun matchesIsJsonWithMultipleColumnModifier() {
        assertThat(p).matches("foo is json (value, array, object, scalar number, scalar timestamp with time zone)")
    }

    @Test
    fun matchesLongIsJson() {
        assertThat(p).matches("""
            foo is json (value, array, object, scalar number, scalar timestamp with time zone)
            format json strict allow scalars with unique keys
            """)
    }

}
