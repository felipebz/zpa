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

class ContinueStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CONTINUE_STATEMENT)
    }

    @Test
    fun matchesContinue() {
        assertThat(p).matches("continue;")
    }

    @Test
    fun matchesContinueFromLabel() {
        assertThat(p).matches("continue foo;")
    }

    @Test
    fun matchesContinueWhen() {
        assertThat(p).matches("continue when true;")
    }

    @Test
    fun matchesLabeledContinue() {
        assertThat(p).matches("<<foo>> continue;")
    }

    @Test
    fun matchesLongContinue() {
        assertThat(p).matches("continue foo when true;")
    }

}
