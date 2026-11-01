//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.asSemantic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class ListAggExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleListAgg() {
        assertThat(p).matches("listagg(foo) within group (order by bar)")
    }

    @Test
    fun matchesListAggAll() {
        assertThat(p).matches("listagg(all foo) within group (order by bar)")
    }

    @Test
    fun matchesListAggDistinct() {
        assertThat(p).matches("listagg(distinct foo) within group (order by bar)")
    }

    @Test
    fun matchesListAggUniqueWithDelimiter() {
        assertThat(p).matches("listagg(unique ename, ',') within group (order by hiredate)")
    }

    @Test
    fun matchesListAggUniqueWithOverflowAndAnalyticPartition() {
        assertThat(p).matches(
            "listagg(unique ename, ',' on overflow truncate) within group (order by hiredate) over (partition by deptno)"
        )
    }

    @Test
    fun matchesListAggWithDelimiter() {
        assertThat(p).matches("listagg(foo, ',') within group (order by bar)")
    }

    @Test
    fun matchesListAggWithDelimiter2() {
        assertThat(p).matches("listagg(foo, chr(10)) within group (order by bar)")
    }

    @Test
    fun matchesMultipleOrderByItemsWithDirectionAndNullPlacement() {
        assertThat(p).matches("listagg(foo, ',') within group (order by bar desc nulls last, baz asc nulls first)")
    }

    @Test
    fun rejectsOrderSiblingsByWithinGroup() {
        assertThat(p).notMatches("listagg(foo) within group (order siblings by bar)")
    }

    @Test
    fun matchesListAggOverflowError() {
        assertThat(p).matches("listagg(foo on overflow error) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncate() {
        assertThat(p).matches("listagg(foo on overflow truncate) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithIndicator() {
        assertThat(p).matches("listagg(foo on overflow truncate '...') within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithCount() {
        assertThat(p).matches("listagg(foo on overflow truncate '...' with count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithoutCount() {
        assertThat(p).matches("listagg(foo on overflow truncate '...' without count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithoutIndicatorWithCount() {
        assertThat(p).matches("listagg(foo on overflow truncate with count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithoutIndicatorWithoutCount() {
        assertThat(p).matches("listagg(foo on overflow truncate without count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithNullIndicator() {
        assertThat(p).matches("listagg(foo on overflow truncate null with count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithRawCompatibleIndicator() {
        assertThat(p).matches("listagg(foo on overflow truncate utl_raw.cast_to_raw('...') without count) within group (order by bar)")
    }

    @Test
    fun matchesListAggOverflowTruncateWithConstantExpressionIndicator() {
        assertThat(p).matches("listagg(foo on overflow truncate '...' || '!' with count) within group (order by bar)")
    }

    @Test
    fun matchesListAggWithoutWithinGroup() {
        assertThat(p).matches("listagg(foo)")
    }

    @Test
    fun matchesListAggWithNonReservedFilterIdentifier() {
        assertThat(p).matches("listagg(filter)")
    }

    @Test
    fun matchesListAggPartitionBy() {
        assertThat(p).matches("listagg(foo) within group (order by bar) over (partition by baz)")
    }

    @Test
    fun matchesListAggPartitionByWithoutWithinGroup() {
        assertThat(p).matches("listagg(foo) over (partition by baz)")
    }

    @Test
    fun matchesListAggWithDelimiterAndPartitionBy() {
        assertThat(p).matches("listagg(foo, ';') over (partition by grp)")
    }

    @Test
    fun matchesListAggWithWindowName() {
        assertThat(p).matches("listagg(foo) over list_window")
    }

    @Test
    fun matchesListAggWithWindowNameAndPartitionBy() {
        assertThat(p).matches("listagg(foo) over (list_window partition by baz)")
    }

    @Test
    fun matchesListAggWithEmptyAnalyticClause() {
        assertThat(p).matches("listagg(foo) over ()")
    }

    @Test
    fun matchesListAggWithDelimiterAndEmptyAnalyticClause() {
        assertThat(p).matches("listagg(foo, ';') over ()")
    }

    @Test
    fun matchesListAggWithWithinGroupAndEmptyAnalyticClause() {
        assertThat(p).matches("listagg(foo, ';') within group (order by foo) over ()")
    }

    @Test
    fun rejectsListAggWindowNameWithoutPartitionBy() {
        assertThat(p).notMatches("listagg(foo) over (list_window)")
    }

    @Test
    fun matchesListAggFilter() {
        assertThat(p).matches("listagg(foo) filter (where bar = 1)")
    }

    @Test
    fun preservesFilterClauseAstBoundary() {
        val filterClause = p.parse("listagg(foo) filter (where bar = 1)")
            .getFirstDescendant(AggregateSqlFunctionsGrammar.FILTER_CLAUSE)

        assertThatAst(filterClause.asSemantic().type).isEqualTo(AggregateSqlFunctionsGrammar.FILTER_CLAUSE)
    }

    @Test
    fun matchesListAggWithWindowNameAndFilter() {
        assertThat(p).matches("listagg(foo) over list_window filter (where bar = 1)")
    }

    @Test
    fun matchesListAggWithWithinGroupAndFilter() {
        assertThat(p).matches("listagg(foo) within group (order by bar) filter (where bar = 1)")
    }

    @Test
    fun matchesListAggWithAnalyticClauseAndFilter() {
        assertThat(p).matches("listagg(foo) within group (order by bar) over (partition by baz) filter (where bar = 1)")
    }

    @Test
    fun rejectsListAggAnalyticOrderBy() {
        assertThat(p).notMatches("listagg(foo) within group (order by bar) over (order by baz)")
        assertThat(p).notMatches("listagg(unique ename, ',') within group (order by hiredate) over (partition by deptno order by hiredate)")
        assertThat(p).notMatches("listagg(foo, ';') over (order by foo)")
        assertThat(p).notMatches("listagg(foo) within group (order by bar) over (list_window order by baz)")
        assertThat(p).notMatches("listagg(foo) within group (order by bar) over (partition by baz order by qux)")
    }

    @Test
    fun rejectsListAggAnalyticWindowing() {
        assertThat(p).notMatches("listagg(foo) over (partition by grp rows between unbounded preceding and current row)")
    }

    @Test
    fun rejectsListAggFilterBeforeAnalyticClause() {
        assertThat(p).notMatches("listagg(foo) filter (where bar = 1) over list_window")
    }

    @Test
    fun rejectsIncompleteListAggFilter() {
        assertThat(p).notMatches("listagg(foo) filter (where)")
    }

    @Test
    fun rejectsIncompleteListAggWithinGroup() {
        assertThat(p).notMatches("listagg(foo) within group")
    }

    @Test
    fun rejectsIncompleteListAggOverflowClause() {
        assertThat(p).notMatches("listagg(foo on overflow truncate without) within group (order by bar)")
        assertThat(p).notMatches("listagg(foo on overflow truncate '...' with) within group (order by bar)")
    }

    @Test
    fun matchesLongListAgg() {
        assertThat(p).matches("listagg(foo, ',') within group (order by bar) over (partition by baz)")
    }

}
