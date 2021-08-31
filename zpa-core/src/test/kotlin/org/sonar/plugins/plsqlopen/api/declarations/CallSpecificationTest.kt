/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class CallSpecificationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CALL_SPECIFICATION)
    }

    @Test
    fun matchesJavaCallSpec() {
        assertThat(p).matches("language java name 'foo';")
    }

    @Test
    fun matchesLanguageCNameLibrary() {
        assertThat(p).matches("language c name \"foo\" library bar;")
    }

    @Test
    fun matchesLanguageCLibraryName() {
        assertThat(p).matches("language c library bar name \"foo\";")
    }

    @Test
    fun matchesExternal() {
        assertThat(p).matches("external name \"foo\" library bar;")
    }

    @Test
    fun matchesAgentIn() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent);")
    }

    @Test
    fun matchesAgentInMultiple() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent, agent2);")
    }

    @Test
    fun matchesWithContext() {
        assertThat(p).matches("language c library bar name \"foo\" with context;")
    }

    @Test
    fun matchesParameterContext() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (context);")
    }

    @Test
    fun matchesParameterSelf() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (self);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self tdo);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator struct);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator tdo);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self length);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self duration);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self maxlen);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetid);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetform);")
    }

    @Test
    fun matchesParameterSimple() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int);")
    }

    @Test
    fun matchesMultipleParameters() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int, y int);")
    }

    @Test
    fun matchesParameterReturn() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (return int);")
    }

    @Test
    fun matchesParameterWithProperty() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x length int);")
    }

    @Test
    fun matchesParameterByReference() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x by reference int);")
    }

}
