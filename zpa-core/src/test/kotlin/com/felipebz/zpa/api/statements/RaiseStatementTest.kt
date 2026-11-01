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

class RaiseStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.RAISE_STATEMENT)
    }

    @Test
    fun matchesSimpleRaise() {
        assertThat(p).matches("raise;")
    }

    @Test
    fun matchesRaiseException() {
        assertThat(p).matches("raise ex;")
    }

    @Test
    fun matchesRaiseMember() {
        assertThat(p).matches("raise pack.ex;")
    }

    @Test
    fun matchesLabeledRaise() {
        assertThat(p).matches("<<foo>> raise;")
    }

}
