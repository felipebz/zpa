/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

class DmlTableExpressionClauseTest : RuleTest() {

    @Before
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

}
