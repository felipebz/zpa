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

class WhileStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.WHILE_STATEMENT)
    }

    @Test
    fun matchesWhileLoop() {
        assertThat(p).matches(""
                + "while true loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesNestedWhileLoop() {
        assertThat(p).matches(""
                + "while true loop "
                + "while true loop "
                + "null; "
                + "end loop; "
                + "end loop;")
    }

    @Test
    fun matchesLabeledLoop() {
        assertThat(p).matches(""
                + "<<foo>> while true loop "
                + "null; "
                + "end loop foo;")
    }

}
