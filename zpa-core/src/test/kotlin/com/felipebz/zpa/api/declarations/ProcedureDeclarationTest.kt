//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun matchesProcedureNamedOut() {
        assertThat(p).matches("procedure out(x number);")
    }

    @Test
    fun matchesProcedureWithOutNamedParameter() {
        assertThat(p).matches("procedure x(out number);")
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


    @Test
    fun matchesProcedureWithDeterministicAndParallelEnable() {
        assertThat(p).matches("procedure foo(a number) deterministic is begin null; end;")
        assertThat(p).matches("procedure foo(p_cursor sys_refcursor) parallel_enable is begin null; end;")
    }

    @Test
    fun doesNotMatchProcedureWithDeterministicAfterIs() {
        assertThat(p).notMatches("procedure foo(a number) is deterministic begin null; end;")
    }
}
