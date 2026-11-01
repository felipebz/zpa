//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CallCommandTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CALL_COMMAND)
    }

    @Test
    fun matchesQualifiedCallsWithParentheses() {
        assertThat(p).matches("call pkg.proc();")
        assertThat(p).matches("call pkg.proc(1, 'a');")
        assertThat(p).matches("call schema.pkg.proc(1);")
        assertThat(p).matches(
            "call ret_warehouse_typ(warehouse_typ(234, 'Warehouse 234', 2235)).ret_name() " +
                "into :result;"
        )
        assertThat(p).matches("call pkg.func(1) into :result;")
    }

    @Test
    fun rejectsCallsWithoutParenthesesOrBindVariables() {
        assertThat(p).notMatches("call pkg.proc;")
        assertThat(p).notMatches("call pkg.func(1) into result;")
        assertThat(p).notMatches("call pkg.func into :result;")
        assertThat(p).notMatches("call;")
    }
}
