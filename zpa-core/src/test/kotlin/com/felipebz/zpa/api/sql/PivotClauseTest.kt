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

class PivotClauseTest: RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.PIVOT_CLAUSE)
    }

    @Test
    fun matchesSimplePivot() {
        assertThat(p).matches("pivot (sum(amount) for quarter in ('q1', 'q2'))")
    }

    @Test
    fun matchesSimplePivotXml() {
        assertThat(p).matches("pivot xml (sum(amount) for quarter in (any))")
    }

    @Test
    fun matchesSimplePivotWithAliases() {
        assertThat(p).matches("pivot (sum(quantity_sold) as qty for region in ('north' north, 'south' south))")
    }

    @Test
    fun matchesSimplePivotWithAs() {
        assertThat(p).matches("pivot (sum(quantity_sold) as qty for region in ('north' as north, 'south' as south))")
    }

    @Test
    fun matchesSimplePivotWithSeveralAggregateExpressions() {
        assertThat(p).matches(
            """
            pivot (
              sum(amount ) as total,
              count(*) as count
              for quarter
              in ('q1' as q1, 'q2' as q2, 'q3' as q3, 'q4' as q4)
            )
            """
        )
    }

    @Test
    fun matchesPivotWithAny() {
        assertThat(p).matches("pivot xml (count(*) for quarter in (any, any))")
    }

    @Test
    fun matchesPivotWithSubquery() {
        assertThat(p).matches(
            "pivot xml (count(*) for quarter in (select distinct quarter from sales_quarters))"
        )
    }

    @Test
    fun matchesPivotWithMultipleColumns() {
        assertThat(p).matches(
            "pivot (sum(amount) for (year, quarter) in ((2025, 'q1') as q1, (2025, 'q2') q2))"
        )
    }

    @Test
    fun rejectsNonAggregateMeasure() {
        assertThat(p).notMatches("pivot (amount for quarter in ('q1'))")
    }

    @Test
    fun rejectsEmptyAggregateArguments() {
        assertThat(p).notMatches("pivot (sum() for quarter in ('q1'))")
    }

    @Test
    fun rejectsParenthesizedMeasure() {
        assertThat(p).notMatches("pivot (() for quarter in ('q1'))")
    }

    @Test
    fun rejectsTrailingCommaInAggregateList() {
        assertThat(p).notMatches("pivot (sum(amount), for quarter in ('q1'))")
    }

    @Test
    fun rejectsTrailingCommaInPivotForColumns() {
        assertThat(p).notMatches("pivot (sum(amount) for (quarter,) in ('q1'))")
    }

    @Test
    fun rejectsTrailingCommaInPivotInValues() {
        assertThat(p).notMatches("pivot (sum(amount) for quarter in ('q1',))")
    }

    @Test
    fun rejectsLiteralAggregateAlias() {
        assertThat(p).notMatches("pivot (sum(amount) 'total' for quarter in ('q1'))")
    }

    @Test
    fun rejectsLiteralPivotValueAlias() {
        assertThat(p).notMatches("pivot (sum(amount) for quarter in ('q1' 'first_quarter'))")
    }

    @Test
    fun rejectsPivotSubqueryWithoutXml() {
        assertThat(p).notMatches("pivot (count(*) for quarter in (select distinct quarter from sales_quarters))")
    }

    @Test
    fun rejectsPivotAnyWithoutXml() {
        assertThat(p).notMatches("pivot (count(*) for quarter in (any))")
    }

    @Test
    fun rejectsPivotXmlWithExplicitValues() {
        assertThat(p).notMatches("pivot xml (count(*) for quarter in ('q1'))")
    }
}
