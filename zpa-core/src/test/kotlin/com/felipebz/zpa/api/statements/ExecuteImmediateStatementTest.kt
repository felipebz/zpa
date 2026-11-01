//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExecuteImmediateStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT)
    }

    @Test
    fun matchesSimpleExecuteImmediate() {
        assertThat(p).matches("execute immediate 'command';")
    }

    @Test
    fun matchesExecuteImmediateIntoVariable() {
        assertThat(p).matches("execute immediate 'command' into var;")
    }

    @Test
    fun matchesExecuteImmediateIntoRecord() {
        assertThat(p).matches("execute immediate 'command' into rec.field;")
    }

    @Test
    fun matchesExecuteImmediateIntoField() {
        assertThat(p).matches("execute immediate 'command' into tab(i).field;")
    }

    @Test
    fun matchesExecuteImmediateBulkCollectIntoVariable() {
        assertThat(p).matches("execute immediate 'command' bulk collect into var;")
    }

    @Test
    fun matchesExecuteImmediateIntoMultipleVariables() {
        assertThat(p).matches("execute immediate 'command' into var, var2;")
    }

    @Test
    fun matchesExecuteImmediateBulkCollectIntoMultipleVariables() {
        assertThat(p).matches("execute immediate 'command' bulk collect into var, var2;")
    }

    @Test
    fun matchesExecuteImmediateUsingWithVariable() {
        assertThat(p).matches("execute immediate 'command' using var;")
    }

    @Test
    fun matchesExecuteImmediateUsingWithMultipleVariables() {
        assertThat(p).matches("execute immediate 'command' using var, var2;")
    }

    @Test
    fun matchesExecuteImmediateUsingWithExplicitInVariable() {
        assertThat(p).matches("execute immediate 'command' using in var;")
    }

    @Test
    fun matchesExecuteImmediateUsingWithOutVariable() {
        assertThat(p).matches("execute immediate 'command' using out var;")
    }

    @Test
    fun matchesExecuteImmediateUsingWithInOutVariable() {
        assertThat(p).matches("execute immediate 'command' using in out var;")
    }
    @Test
    fun matchesExecuteImmediateUsingWithInOutVariableNamedOut() {
        assertThat(p).matches("execute immediate 'command' using in out out;")
    }

    @Test
    fun rejectsExecuteImmediateUsingOutWithoutVariable() {
        assertThat(p).notMatches("execute immediate 'command' using out;")
    }

    @Test
    fun matchesLabeledExecuteImmediate() {
        assertThat(p).matches("<<label>> execute immediate 'command';")
    }

    @Test
    fun matchesExecuteImmediateWithReturning() {
        assertThat(p).matches("execute immediate 'command' returning into foo;")
    }

}
