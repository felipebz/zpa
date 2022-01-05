/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class DeleteStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.DELETE_STATEMENT)
    }

    @Test
    fun matchesSimpleDelete() {
        assertThat(p).matches("delete tab;")
    }

    @Test
    fun matchesDeleteFrom() {
        assertThat(p).matches("delete from tab;")
    }

    @Test
    fun matchesDeleteWithWhere() {
        assertThat(p).matches("delete from tab where x = 1;")
    }

    @Test
    fun matchesDeleteWithAlias() {
        assertThat(p).matches("delete tab t;")
    }

    @Test
    fun matchesDeleteWithSchema() {
        assertThat(p).matches("delete sch.tab;")
    }

    @Test
    fun matchesDeleteCurrentOf() {
        assertThat(p).matches("delete tab where current of cur;")
    }

    @Test
    fun matchesLabeledDelete() {
        assertThat(p).matches("<<foo>> delete tab;")
    }

    @Test
    fun matchesDeleteFromQuery() {
        assertThat(p).matches("delete (select * from dual);")
    }

    @Test
    fun matchesDeleteWithReturningInto() {
        assertThat(p).matches("delete from tab returning x bulk collect into y;")
    }
}
