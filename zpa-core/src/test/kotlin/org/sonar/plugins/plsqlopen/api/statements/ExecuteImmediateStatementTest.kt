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
package org.sonar.plugins.plsqlopen.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

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
    fun matchesLabeledExecuteImmediate() {
        assertThat(p).matches("<<label>> execute immediate 'command';")
    }

    @Test
    fun matchesExecuteImmediateWithReturning() {
        assertThat(p).matches("execute immediate 'command' returning into foo;")
    }

}
