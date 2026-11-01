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

class NumericExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesNumericAddition() {
        assertThat(p).matches("1 + 1")
        assertThat(p).matches("1+1")
    }

    @Test
    fun matchesNumericSubtraction() {
        assertThat(p).matches("1 - 1")
        assertThat(p).matches("1-1")
    }

    @Test
    fun matchesNumericMultiplication() {
        assertThat(p).matches("1 * 1")
    }

    @Test
    fun matchesNumericDivision() {
        assertThat(p).matches("1 / 1")
    }

    @Test
    fun matchesNumericExponentiation() {
        assertThat(p).matches("1 ** 1")
    }

    @Test
    fun matchesModulo() {
        assertThat(p).matches("1 mod 1")
    }

    @Test
    fun matchesMathematicalOperationBetweenTwoQueries() {
        assertThat(p).matches("(select 1 from dual) + (select 1 from dual)")
    }

    @Test
    fun matchesMathematicalOperationBetweenTwoCaseExpressions() {
        assertThat(p).matches("(case when 1 = 1 then 1 end) + (case when 1 = 1 then 1 end)")
    }

    @Test
    fun matchesCursorRowcount() {
        assertThat(p).matches("cur%rowcount + 1")
    }

    @Test
    fun matchesHostCursorRowcount() {
        assertThat(p).matches(":cur%rowcount + 1")
    }

    @Test
    fun matchesSqlRowcount() {
        assertThat(p).matches("sql%rowcount + 1")
    }

    @Test
    fun matchesSqlBulkRowcount() {
        assertThat(p).matches("sql%bulk_rowcount(1) + 1")
    }

    @Test
    fun matchesHostVariableExpression() {
        assertThat(p).matches(":var + 1")
    }

    @Test
    fun matchesIndicatorVariableExpression() {
        assertThat(p).matches(":var:indicator + 1")
    }

    @Test
    fun matchesVariableExpression() {
        assertThat(p).matches("var + 1")
    }

    @Test
    fun matchesFunctionCallExpression() {
        assertThat(p).matches("func(var) + 1")
    }

    @Test
    fun matchesCollectionCount() {
        assertThat(p).matches("collection.count")
    }

    @Test
    fun matchesCollectionFirst() {
        assertThat(p).matches("collection.first + 1")
    }

    @Test
    fun matchesCollectionLast() {
        assertThat(p).matches("collection.last + 1")
    }

    @Test
    fun matchesCollectionLimit() {
        assertThat(p).matches("collection.limit + 1")
    }

    @Test
    fun matchesCollectionNext() {
        assertThat(p).matches("collection.next(1) + 1")
    }

    @Test
    fun matchesCollectionPrior() {
        assertThat(p).matches("collection.prior(1) + 1")
    }

    @Test
    fun notMatchesQueries() {
        assertThat(p).notMatches("1 + select 1 from dual")
        assertThat(p).notMatches("(select 1 from dual) / select 1 from dual")
    }

}
