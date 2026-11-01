//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefCursorDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.REF_CURSOR_DECLARATION)
    }

    @Test
    fun matchesSimpleRefCursor() {
        assertThat(p).matches("type myref is ref cursor;")
    }

    @Test
    fun matchesSimpleRefCursorWithReturn() {
        assertThat(p).matches("type myref is ref cursor return number;")
    }

}
