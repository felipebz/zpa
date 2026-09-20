/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RowPatternGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RowPatternClauseTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(RowPatternGrammar.ROW_PATTERN_CLAUSE)
    }

    @Test
    fun matchesOracleExample() {
        assertThat(p).matches(
            """
            match_recognize (
                partition by symbol
                order by tstamp
                measures strt.tstamp as start_tstamp,
                         last(down.tstamp) as bottom_tstamp,
                         last(up.tstamp) as end_tstamp
                one row per match
                after match skip to last up
                pattern (strt down+ up+)
                define
                    down as down.price < prev(down.price),
                    up as up.price > prev(up.price)
            )
            """.trimIndent()
        )
    }

    @Test
    fun matchesAllRowPatternClauses() {
        assertThat(p).matches(
            """
            match_recognize (
                partition by symbol, exchange
                order by tstamp desc
                measures classifier() classifier_name,
                         match_number() as match_number,
                         next(a.price) as next_price,
                         first(a.price) as first_price
                all rows per match
                after match skip to next row
                pattern (^ a+? | {- b -} c{,4}? $)
                subset u = (a, b)
                define
                    a as a.price > 0,
                    b as b.price < 0,
                    c as c.price = 0
            )
            """.trimIndent()
        )
    }

    @Test
    fun matchesPatternOperators() {
        setRootRule(RowPatternGrammar.ROW_PATTERN)

        assertThat(p).matches("a|b c{2,4}?")
        assertThat(p).matches("permute(a, b, c)")
        assertThat(p).matches("()")
        assertThat(p).matches("a{2} b{3,} c{,4} d? e??")
        // Oracle accepts {,} and its reluctant form although the syntax table omits them.
        assertThat(p).matches("a{,}")
        assertThat(p).matches("a{,}?")
    }

    @Test
    fun matchesSelectWithMatchRecognize() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select *
            from ticker match_recognize (
                partition by symbol
                order by tstamp
                measures last(up.tstamp) as end_tstamp
                one row per match
                pattern (up+)
                define up as up.price > prev(up.price)
            ) mr
            order by mr.symbol, mr.end_tstamp
            """.trimIndent()
        )
    }

    @Test
    fun matchesMatchRecognizeClauseAndAlias() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches(
            "select * from dual match_recognize (pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "select match_recognize.dummy from dual match_recognize"
        )
    }

    @Test
    fun rejectsRowPatternWithoutDefine() {
        assertThat(p).notMatches("match_recognize (pattern (a))")
    }

    @Test
    fun rejectsSpacedMatchRecognize() {
        assertThat(p).notMatches("match recognize (pattern (a) define a as 1 = 1)")
    }

    @Test
    fun matchesRowPatternOrderOptions() {
        assertThat(p).matches(
            "match_recognize (order by first_col asc nulls first, second_col desc nulls last " +
                "pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun rejectsNonSimpleRowPatternColumns() {
        assertThat(p).notMatches(
            "match_recognize (partition by dual.dummy pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "match_recognize (order by dual.dummy pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "match_recognize (partition by upper(dummy) pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "match_recognize (order by upper(dummy) pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "match_recognize (order siblings by dummy pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun matchesAllRowsPerMatchOptions() {
        listOf(
            "all rows per match",
            "all rows per match show empty matches",
            "all rows per match omit empty matches",
            "all rows per match with unmatched rows"
        ).forEach { rowsPerMatch ->
            assertThat(p).matches(
                "match_recognize ($rowsPerMatch pattern (a) define a as 1 = 1)"
            )
        }
    }

    @Test
    fun matchesScopedRunningAndFinalSemantics() {
        assertThat(p).matches(
            "match_recognize (measures final last(a.col) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (measures running last(a.col) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (measures final count(*) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (measures running sum(a.col) + 1 as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (pattern (a) define a as running last(a.col) > 0)"
        )
    }

    @Test
    fun matchesCountPatternVariableStarInMeasuresAndDefine() {
        assertThat(p).matches(
            "match_recognize (measures count(a.*) as cnt pattern (a) define a as count(a.*) > 0)"
        )
    }

    @Test
    fun admitsDistinctAggregateSyntaxForSemanticValidation() {
        assertThat(p).matches(
            "match_recognize (measures count(distinct a.dummy) as cnt pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun admitsAllAndDistinctScopedAggregates() {
        listOf(
            "final count(all a.col)",
            "final count(distinct a.col)",
            "running sum(all a.col)",
            "running sum(distinct a.col)"
        ).forEach { aggregate ->
            assertThat(p).matches(
                "match_recognize (measures $aggregate as x pattern (a) define a as 1 = 1)"
            )
        }
    }

    @Test
    fun rejectsBareStarForNonCountRowAggregates() {
        assertThat(p).notMatches(
            "match_recognize (measures running sum(*) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "match_recognize (measures final avg(*) as x pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun admitsOrdinaryFunctionsInMeasureExpressions() {
        assertThat(p).matches(
            "match_recognize (measures upper(a.dummy) as x pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun preservesNestedPivotBeforeMatchRecognize() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).matches(
            "select * from (select * from t pivot (count(*) for category in (1))) " +
                "match_recognize (pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun rejectsPivotAndMatchRecognizeAtSameTableLevel() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        assertThat(p).notMatches(
            "select * from t pivot (count(*) for category in (1)) " +
                "match_recognize (pattern (a) define a as 1 = 1)"
        )
        assertThat(p).notMatches(
            "select * from t match_recognize (pattern (a) define a as 1 = 1) " +
                "pivot (count(*) for category in (1))"
        )
    }

    @Test
    fun matchesRuntimeConstantNavigationOffsets() {
        assertThat(p).matches(
            "match_recognize (measures first(a.col, 2) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (measures first(a.col, :offset) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (measures first(a.col, :offset + 1) as x pattern (a) define a as 1 = 1)"
        )
        assertThat(p).matches(
            "match_recognize (pattern (a) define a as prev(a.col, :offset + 1) > 0)"
        )
        assertThat(p).matches(
            "match_recognize (measures next(final last(a.col), :offset) as x " +
                "pattern (a) define a as 1 = 1)"
        )
    }

    @Test
    fun rejectsFinalOrRunningPhysicalNavigationInDefine() {
        assertThat(p).notMatches(
            "match_recognize (pattern (a) define a as final last(a.col) > 0)"
        )
        assertThat(p).notMatches(
            "match_recognize (pattern (a) define a as running prev(a.col) > 0)"
        )
    }

}
