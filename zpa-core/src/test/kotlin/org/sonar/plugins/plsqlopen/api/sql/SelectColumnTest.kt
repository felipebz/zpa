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

class SelectColumnTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(DmlGrammar.SELECT_COLUMN)
    }

    @Test
    fun matchesAllColumns() {
        assertThat(p).matches("*")
    }

    @Test
    fun matchesFunctionCall() {
        assertThat(p).matches("func(var)")
    }

    @Test
    fun matchesCount() {
        assertThat(p).matches("count(*)")
    }

    @Test
    fun matchesColumnName() {
        assertThat(p).matches("name")
    }

    @Test
    fun matchesTableColumn() {
        assertThat(p).matches("tab.name")
    }

    @Test
    fun matchesAllTableColumns() {
        assertThat(p).matches("tab.*")
    }

    @Test
    fun matchesTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.name")
    }

    @Test
    fun matchesAllTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.*")
    }

    @Test
    fun matchesLiterals() {
        assertThat(p).matches("'x'")
        assertThat(p).matches("1")
        assertThat(p).matches("null")
    }

    @Test
    fun matchesSequenceCurrentValue() {
        assertThat(p).matches("seq.currval")
    }

    @Test
    fun matchesSequenceNextValue() {
        assertThat(p).matches("seq.nextval")
    }

    @Test
    fun matchesColumnWithAlias() {
        assertThat(p).matches("null alias")
    }

    @Test
    fun matchesColumnWithAliasWithAsKeyword() {
        assertThat(p).matches("null as alias")
    }

    @Test
    fun matchesSubqueryAsColumn() {
        assertThat(p).matches("(select 1 from dual) col")
    }

    @Test
    fun matchesColumnWithConnectByRoot() {
        assertThat(p).matches("connect_by_root foo")
        assertThat(p).matches("connect_by_root foo alias")
    }

}
