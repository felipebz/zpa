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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.asSemantic
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun matchesListAggWithDelimiter() {
        assertThat(p).matches("listagg(foo, ',') within group (order by bar)")
    }

    @Test
    fun matchesListAggWithDelimiter2() {
        assertThat(p).matches("listagg(foo, chr(10)) within group (order by bar)")
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
    fun matchesListAggWithWindowName() {
        assertThat(p).matches("listagg(foo) over list_window")
    }

    @Test
    fun matchesListAggWithWindowNameAndPartitionBy() {
        assertThat(p).matches("listagg(foo) over (list_window partition by baz)")
    }

    @Test
    fun rejectsListAggWithEmptyAnalyticClause() {
        assertThat(p).notMatches("listagg(foo) over ()")
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
        assertThat(p).notMatches("listagg(foo) within group (order by bar) over (list_window order by baz)")
        assertThat(p).notMatches("listagg(foo) within group (order by bar) over (partition by baz order by qux)")
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
