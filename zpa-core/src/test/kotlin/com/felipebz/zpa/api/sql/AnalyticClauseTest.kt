//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalyticClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.ANALYTIC_CLAUSE)
    }

    @Test
    fun matchesSimpleOver() {
        assertThat(p).matches("over ()")
    }

    @Test
    fun matchesOverWindowName() {
        assertThat(p).matches("over window_name")
    }

    @Test
    fun matchesOverWindowNameWithPartitionBy() {
        assertThat(p).matches("over (window_name partition by foo)")
    }

    @Test
    fun matchesOverPartitionBy() {
        assertThat(p).matches("over (partition by foo)")
    }

    @Test
    fun matchesOverPartitionByWithMultipleExpressions() {
        assertThat(p).matches("over (partition by foo, bar, baz)")
    }

    @Test
    fun matchesOverOrderBy() {
        assertThat(p).matches("over (order by foo)")
    }

    @Test
    fun matchesOverWithWindowingUnboundedPreceding() {
        assertThat(p).matches("over (order by foo rows unbounded preceding)")
        assertThat(p).matches("over (order by foo range unbounded preceding)")
    }

    @Test
    fun matchesOverWithWindowingCurrentRow() {
        assertThat(p).matches("over (order by foo rows current row)")
    }

    @Test
    fun matchesOverWithWindowingExpressionPreceding() {
        assertThat(p).matches("over (order by foo rows 1 preceding)")
    }

    @Test
    fun matchesOverWithWindowingWithBetween() {
        assertThat(p).matches("over (order by foo rows between unbounded preceding and current row)")
    }

    @Test
    fun matchesLongAnalyticClause() {
        assertThat(p).matches("over (partition by foo order by foo rows between unbounded preceding and unbounded following)")
    }

    @Test
    fun matchesOverPartitionByExpression() {
        assertThat(p).matches("over (partition by foo + bar)")
    }

}
