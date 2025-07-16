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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class OpenForStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.OPEN_FOR_STATEMENT)
    }

    @Test
    fun matchesSimpleOpen() {
        assertThat(p).matches("open cur for myquery;")
    }

    @Test
    fun matchesOpenCursorInPackage() {
        assertThat(p).matches("open pack.cur for myquery;")
    }

    @Test
    fun matchesOpenForSelectExpression() {
        assertThat(p).matches("open cur for select 1 from dual;")
    }

    @Test
    fun matchesOpenForHostCursor() {
        assertThat(p).matches("open :cur for myquery;")
    }

    @Test
    fun matchesOpenWithUsingClause() {
        assertThat(p).matches("open cur for myquery using foo;")
    }

    @Test
    fun matchesOpenWithInParameterInUsingClause() {
        assertThat(p).matches("open cur for myquery using in foo;")
    }

    @Test
    fun matchesOpenWithInParameterInOutUsingClause() {
        assertThat(p).matches("open cur for myquery using in out foo;")
    }

    @Test
    fun matchesOpenWithInParameterOutUsingClause() {
        assertThat(p).matches("open cur for myquery using out foo;")
    }

    @Test
    fun matchesOpenWithMultipleParameters() {
        assertThat(p).matches("open cur for myquery using foo, bar, baz;")
    }

    @Test
    fun matcheslabeledOpen() {
        assertThat(p).matches("<<foo>> open cur for myquery;")
    }

}
