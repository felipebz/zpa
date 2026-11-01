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

class LoopStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.LOOP_STATEMENT)
    }

    @Test
    fun matchesSimpleLoop() {
        assertThat(p).matches(""
                + "loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesNestedLoop() {
        assertThat(p).matches(""
                + "loop "
                + "loop "
                + "null; "
                + "end loop; "
                + "end loop;")
    }

    @Test
    fun matchesLabeledLoop() {
        assertThat(p).matches(""
                + "<<foo>> loop "
                + "null; "
                + "end loop foo;")
    }

}
