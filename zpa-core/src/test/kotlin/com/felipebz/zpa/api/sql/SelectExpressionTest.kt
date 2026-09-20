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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class SelectExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
    }

    @Test
    fun matchesSimpleSelect() {
        assertThat(p).matches("select 1 from dual")
    }

    @Test
    fun matchesSimpleSelectInto() {
        assertThat(p).matches("select 1 into var from dual")
    }

    @Test
    fun matchesSelectBulkCollectInto() {
        assertThat(p).matches("select 1 bulk collect into var from dual")
    }

    @Test
    fun matchesSelectWithWhere() {
        assertThat(p).matches("select 1 from dual where 1 = 1")
    }

    @Test
    fun matchesSelectWithMultipleColumns() {
        assertThat(p).matches("select 1, 2 from dual")
    }

    @Test
    fun matchesSelectWithMultipleColumnsAndIntoClause() {
        assertThat(p).matches("select 1, 2 into var1, var2 from dual")
    }

    @Test
    fun matchesSelectWithMultipleTables() {
        assertThat(p).matches("select 1 from emp, dept")
    }

    @Test
    fun matchesSelectAll() {
        assertThat(p).matches("select all 1 from dual")
    }

    @Test
    fun matchesSelectDistinct() {
        assertThat(p).matches("select distinct 1 from dual")
    }

    @Test
    fun matchesSelectUnique() {
        assertThat(p).matches("select unique 1 from dual")
    }

    @Test
    fun matchesSelectWithGroupBy() {
        assertThat(p).matches("select 1 from dual group by 1")
    }

    @Test
    fun matchesSelectWithOrderBy() {
        assertThat(p).matches("select 1 from dual order by 1")
    }

    @Test
    fun matchesSelectWithParenthesis() {
        assertThat(p).matches("(select 1 from dual)")
    }

    @Test
    fun matchesSelectWithUnion() {
        assertThat(p).matches("select 1 from dual union select 2 from dual")
        assertThat(p).matches("(select 1 from dual) union (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithUnionAll() {
        assertThat(p).matches("select 1 from dual union all select 2 from dual")
        assertThat(p).matches("(select 1 from dual) union all (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithMinus() {
        assertThat(p).matches("select 1 from dual minus select 2 from dual")
        assertThat(p).matches("(select 1 from dual) minus (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithMinusAll() {
        assertThat(p).matches("select 1 from dual minus all select 2 from dual")
        assertThat(p).matches("(select 1 from dual) minus all (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithIntersect() {
        assertThat(p).matches("select 1 from dual intersect select 2 from dual")
        assertThat(p).matches("(select 1 from dual) intersect (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithIntersectAll() {
        assertThat(p).matches("select 1 from dual intersect all select 2 from dual")
        assertThat(p).matches("(select 1 from dual) intersect all (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithExcept() {
        assertThat(p).matches("select 1 from dual except select 2 from dual")
        assertThat(p).matches("(select 1 from dual) except (select 2 from dual)")
    }

    @Test
    fun matchesSelectWithExceptAll() {
        assertThat(p).matches("select 1 from dual except all select 2 from dual")
        assertThat(p).matches("(select 1 from dual) except all (select 2 from dual)")
    }

    @Test
    fun matchesSelectCountDistinct() {
        assertThat(p).matches("select count(distinct foo) from dual")
    }

    @Test
    fun matchesSelectWithAnalyticFunction() {
        assertThat(p).matches("select count(foo) over () from dual")
        assertThat(p).matches("select (count(foo) over ()) from dual")
        assertThat(p).matches("select func(count(foo) over ()) from dual")
    }

    @Test
    fun matchesSelectWithAnsiJoin() {
        assertThat(p).matches("select 1 from foo join bar on join.id = bar.id")
    }

    @Test
    fun matchesSelectWithMixedJoinSyntax() {
        assertThat(p).matches("select 1 from foo join bar on join.id = bar.id, baz")
        assertThat(p).matches("select 1 from baz, foo join bar on join.id = bar.id")
    }

    @Test
    fun matchesSelectWithLateralInlineView() {
        assertThat(p).matches("select 1 from foo, lateral (select id from bar where bar.id = foo.id)")
        assertThat(p).matches("select 1 from foo, lateral (select id from bar where bar.id = foo.id) baz")
    }

    @Test
    fun matchesSelectWithLateralInJoin() {
        assertThat(p).matches("select 1 from foo cross join lateral (select id from bar where bar.id = foo.id)")
        assertThat(p).matches("select 1 from foo left join lateral (select id from bar where bar.id = foo.id) baz on 1 = 1")
    }

    @Test
    fun matchesSelectWithPartitionExtendedTableName() {
        assertThat(p).matches("select 1 from foo partition (part1)")
        assertThat(p).matches("select 1 from foo partition (part1) bar")
        assertThat(p).matches("select 1 from foo partition for (1)")
        assertThat(p).matches("select 1 from foo partition for (1, 2)")
        assertThat(p).matches("select 1 from foo subpartition (subpart1) bar")
        assertThat(p).matches("select 1 from foo subpartition (subpart1)")
        assertThat(p).matches("select 1 from foo subpartition for ('a', 1) bar")
        assertThat(p).matches("select 1 from foo join bar partition (part1) baz on baz.id = foo.id")
    }

    @Test
    fun doesNotMatchInvalidPartitionExtensionForms() {
        assertThat(p).notMatches("select 1 from foo partition (1 + 1)")
        assertThat(p).notMatches("select 1 from foo partition (part1, part2)")
        assertThat(p).notMatches("select 1 from foo subpartition (1 + 1)")
        assertThat(p).notMatches("select 1 from foo subpartition (subpart1, subpart2)")
    }

    @Test
    fun matchesSelectWithFunctionSpecificNullTreatment() {
        assertThat(p).matches("select first_value(foo ignore nulls) over (order by bar) from dual")
        assertThat(p).matches("select first_value(foo) ignore nulls over (order by bar) from dual")
        assertThat(p).matches("select last_value(foo respect nulls) over (order by bar) from dual")
        assertThat(p).matches("select last_value(foo) respect nulls over (order by bar) from dual")
        assertThat(p).matches("select lag(foo ignore nulls) over (order by bar) from dual")
        assertThat(p).matches("select lag(foo ignore nulls, 1) over (order by bar) from dual")
        assertThat(p).matches("select lag(foo ignore nulls, 1, 0) over (order by bar) from dual")
        assertThat(p).matches("select lag(foo, 1, 0) ignore nulls over (order by bar) from dual")
        assertThat(p).matches("select lead(foo respect nulls, 1) over (order by bar) from dual")
        assertThat(p).matches("select lead(foo, 1) respect nulls over (order by bar) from dual")
        assertThat(p).matches("select nth_value(foo, 2) ignore nulls over (order by bar) from dual")
        assertThat(p).matches("select nth_value(foo, 2) from first over (order by bar) from dual")
        assertThat(p).matches("select nth_value(foo, 2) from last ignore nulls over (order by bar) from dual")
    }

    @Test
    fun matchesSelectWithOrdinaryAnalyticFunctionForms() {
        assertThat(p).matches("select lag(foo, 1) over (order by bar) from dual")
        assertThat(p).matches("select first_value(foo) over (order by bar) from dual")
        assertThat(p).matches("select nth_value(foo, 2) over (order by bar) from dual")
    }

    @Test
    fun doesNotMatchInvalidNullTreatmentPlacement() {
        assertThat(p).notMatches("select lag(foo, 1 ignore nulls) over (order by bar) from dual")
        assertThat(p).notMatches("select lead(foo, 1 respect nulls) over (order by bar) from dual")
        assertThat(p).notMatches("select lag(foo ignore nulls, 1) respect nulls over (order by bar) from dual")
        assertThat(p).notMatches("select lead(foo respect nulls, 1) ignore nulls over (order by bar) from dual")
        assertThat(p).notMatches("select first_value(foo ignore nulls) respect nulls over (order by bar) from dual")
        assertThat(p).notMatches("select last_value(foo respect nulls) ignore nulls over (order by bar) from dual")
        assertThat(p).notMatches("select nth_value(foo, 2 ignore nulls) over (order by bar) from dual")
        assertThat(p).notMatches("select sum(1 ignore nulls) over () from dual")
        assertThat(p).notMatches("select sum(1) ignore nulls over () from dual")
        assertThat(p).notMatches("select lower(foo ignore nulls) from dual")
        assertThat(p).notMatches("select lower(foo) ignore nulls over () from dual")
        assertThat(p).notMatches("select abs(1 respect nulls) from dual")
    }

    @Test
    fun matchesSelectWithCrossApply() {
        assertThat(p).matches("select 1 from foo cross apply (select id from bar where bar.id = foo.id)")
        assertThat(p).matches("select 1 from foo cross apply (select id from bar where bar.id = foo.id) baz")
        assertThat(p).matches("select 1 from foo cross apply table(foo.items) baz")
    }

    @Test
    fun matchesSelectWithOuterApply() {
        assertThat(p).matches("select 1 from foo outer apply (select id from bar where bar.id = foo.id) baz")
        assertThat(p).matches("select 1 from foo outer apply table(foo.items)")
    }

    @Test
    fun matchesSelectWithGroupByAfterHierarchicalQuery() {
        assertThat(p).matches("select 1 from foo start with a = 1 connect by prior b = c group by d")
        assertThat(p).matches("select 1 from foo start with a = 1 connect by prior b = c group by d having count(1) > 1")
    }

    @Test
    fun matchesSelectWithOffsetWithoutOrderBy() {
        assertThat(p).matches("select 1 from dual offset 1 rows")
        assertThat(p).matches("select 1 from dual offset (a - 1) * b rows fetch next b rows only")
    }

    @Test
    fun matchesOffsetAsAnAlias() {
        assertThat(p).matches("select 1 from some_table offset")
        assertThat(p).matches("select offset.id from some_table offset")
        assertThat(p).matches("select 1 from some_table offset where offset.id = 1")
    }

    @Test
    fun matchesSelectWithCountUnique() {
        assertThat(p).matches("select count(unique foo) from dual")
    }

    @Test
    fun matchesSelectWithSubqueryFactoring() {
        assertThat(p).matches("with foo as (select id from tab) select 1 from foo join bar on join.id = bar.id")
    }

    @Test
    fun matchesSelectForUpdateBeforeOrderBy() {
        assertThat(p).matches("select * from foo for update order by bar ")
    }

    @Test
    fun matchesSelectForUpdateAfterOrderBy() {
        assertThat(p).matches("select * from foo order by baz for update")
    }

    @Test
    fun matchesSelectWithParenthesisForUpdate() {
        assertThat(p).matches("(select 1 from dual) for update")
    }

    @Test
    fun matchesSelectUsingBulkAsAnAlias() {
        assertThat(p).matches("select 1 bulk into var from dual")
    }

    @Test
    fun matchesSelectBulkCollectUsingBulkAsAnAlias() {
        assertThat(p).matches("select 1 bulk bulk collect into var from dual")
    }

    @Test
    fun matchesSelectWithFetchFirstRowsOnly() {
        assertThat(p).matches("select 1 from dual fetch first 1 row only")
    }

    @Test
    fun matchesSelectWithOrderByAndFetchFirstRows() {
        assertThat(p).matches("select 1 from dual order by 1 fetch first 1 row only")
    }

    @Test
    fun matchesSelectFromValues() {
        assertThat(p).matches("select 1 from (values (1, 'foo'), (2, 'bar')) as t(a, b)")
    }

    @Test
    fun matchesShortSelect() {
        assertThat(p).matches("select 1")
        assertThat(p).matches("select 1 into var")
    }

    @Test
    fun matchesShortSelectWithWhere() {
        assertThat(p).matches("select 1 where 1 = 1")
        assertThat(p).matches("select 1 into var where 1 = 1")
    }
}
