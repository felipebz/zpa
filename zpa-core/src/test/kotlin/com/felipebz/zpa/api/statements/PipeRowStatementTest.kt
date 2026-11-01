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

class PipeRowStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.PIPE_ROW_STATEMENT)
    }

    @Test
    fun matchesSimpleSearchedCase() {
        assertThat(p).matches("pipe row (value);")
    }

    @Test
    fun matchesLabeledSearchedCase() {
        assertThat(p).matches("<<foo>> pipe row (value);")
    }

}
