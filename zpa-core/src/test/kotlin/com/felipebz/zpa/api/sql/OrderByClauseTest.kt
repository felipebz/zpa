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

class OrderByClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.ORDER_BY_CLAUSE)
    }

    @Test
    fun matchesSimpleOrderBy() {
        assertThat(p).matches("order by 1")
    }

    @Test
    fun matchesSimpleOrderByAsc() {
        assertThat(p).matches("order by 1 asc")
    }

    @Test
    fun matchesSimpleOrderByDesc() {
        assertThat(p).matches("order by 1 desc")
    }

    @Test
    fun matchesSimpleOrderByColumn() {
        assertThat(p).matches("order by col")
    }

    @Test
    fun matchesOrderByWithMultipleValuesAndOrdering() {
        assertThat(p).matches("order by col1 asc, col2 desc, col3 desc")
    }

    @Test
    fun matchesSimpleOrderByTableColumn() {
        assertThat(p).matches("order by tab.col")
    }

    @Test
    fun matchesSimpleOrderByTableColumnWithSchema() {
        assertThat(p).matches("order by sch.tab.col")
    }

    @Test
    fun matchesSimpleOrderByFunctionCall() {
        assertThat(p).matches("order by func(var)")
    }

    @Test
    fun matchesOrderSiblingsBy() {
        assertThat(p).matches("order siblings by col")
    }

    @Test
    fun matchesOrderByNullsFirst() {
        assertThat(p).matches("order by col nulls first")
    }

    @Test
    fun matchesOrderByNullsLast() {
        assertThat(p).matches("order by col nulls last")
    }

}
