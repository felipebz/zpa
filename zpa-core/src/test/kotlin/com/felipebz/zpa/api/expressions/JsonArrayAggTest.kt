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

class JsonArrayAggTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonArrayAgg() {
        assertThat(p).matches("json_arrayagg(1)")
    }

    @Test
    fun matchesJsonArrayAggWithFormat() {
        assertThat(p).matches("json_arrayagg('{}' format json)")
    }

    @Test
    fun matchesJsonArrayAggWithOrderBy() {
        assertThat(p).matches("json_arrayagg(foo order by col)")
    }

    @Test
    fun matchesJsonArrayAggWithNullOnNull() {
        assertThat(p).matches("json_arrayagg(foo null on null)")
    }

    @Test
    fun matchesJsonArrayAggWithAbsentOnNull() {
        assertThat(p).matches("json_arrayagg(foo absent on null)")
    }

    @Test
    fun matchesJsonArrayAggWithReturning() {
        assertThat(p).matches("json_arrayagg(foo returning json)")
    }

    @Test
    fun matchesJsonArrayAggWithStrict() {
        assertThat(p).matches("json_arrayagg(foo strict)")
    }

    @Test
    fun matchesLongJsonArrayAgg() {
        assertThat(p).matches("json_arrayagg(foo order by col null on null returning json strict)")
    }


    @Test
    fun matchesJsonArrayAggWithPrettyAndAscii() {
        assertThat(p).matches("json_arrayagg(foo returning clob pretty)")
        assertThat(p).matches("json_arrayagg(foo order by bar returning clob pretty ascii)")
    }

    @Test
    fun doesNotMatchJsonArrayAggWithPrettyBeforeReturning() {
        assertThat(p).notMatches("json_arrayagg(foo pretty returning clob)")
    }
}
