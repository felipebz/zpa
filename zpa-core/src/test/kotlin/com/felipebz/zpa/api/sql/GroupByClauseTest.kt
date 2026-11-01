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
