//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IsJsonConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleIsJson() {
        assertThat(p).matches("foo is json")
    }

    @Test
    fun matchesSimpleIsNotJson() {
        assertThat(p).matches("foo is not json")
    }

    @Test
    fun matchesIsJsonFormatJson() {
        assertThat(p).matches("foo is json format json")
    }

    @Test
    fun matchesIsJsonStrictLax() {
        assertThat(p).matches("foo is json strict")
        assertThat(p).matches("foo is json lax")
    }

    @Test
    fun matchesIsJsonStrictAllowDisallowScalars() {
        assertThat(p).matches("foo is json allow scalars")
        assertThat(p).matches("foo is json disallow scalars")
    }

    @Test
    fun matchesIsJsonStrictWithWithoutUniqueKeys() {
        assertThat(p).matches("foo is json with unique keys")
        assertThat(p).matches("foo is json without unique keys")
    }

    @Test
    fun matchesIsJsonValidate() {
        assertThat(p).matches("foo is json validate '{}'")
    }

    @Test
    fun matchesIsJsonValidateCast() {
        assertThat(p).matches("foo is json validate cast '{}'")
    }

    @Test
    fun matchesIsJsonValidateUsing() {
        assertThat(p).matches("foo is json validate cast '{}'")
    }

    @Test
    fun matchesIsJsonWithColumnModifier() {
        assertThat(p).matches("foo is json object")
    }

    @Test
    fun matchesIsJsonWithColumnModifierInParenthesis() {
        assertThat(p).matches("foo is json (object)")
    }

    @Test
    fun matchesIsJsonWithMultipleColumnModifier() {
        assertThat(p).matches("foo is json (value, array, object, scalar number, scalar timestamp with time zone)")
    }

    @Test
    fun matchesLongIsJson() {
        assertThat(p).matches("""
            foo is json (value, array, object, scalar number, scalar timestamp with time zone)
            format json strict allow scalars with unique keys
            """)
    }

}
