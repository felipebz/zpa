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
package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CursorDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CURSOR_DECLARATION)
    }

    @Test
    fun matchesSimpleCursor() {
        assertThat(p).matches("cursor cur is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithParameter() {
        assertThat(p).matches("cursor cur(x number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithMultipleParameters() {
        assertThat(p).matches("cursor cur(x number, y number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithExplicitInParameter() {
        assertThat(p).matches("cursor cur(x in number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithDefaultParameter() {
        assertThat(p).matches("cursor cur(x number default 1) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithDefaultParameterAlternative() {
        assertThat(p).matches("cursor cur(x number := 1) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithReturnType() {
        assertThat(p).matches("cursor cur return my_type is select 1 from dual;")
    }

    @Test
    fun matchesCursorSpecification() {
        assertThat(p).matches("cursor cur return my_type;")
    }

}
