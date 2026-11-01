//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InquiryDirectiveTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.INQUIRY_DIRECTIVE)
    }

    @Test
    fun test() {
        assertThat(p).matches("$\$PLSQL_UNIT")
    }

}
