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

class GroupByClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.GROUP_BY_CLAUSE)
    }

    @Test
    fun matchesSimpleGroupBy() {
        assertThat(p).matches("group by 1")
    }

    @Test
    fun matchesSimpleGroupByColumn() {
        assertThat(p).matches("group by col")
    }

    @Test
    fun matchesSimpleGroupByExpression() {
        assertThat(p).matches("group by (col) + col2")
    }

    @Test
    fun matchesSimpleGroupByMultipleColumn() {
        assertThat(p).matches("group by col, col2")
    }

    @Test
    fun matchesSimpleGroupByTableColumn() {
        assertThat(p).matches("group by tab.col")
    }

    @Test
    fun matchesSimpleGroupByTableColumnWithSchema() {
        assertThat(p).matches("group by sch.tab.col")
    }

    @Test
    fun matchesSimpleGroupByFunctionCall() {
        assertThat(p).matches("group by func(var)")
    }

    @Test
    fun matchesSimpleGroupByRollup() {
        assertThat(p).matches("group by rollup (col)")
    }

    @Test
    fun matchesSimpleGroupByCube() {
        assertThat(p).matches("group by cube (col)")
    }

    @Test
    fun matchesSimpleGroupByRollupAndCube() {
        assertThat(p).matches("group by rollup (col), cube (col)")
    }

    @Test
    fun matchesSimpleGroupWithEmptyGroupingSets() {
        assertThat(p).matches("group by grouping sets (())")
    }

    @Test
    fun matchesSimpleGroupWithGroupingSets() {
        assertThat(p).matches("group by grouping sets (col)")
    }

    @Test
    fun matchesSimpleGroupWithGroupingSetsWithMultipleColumns() {
        assertThat(p).matches("group by grouping sets (col, col2)")
    }

    @Test
    fun matchesSimpleGroupWithGroupingSetsWithMultipleOptions() {
        assertThat(p).matches("group by grouping sets (col, col2, rollup (col), cube (col))")
    }

}
