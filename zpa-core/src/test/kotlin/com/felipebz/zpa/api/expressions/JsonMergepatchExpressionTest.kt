//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonMergepatchExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleJsonMergepatch() {
        assertThat(p).matches("json_mergepatch(target, patch)")
    }

    @Test
    fun matchesJsonMergepatchWithReturning() {
        assertThat(p).matches("json_mergepatch(target, patch returning clob)")
    }

    @Test
    fun matchesJsonMergepatchWithNullOnError() {
        assertThat(p).matches("json_mergepatch(target, patch null on error)")
    }

    @Test
    fun matchesLongJsonMergepatch() {
        assertThat(p).matches("""json_mergepatch(target, patch
            returning varchar2(200)
            pretty ascii truncate
            error on error)""")
    }

}
