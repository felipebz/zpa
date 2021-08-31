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
package org.sonar.plugins.plsqlopen.api.units

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class CreateFunctionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
    }

    @Test
    fun matchesSimpleFunction() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleFunctionWithNameRefresh() {
        assertThat(p).matches(""
                + "create function refresh return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleFunctionAlternative() {
        assertThat(p).matches(""
                + "create function test return number as\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesEditionableFunction() {
        assertThat(p).matches(""
                + "create editionable function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesNonEditionableFunction() {
        assertThat(p).matches(""
                + "create noneditionable function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithSharingMetadata() {
        assertThat(p).matches(""
                + "create function test return number sharing = metadata is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithSharingNone() {
        assertThat(p).matches(""
                + "create editionable function test return number sharing = none is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleCreateOrReplaceFunction() {
        assertThat(p).matches(""
                + "create or replace function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithSchema() {
        assertThat(p).matches(""
                + "create function schema.test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithParameter() {
        assertThat(p).matches(""
                + "create function test(parameter in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithMultipleParameters() {
        assertThat(p).matches(""
                + "create function test(parameter1 in number, parameter2 in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create function test return number authid current_user is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create function test return number authid definer is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithDefaultCollation() {
        assertThat(p).matches(""
                + "create function test return number default collation using_nls_comp is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithAccessibleBy() {
        assertThat(p).matches(""
                + "create function test return number accessible by (procedure proc) is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithJavaCallSpec() {
        assertThat(p).matches("create function test return number is language java name 'javatest';")
    }

    @Test
    fun matchesFunctionWithVariableDeclaration() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "var number;"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesDeterministicFunction() {
        assertThat(p).matches(""
                + "create function test return number deterministic is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesPipelinedFunction() {
        assertThat(p).matches(""
                + "create function test return number pipelined is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesParallelEnableFunction() {
        assertThat(p).matches(""
                + "create function test return number parallel_enable is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesResultCacheFunction() {
        assertThat(p).matches(""
                + "create function test return number result_cache is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesResultCacheWithReliesOnFunction() {
        assertThat(p).matches(""
                + "create function test return number result_cache relies_on(tbl_test1, tbl_test2) is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesFunctionWithTimestamp() {
        assertThat(p).matches(""
                + "create function test timestamp '2015-01-01' return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;")
    }

    @Test
    fun matchesAggregateFunction() {
        assertThat(p).matches("create function test return varchar2 aggregate using foo.bar;")
    }

}
