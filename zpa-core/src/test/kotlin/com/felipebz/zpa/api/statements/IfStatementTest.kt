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

class IfStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.IF_STATEMENT)
    }

    @Test
    fun matchesSimpleIf() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElsif() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithMultipleElsif() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElse() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "else "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesIfWithElsifAndElse() {
        assertThat(p).matches(""
                + "if true then "
                + "null; "
                + "elsif true then "
                + "null; "
                + "else "
                + "null; "
                + "end if;")
    }

    @Test
    fun matchesNestedIf() {
        assertThat(p).matches(""
                + "if true then "
                + "if true then "
                + "null; "
                + "end if;"
                + "end if;")
    }

    @Test
    fun matchesLabeledIf() {
        assertThat(p).matches(""
                + "<<foo>> if true then "
                + "null; "
                + "end if foo;")
    }

}
