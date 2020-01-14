/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.session

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.plugins.plsqlopen.api.SessionControlGrammar
import org.sonar.sslr.tests.Assertions.assertThat

class SessionControlGrammarTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(SessionControlGrammar.SESSION_CONTROL_COMMAND)
    }

    @Test
    fun matchesAlterSession() {
        assertThat(p).matches("alter session set nls_date_format = 'dd/mm/yyyy';")
    }

    @Test
    fun matchesSetRole() {
        assertThat(p).matches("set role foo;")
        assertThat(p).matches("set role foo, bar, baz;")
    }

    @Test
    fun matchesSetRoleNone() {
        assertThat(p).matches("set role none;")
    }

    @Test
    fun matchesSetRoleAll() {
        assertThat(p).matches("set role all;")
    }

    @Test
    fun matchesSetRoleAllExcept() {
        assertThat(p).matches("set role all except foo;")
        assertThat(p).matches("set role all except foo, bar, baz;")
    }

    @Test
    fun matchesSetRoleWithPassword() {
        assertThat(p).matches("set role foo identified by pass;")
        assertThat(p).matches("set role foo identified by pass, bar, baz;")
    }

}
