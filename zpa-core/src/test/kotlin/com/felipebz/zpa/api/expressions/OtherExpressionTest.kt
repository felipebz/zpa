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

class OtherExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesCollectionExists() {
        assertThat(p).matches("collection.exists(0)")
    }

    @Test
    fun matchesCursorMethods() {
        assertThat(p).matches("cur%found")
        assertThat(p).matches("cur%notfound")
        assertThat(p).matches("cur%isopen")
    }

    @Test
    fun matchesHostCursorMethods() {
        assertThat(p).matches(":cur%found")
        assertThat(p).matches(":cur%notfound")
        assertThat(p).matches(":cur%isopen")
    }

    @Test
    fun matchesSqlMethods() {
        assertThat(p).matches("sql%found")
        assertThat(p).matches("sql%notfound")
        assertThat(p).matches("sql%isopen")
    }

    @Test
    fun matchesIsNull() {
        assertThat(p).matches("var is null")
    }

    @Test
    fun matchesIsNotNull() {
        assertThat(p).matches("var is not null")
    }

    @Test
    fun matchesIsTrue() {
        assertThat(p).matches("var is true")
    }

    @Test
    fun matchesIsNotTrue() {
        assertThat(p).matches("var is not true")
    }

    @Test
    fun matchesIsFalse() {
        assertThat(p).matches("var is false")
    }

    @Test
    fun matchesIsNotFalse() {
        assertThat(p).matches("var is not false")
    }

    @Test
    fun matchesBasicIn() {
        assertThat(p).matches("var in (1)")
    }

    @Test
    fun matchesBasicInWithMultipleValues() {
        assertThat(p).matches("var in (1, 2, 3)")
    }

    @Test
    fun matchesExpressionWithInWithMultipleValues() {
        setRootRule(PlSqlGrammar.IN_EXPRESSION)
        assertThat(p).matches("v1||v2 in ('ab', 'bc', 'ac')")
    }

    @Test
    fun matchesBasicInWithoutParenthesis() {
        assertThat(p).matches("var in 1")
    }

    @Test
    fun matchesMultidimensionalCollection() {
        assertThat(p).matches("foo(1)(1)")
    }

    @Test
    fun InExpressionShouldNotMatchAdditionalExpressions() {
        setRootRule(PlSqlGrammar.IN_EXPRESSION)
        assertThat(p).notMatches("var in (select 1 from dual) and x = y")
    }

}
