/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

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
