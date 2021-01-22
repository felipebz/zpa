/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api.sql

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class OrderByClauseTest : RuleTest() {

    @Before
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
