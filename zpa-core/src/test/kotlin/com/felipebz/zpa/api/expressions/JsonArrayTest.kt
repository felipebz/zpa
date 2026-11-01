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

class JsonArrayTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonArrayWithOneElement() {
        assertThat(p).matches("json_array(1)")
    }

    @Test
    fun matchesJsonArrayWithManyElement() {
        assertThat(p).matches("json_array(1, 'test', sysdate)")
    }

    @Test
    fun matchesJsonArrayWithFormat() {
        assertThat(p).matches("json_array('{}' format json)")
    }

    @Test
    fun matchesJsonArrayWithNullOnNull() {
        assertThat(p).matches("json_array(foo null on null)")
    }

    @Test
    fun matchesJsonArrayWithAbsentOnNull() {
        assertThat(p).matches("json_array(foo absent on null)")
    }

    @Test
    fun matchesJsonArrayWithReturning() {
        assertThat(p).matches("json_array(foo returning json)")
    }

    @Test
    fun matchesJsonArrayWithStrict() {
        assertThat(p).matches("json_array(foo strict)")
    }

    @Test
    fun matchesLongJsonArray() {
        assertThat(p).matches("json_array(json_array(1,2,3), 100, 'test', null null on null returning json strict)")
    }

    @Test
    fun matchesJsonArrayFromQuery() {
        assertThat(p).matches("json_array(select * from tab null on null returning json strict)")
    }

    @Test
    fun matchesAlternativeSyntaxOfJsonArray() {
        assertThat(p).matches("json[1]  ")
        assertThat(p).matches("json[json[1,2,3], 100, 'test', null null on null returning json strict]")
        assertThat(p).matches("json[select * from tab]")
    }


    @Test
    fun matchesJsonArrayWithoutElements() {
        assertThat(p).matches("json_array(returning clob)")
    }

    @Test
    fun matchesJsonArrayWithPrettyAndAscii() {
        assertThat(p).matches("json_array(1, 2 returning clob pretty)")
        assertThat(p).matches("json_array(1 returning clob pretty ascii)")
    }

    @Test
    fun doesNotMatchJsonArrayWithPrettyBeforeReturning() {
        assertThat(p).notMatches("json_array(1 pretty returning clob)")
    }
}
