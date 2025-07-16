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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class DmlTableExpressionClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
    }

    @Test
    fun matchesTable() {
        assertThat(p).matches("tab")
    }

    @Test
    fun matchesTableInSchema() {
        assertThat(p).matches("sch.tab")
    }

    @Test
    fun matchesTableWithDbLink() {
        assertThat(p).matches("tab@link")
        assertThat(p).matches("tab@link.domain.com")
    }

    @Test
    fun matchesTableWithAlias() {
        assertThat(p).matches("tab alias")
        assertThat(p).matches("tab as alias")
    }

    @Test
    fun matchesSubquery() {
        assertThat(p).matches("(select 1 from dual)")
    }

    @Test
    fun matchesFunctionCall() {
        assertThat(p).matches("func(var)")
        assertThat(p).matches("table(xmlsequence(x))")
    }

    @Test
    fun matchesNestedClause() {
        assertThat(p).matches("tab nested doc columns (foo path foo) alias")
    }

    @Test
    fun matchesNestedClauseWithExplicitPath() {
        assertThat(p).matches("tab nested path doc columns (foo path foo)")
    }

    @Test
    fun matchesNestedClauseWithRelativeObjectAccess() {
        assertThat(p).matches("tab nested doc.c1[*].c2 columns (foo path foo)")
    }

    @Test
    fun matchesNestedClauseWithSimplePathAccess() {
        assertThat(p).matches("tab nested doc, '$.c1[*].c2' columns (foo path foo)")
    }

}
