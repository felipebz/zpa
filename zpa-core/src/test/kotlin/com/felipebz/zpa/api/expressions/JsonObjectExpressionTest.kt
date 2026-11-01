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

class JsonObjectExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonObjectWithKeyIs() {
        assertThat(p).matches("json_object(key 'foo' is bar)")
        assertThat(p).matches("json_object('foo' is bar, 'baz' is qux)")
    }

    @Test
    fun matchesJsonObjectWithExpressionAndColon() {
        assertThat(p).matches("json_object(lower(foo) : bar)")
    }

    @Test
    fun matchesJsonObjectWithReturningAndPretty() {
        assertThat(p).matches("json_object('foo' value bar returning clob pretty)")
    }

    @Test
    fun matchesSimpleJsonObject() {
        assertThat(p).matches("json_object(foo)")
    }

    @Test
    fun matchesJsonObjectWithKey() {
        assertThat(p).matches("json_object(k value 'v')")
    }

    @Test
    fun matchesJsonObjectWithExplicitKey() {
        assertThat(p).matches("json_object(key k value 'v')")
    }

    @Test
    fun matchesJsonObjectWithStringKeys() {
        assertThat(p).matches("json_object('k': 'v')")
    }

    @Test
    fun matchesJsonObjectWithIsSeparator() {
        assertThat(p).matches("json_object('foo' is dummy)")
    }

    @Test
    fun matchesJsonObjectWithExplicitKeyAndIsSeparator() {
        assertThat(p).matches("json_object(key 'foo' is dummy)")
    }

    @Test
    fun matchesJsonObjectWithMixedSeparators() {
        assertThat(p).matches(
            """
            json_object(
                'a' value dummy,
                'b' is dummy,
                'c' : dummy
            )
            """.trimIndent())
    }

    @Test
    fun matchesJsonObjectWithIdentifierKeyAndColon() {
        assertThat(p).matches("json_object(k: v)")
    }

    @Test
    fun matchesJsonObjectWithExpressionKeyAndColon() {
        assertThat(p).matches("json_object(first_name || '_key': value)")
    }

    @Test
    fun matchesJsonObjectWithFormatJson() {
        assertThat(p).matches("json_object('k': '{}' format json)")
    }

    @Test
    fun matchesJsonObjectWithReturning() {
        assertThat(p).matches("json_object(foo returning clob)")
    }

    @Test
    fun matchesJsonObjectWithAbsentOnNull() {
        assertThat(p).matches("json_object(* absent on null)")
    }

    @Test
    fun matchesLongJsonObject() {
        assertThat(p).matches("""json_object('k' value 'v',
            'a': '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys)""")
    }

    @Test
    fun matchesAlternativeSyntaxOfJsonObject() {
        assertThat(p).matches("json { foo }")
        assertThat(p).matches("json { 'k': 'v' }")
        assertThat(p).matches("json { 'foo' is dummy }")
        assertThat(p).matches(
            """json { 'k' value 'v',
            'a': '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys }"""
        )
    }

}
