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

class GroupByClauseTest : RuleTest() {

    @Before
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

}
