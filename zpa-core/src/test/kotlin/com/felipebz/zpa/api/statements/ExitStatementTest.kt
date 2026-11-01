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

class ExitStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXIT_STATEMENT)
    }

    @Test
    fun matchesExit() {
        assertThat(p).matches("exit;")
    }

    @Test
    fun matchesExitFromLabel() {
        assertThat(p).matches("exit foo;")
    }

    @Test
    fun matchesExitWhen() {
        assertThat(p).matches("exit when true;")
    }

    @Test
    fun matchesLabeledExit() {
        assertThat(p).matches("<<foo>> exit;")
    }

    @Test
    fun matchesLongExit() {
        assertThat(p).matches("exit foo when true;")
    }

}
