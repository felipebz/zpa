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
package org.sonar.plugins.plsqlopen.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HostAndIndicatorVariableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE)
    }

    @Test
    fun matchesSimpleBindingVariable() {
        assertThat(p).matches(":var")
    }

    @Test
    fun matchesSimpleNumericBindingVariable() {
        assertThat(p).matches(":1")
    }

    @Test
    fun matchesShortIndicatorVariable() {
        assertThat(p).matches(":var:ind")
    }

    @Test
    fun matchesLongIndicatorVariable() {
        assertThat(p).matches(":var indicator :ind")
    }

    @Test
    fun matchesQuestionMarkBinding() {
        assertThat(p).matches("?")
    }

}
