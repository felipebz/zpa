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

class ToTimestampTzExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimple() {
        assertThat(p).matches("to_timestamp_tz(foo)")
    }

    @Test
    fun matchesDefaultClause() {
        assertThat(p).matches("to_timestamp_tz(foo default systimestamp on conversion error)")
    }

    @Test
    fun matchesMultipleArguments() {
        assertThat(p).matches("to_timestamp_tz(foo, 'dd/mm/rrrr', 'NLS_DATE_LANGUAGE = American')")
    }

}
