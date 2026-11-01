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

class OpenForStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.OPEN_FOR_STATEMENT)
    }

    @Test
    fun matchesSimpleOpen() {
        assertThat(p).matches("open cur for myquery;")
    }

    @Test
    fun matchesOpenCursorInPackage() {
        assertThat(p).matches("open pack.cur for myquery;")
    }

    @Test
    fun matchesOpenForSelectExpression() {
        assertThat(p).matches("open cur for select 1 from dual;")
    }

    @Test
    fun matchesOpenForHostCursor() {
        assertThat(p).matches("open :cur for myquery;")
    }

    @Test
    fun matchesOpenWithUsingClause() {
        assertThat(p).matches("open cur for myquery using foo;")
    }

    @Test
    fun matchesOpenWithInParameterInUsingClause() {
        assertThat(p).matches("open cur for myquery using in foo;")
    }

    @Test
    fun matchesOpenWithInParameterInOutUsingClause() {
        assertThat(p).matches("open cur for myquery using in out foo;")
    }

    @Test
    fun matchesOpenWithInParameterOutUsingClause() {
        assertThat(p).matches("open cur for myquery using out foo;")
    }

    @Test
    fun matchesOpenWithMultipleParameters() {
        assertThat(p).matches("open cur for myquery using foo, bar, baz;")
    }

    @Test
    fun matcheslabeledOpen() {
        assertThat(p).matches("<<foo>> open cur for myquery;")
    }

}
