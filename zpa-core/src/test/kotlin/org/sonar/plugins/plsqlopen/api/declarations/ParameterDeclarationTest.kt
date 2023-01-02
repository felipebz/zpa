/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class ParameterDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.PARAMETER_DECLARATION)
    }

    @Test
    fun matchesSimpleParameter() {
        assertThat(p).matches("parameter number")
    }

    @Test
    fun matchesExplicitInParameter() {
        assertThat(p).matches("parameter in number")
    }

    @Test
    fun matchesExplicitInParameterWithDefaultValue() {
        assertThat(p).matches("parameter in number := 1")
    }

    @Test
    fun matchesExplicitInParameterWithDefaultValueAlternative() {
        assertThat(p).matches("parameter in number default 1")
    }

    @Test
    fun matchesOutParameter() {
        assertThat(p).matches("parameter out number")
    }

    @Test
    fun matchesInOutParameter() {
        assertThat(p).matches("parameter in out number")
    }

    @Test
    fun matchesOutParameterWithNocopy() {
        assertThat(p).matches("parameter out nocopy number")
    }

    @Test
    fun matchesInOutParameterWithNocopy() {
        assertThat(p).matches("parameter in out nocopy number")
    }

}
