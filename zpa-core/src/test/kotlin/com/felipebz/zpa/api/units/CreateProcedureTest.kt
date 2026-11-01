//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
