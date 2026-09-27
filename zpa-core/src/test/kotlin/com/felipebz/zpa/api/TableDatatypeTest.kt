/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Oracle 26 parses a bare TABLE wherever a datatype is expected. SQL stores it as RAW(16); PL/SQL
 * accepts it only for polymorphic table functions and reports PLS-00765 elsewhere after parsing.
 */
class TableDatatypeTest : RuleTest() {

    @Test
    fun matchesBareTableInSqlDatatypePositions() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create table t (c table);")
        assertThat(p).matches("create table t (c table default null not null, d number);")
        assertThat(p).matches("alter table t add c table;")
        assertThat(p).matches("alter table t add (c table);")
        assertThat(p).matches("alter table t modify c table;")
        assertThat(p).matches("alter table t modify (c table);")
        assertThat(p).matches("create cluster c (k table) size 1024;")
        assertThat(p).matches("create domain d as table;")

        setRootRule(PlSqlGrammar.EXPRESSION)
        assertThat(p).matches("cast(null as table)")
    }

    @Test
    fun rejectsTableTypeSyntaxInSqlDatatypePositions() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        // ORA-03077
        assertThat(p).notMatches("create table t (c table(10));")
        // ORA-03099
        assertThat(p).notMatches("create table t (c table of number);")
        assertThat(p).notMatches("alter table t add c table of number;")
        assertThat(p).notMatches("create cluster c (k table of number) size 1024;")
        // ORA-03048
        assertThat(p).notMatches("create domain d as table of number;")

        setRootRule(PlSqlGrammar.EXPRESSION)
        // ORA-00907
        assertThat(p).notMatches("cast(null as table of number)")
    }

    @Test
    fun matchesBareTableInPlSqlDatatypePositions() {
        setRootRule(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(p).matches("create function f(tab table) return table pipelined row polymorphic using pkg;")

        setRootRule(PlSqlGrammar.CREATE_PROCEDURE)
        // PLS-00765 is reported only after parsing.
        assertThat(p).matches("create procedure p(t table) is begin null; end;")

        setRootRule(PlSqlGrammar.BLOCK_STATEMENT)
        assertThat(p).matches("declare x table; begin null; end;")
    }

}
