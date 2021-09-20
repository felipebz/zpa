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
package org.sonar.plugins.plsqlopen.api.statements

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class BlockStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT)
    }

    @Test
    fun matchesSimpleBlock() {
        assertThat(p).matches("begin null; end;")
        assertThat(p).matches("BEGIN NULL; END;")
    }

    @Test
    fun matchesNestedBlock() {
        assertThat(p).matches("begin begin null; end; end;")
    }

    @Test
    fun matchesBlockWithMultipleStatements() {
        assertThat(p).matches("begin null; null; end;")
    }

    @Test
    fun matchesBlockWithOneExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; end;")
    }

    @Test
    fun matchesBlockWithMultipleExceptionHandler() {
        assertThat(p).matches("begin null; exception when others then null; when others then null; end;")
    }

    @Test
    fun matchesBlockWithOneExceptionHandlerAndMultipleExceptions() {
        assertThat(p).matches("begin null; exception when no_data_found or too_many_rows then null; end;")
    }

    @Test
    fun matchesBlockWithNameAtEnd() {
        assertThat(p).matches("begin null; end block_name;")
    }

    @Test
    fun matchesBlockWithDeclareSection() {
        assertThat(p).matches("declare var number; begin null; end block_name;")
    }

    @Test
    fun matchesBlockWithDeclareSectionWithoutDeclarations() {
        assertThat(p).matches("declare begin null; end block_name;")
    }

    @Test
    fun notMatchesBlockWithoutStatements() {
        assertThat(p).notMatches("begin end;")
    }

    @Test
    fun notMatchesBlockWithIncompleteExceptionHandler() {
        assertThat(p).notMatches("begin null; exception end;")
    }

    @Test
    fun matchesLabeledBlock() {
        assertThat(p).matches("<<foo>> begin null; end foo;")
    }
}
