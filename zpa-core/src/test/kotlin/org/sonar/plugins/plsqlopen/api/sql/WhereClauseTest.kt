/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class WhereClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.WHERE_CLAUSE)
    }

    @Test
    fun matchesSimpleWhere() {
        assertThat(p).matches("where 1 = 1")
    }

    @Test
    fun matchesColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2")
    }

    @Test
    fun matchesMultipleColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2 and tab.col = tab2.col2")
    }

    @Test
    fun matchesComparationWithSubquery() {
        assertThat(p).matches("where tab.col = (select 1 from dual)")
    }

    @Test
    fun matchesOuterJoin() {
        assertThat(p).matches("where tab.col(+) = tab2.col2")
    }

    @Test
    fun matchesTupleInSelect() {
        assertThat(p).matches("where (foo, bar, baz) in (select 1, 2, 3 from dual)")
    }

    @Test
    fun matchesTupleInValues() {
        assertThat(p).matches("where (foo, bar) in ((1, 2), (2, 3))")
    }

    @Test
    fun matchesExists() {
        assertThat(p).matches("where exists (select 1 from dual)")
    }

    @Test
    fun matchesExistsWithUnionInSubquery() {
        assertThat(p).matches("where exists ((select 1 from dual) union (select 2 from dual))")
    }

    @Test
    fun matchesNotExists() {
        assertThat(p).matches("where not exists (select 1 from dual)")
    }

    @Test
    fun matchesAny() {
        assertThat(p).matches("where data = any (select 1,2,3 from dual)")
    }
}
