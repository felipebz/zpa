/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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

class RowLimitingClauseTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(DmlGrammar.ROW_LIMITING_CLAUSE)
    }

    @Test
    fun matchesOffsetRow() {
        assertThat(p).matches("offset 1 row")
    }

    @Test
    fun matchesOffsetRows() {
        assertThat(p).matches("offset 1 rows")
    }

    @Test
    fun matchesOffsetAndFetchRowClause() {
        assertThat(p).matches("offset 1 row fetch first 1 row only")
    }

    @Test
    fun matchesFetchFirstRowOnly() {
        assertThat(p).matches("fetch first 1 row only")
    }

    @Test
    fun matchesFetchNextRowOnly() {
        assertThat(p).matches("fetch next 1 row only")
    }

    @Test
    fun matchesFetchFirstRowWithTies() {
        assertThat(p).matches("fetch first 1 row with ties")
    }

    @Test
    fun matchesFetchFirstPercentRowOnly() {
        assertThat(p).matches("fetch first 1 percent row only")
    }

}
