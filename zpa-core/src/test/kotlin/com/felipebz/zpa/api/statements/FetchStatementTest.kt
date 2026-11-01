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

class FetchStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FETCH_STATEMENT)
    }

    @Test
    fun matchesSimpleFetchInto() {
        assertThat(p).matches("fetch foo into bar;")
    }

    @Test
    fun matchesFetchCursorInPackage() {
        assertThat(p).matches("fetch pack.foo into bar;")
    }

    @Test
    fun matchesFetchHostCursorInto() {
        assertThat(p).matches("fetch :foo into bar;")
    }

    @Test
    fun matchesFetchIntoMultipleVariables() {
        assertThat(p).matches("fetch foo into bar, baz;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectInto() {
        assertThat(p).matches("fetch foo bulk collect into bar;")
    }

    @Test
    fun matchesFetchHostCursorBulkCollectInto() {
        assertThat(p).matches("fetch :foo bulk collect into bar;")
    }

    @Test
    fun matchesFetchBulkCollectIntoMultipleVariables() {
        assertThat(p).matches("fetch foo bulk collect into bar, baz;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectIntoHostVariable() {
        assertThat(p).matches("fetch foo bulk collect into :bar;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectIntoWithLimit() {
        assertThat(p).matches("fetch foo bulk collect into bar limit 10;")
    }

    @Test
    fun matchesLabeledFetchInto() {
        assertThat(p).matches("<<foo>> fetch foo into bar;")
    }

}
