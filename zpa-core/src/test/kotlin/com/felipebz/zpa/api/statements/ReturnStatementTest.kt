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

class ReturnStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.RETURN_STATEMENT)
    }

    @Test
    fun matchesSimpleReturn() {
        assertThat(p).matches("return;")
    }

    @Test
    fun matchesReturnWithValue() {
        assertThat(p).matches("return 1;")
    }

    @Test
    fun matchesLabeledReturn() {
        assertThat(p).matches("<<foo>> return;")
    }

}
