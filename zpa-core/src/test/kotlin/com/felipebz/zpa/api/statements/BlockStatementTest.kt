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

    @Test
    fun matchesAutonomousTransactionPragmaAmongStatements() {
        assertThat(p).matches("begin null; pragma autonomous_transaction; null; end;")
    }
}
