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

class ProcedureDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.PROCEDURE_DECLARATION)
    }

    @Test
    fun matchesSimpleProcedure() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleProcedureAlternative() {
        assertThat(p).matches(""
                + "procedure test as\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithParameter() {
        assertThat(p).matches(""
                + "procedure test(parameter in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithMultipleParameters() {
        assertThat(p).matches(""
                + "procedure test(parameter1 in number, parameter2 in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithVariableDeclaration() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "var number;"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

}
