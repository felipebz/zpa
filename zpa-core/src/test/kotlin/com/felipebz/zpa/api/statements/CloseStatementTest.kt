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

class CloseStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CLOSE_STATEMENT)
    }

    @Test
    fun matchesSimpleClose() {
        assertThat(p).matches("close foo;")
    }

    @Test
    fun matchesCloseCursorInPackage() {
        assertThat(p).matches("close pack.foo;")
    }

    @Test
    fun matchesSimpleCloseHostCursor() {
        assertThat(p).matches("close :foo;")
    }

    @Test
    fun matchesLabeledClose() {
        assertThat(p).matches("<<foo>> close foo;")
    }

}
