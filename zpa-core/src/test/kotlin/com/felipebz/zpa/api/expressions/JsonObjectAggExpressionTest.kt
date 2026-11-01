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

class JsonObjectAggExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleJsonObjectAgg() {
        assertThat(p).matches("json_objectagg(k value 'v')")
    }

    @Test
    fun matchesJsonObjectAggWithIsSeparator() {
        assertThat(p).matches("json_objectagg(dummy is dummy)")
    }

    @Test
    fun matchesJsonObjectAggWithExplicitKeyAndIsSeparator() {
        assertThat(p).matches("json_objectagg(key dummy is dummy)")
    }

    @Test
    fun matchesJsonObjectAggWithExplicitKey() {
        assertThat(p).matches("json_objectagg(key k value 'v')")
    }

    @Test
    fun matchesJsonObjectAggWithFormatJson() {
        assertThat(p).matches("json_objectagg('k' value '{}' format json)")
    }

    @Test
    fun matchesJsonObjectAggWithReturning() {
        assertThat(p).matches("json_objectagg(k value 'v' returning clob)")
    }

    @Test
    fun matchesJsonObjectAggWithAbsentOnNull() {
        assertThat(p).matches("json_objectagg('v' value val absent on null)")
    }

    @Test
    fun matchesLongJsonObjectAgg() {
        assertThat(p).matches("""json_objectagg('a' value '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys)""")
    }


    @Test
    fun matchesJsonObjectAggWithPrettyAndAscii() {
        assertThat(p).matches("json_objectagg(key 'k' value foo returning clob pretty)")
        assertThat(p).matches("json_objectagg(key 'k' value foo returning clob pretty ascii)")
    }

    @Test
    fun doesNotMatchJsonObjectAggWithPrettyBeforeReturning() {
        assertThat(p).notMatches("json_objectagg(key 'k' value foo pretty returning clob)")
    }
}
