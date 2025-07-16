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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class InsertStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.INSERT_STATEMENT)
    }

    @Test
    fun matchesSimpleInsert() {
        assertThat(p).matches("insert into tab values (1);")
    }

    @Test
    fun matchesInsertWithTableAlias() {
        assertThat(p).matches("insert into tab t values (1);")
    }

    @Test
    fun matchesInsertWithExplicitColumn() {
        assertThat(p).matches("insert into tab (x) values (1);")
    }

    @Test
    fun matchesInsertWithExplicitColumnAlternative() {
        assertThat(p).matches("insert into tab (tab.x) values (1);")
    }

    @Test
    fun matchesInsertMultipleColumns() {
        assertThat(p).matches("insert into tab (x, y) values (1, 2);")
    }

    @Test
    fun matchesInsertWithSubquery() {
        assertThat(p).matches("insert into tab (select 1, 2 from dual);")
    }

    @Test
    fun matchesInsertWithSubqueryInColumns() {
        assertThat(p).matches("insert into tab (x, y) (select 1, 2 from dual);")
    }

    @Test
    fun matchesInsertWithSchema() {
        assertThat(p).matches("insert into sch.tab values (1);")
    }

    @Test
    fun matchesInsertRecord() {
        assertThat(p).matches("insert into tab values foo;")
    }

    @Test
    fun matchesLabeledInsert() {
        assertThat(p).matches("<<foo>> insert into tab values (1);")
    }

    @Test
    fun matchesInsertWithReturningInto() {
        assertThat(p).matches("insert into tab (x) values (1) returning x*2 into y;")
    }

    @Test
    fun matchesSimpleInsertInQuery() {
        assertThat(p).matches("insert into (select x from tab) values (1);")
    }

    @Test
    fun matchesMultiTableInsert() {
        assertThat(p).matches("insert all into tab (x) values (y) into tab (x) values (y) select 1 y from dual;")
    }

    @Test
    fun matchesMultiTableConditionalInsert() {
        assertThat(p).matches("insert all " +
            "when y < 0 then into tab (x) values (y) " +
            "when y > 0 then into tab (x) values (y) " +
            "else into tab (x) values (y) " +
            "select 1 y from dual;")
    }

    @Test
    fun matchesSimpleWithErrorLoggingClause() {
        assertThat(p).matches("insert into tab select 1, 2 from dual log errors into errlog ('oops');")
    }

    @Test
    fun matchesInsertThe() {
        assertThat(p).matches("insert into the(select x from tab) values (1);")
    }

}
