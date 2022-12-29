/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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

class FunctionDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FUNCTION_DECLARATION)
    }

    @Test
    fun matchesSimpleFunction() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleFunctionAlternative() {
        assertThat(p).matches(""
                + "function test return number as\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithParameter() {
        assertThat(p).matches(""
                + "function test(parameter in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithMultipleParameters() {
        assertThat(p).matches(""
                + "function test(parameter1 in number, parameter2 in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithVariableDeclaration() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "var number;"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesDeterministicFunction() {
        assertThat(p).matches(""
                + "function test return number deterministic is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesPipelinedFunction() {
        assertThat(p).matches(""
                + "function test return number pipelined is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesParallelEnableFunction() {
        assertThat(p).matches(""
            + "function test return number parallel_enable is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;")
    }

}
