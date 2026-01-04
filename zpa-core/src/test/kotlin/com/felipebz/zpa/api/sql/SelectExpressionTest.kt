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
