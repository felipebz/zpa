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
package org.sonar.plugins.plsqlopen.api.units

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

class CreateProcedureTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
    }

    @Test
    fun matchesSimpleProcedure() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleProcedureAlternative() {
        assertThat(p).matches(""
                + "create procedure test as\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesEditionableProcedure() {
        assertThat(p).matches(""
                + "create editionable procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesNonEditionableProcedure() {
        assertThat(p).matches(""
                + "create noneditionable procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithSharingMetadata() {
        assertThat(p).matches(""
                + "create procedure test sharing = metadata is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithSharingNone() {
        assertThat(p).matches(""
                + "create procedure test sharing = none is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithDefaultCollation() {
        assertThat(p).matches(""
                + "create procedure test default collation using_nls_comp is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithAccesibleBy() {
        assertThat(p).matches(""
                + "create procedure test accessible by (function func) is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesSimpleCreateOrReplaceProcedure() {
        assertThat(p).matches(""
                + "create or replace procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithSchema() {
        assertThat(p).matches(""
                + "create procedure schema.test is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithParameter() {
        assertThat(p).matches(""
                + "create procedure test(parameter in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithMultipleParameters() {
        assertThat(p).matches(""
                + "create procedure test(parameter1 in number, parameter2 in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create procedure test authid current_user is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create procedure test authid definer is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithJavaCallSpec() {
        assertThat(p).matches("create procedure test is language java name 'javatest';")
    }

    @Test
    fun matchesExternalProcedure() {
        assertThat(p).matches("create procedure test is external name \"foo\" library bar;")
    }

    @Test
    fun matchesProcedureWithVariableDeclaration() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "var number;"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

    @Test
    fun matchesProcedureWithTimestamp() {
        assertThat(p).matches(""
                + "create procedure test timestamp '2015-01-01' is\n"
                + "begin\n"
                + "null;\n"
                + "end;")
    }

}
