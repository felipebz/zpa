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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CharacterExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleConcatenation() {
        assertThat(p).matches("'a'||'b'")
    }

    @Test
    fun matchesMultipleConcatenation() {
        assertThat(p).matches("'a'||'b'||'c'")
    }

    @Test
    fun matchesVariableConcatenation() {
        assertThat(p).matches("var||var")
    }

    @Test
    fun matchesFunctionCallConcatenation() {
        assertThat(p).matches("func(var)||func(var)")
    }

    @Test
    fun matchesHostVariableConcatenation() {
        assertThat(p).matches(":var||:var")
    }

    @Test
    fun matchesIndicatorVariableConcatenation() {
        assertThat(p).matches(":var:indicator||:var:indicator")
    }

    @Test
    fun matchesReplace() {
        assertThat(p).matches("replace(var, 'x', 'y')")
    }

}
