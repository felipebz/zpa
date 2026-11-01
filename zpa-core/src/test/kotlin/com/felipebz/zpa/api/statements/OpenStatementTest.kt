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

class OpenStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.OPEN_STATEMENT)
    }

    @Test
    fun matchesSimpleOpen() {
        assertThat(p).matches("open cur;")
    }

    @Test
    fun matchesOpenCursorInPackage() {
        assertThat(p).matches("open pack.cur;")
    }

    @Test
    fun matchesOpenWithParameter() {
        assertThat(p).matches("open cur(foo);")
    }

    @Test
    fun matchesOpenWithMultipleParameters() {
        assertThat(p).matches("open cur(foo, bar);")
    }

    @Test
    fun matchesOpenWithNamedParameters() {
        assertThat(p).matches("open cur(x => foo, y => bar);")
    }

    @Test
    fun matchesLabeledOpen() {
        assertThat(p).matches("<<foo>> open cur;")
    }

}
