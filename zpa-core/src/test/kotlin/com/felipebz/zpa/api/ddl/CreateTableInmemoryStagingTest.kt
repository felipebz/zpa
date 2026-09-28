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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class CreateTableInmemoryStagingTest : RuleTest() {

    private val table = "create table t (id number, d clob, j json)"

    @Test
    fun matchesInmemoryText() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table inmemory text (d, j)")
        assertThat(p).matches("$table inmemory text (\"D\", t.j)")
        assertThat(p).matches("$table inmemory text (d using 'p1', j using 'p2')")
        assertThat(p).matches("$table no inmemory text (d)")
        assertThat(p).matches("$table no inmemory text (d using 'p1')")
    }

    @Test
    fun matchesInmemoryClauseCombinations() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table inmemory priority critical inmemory text (d, j)")
        assertThat(p).matches("$table inmemory text (d, j) inmemory priority critical")
        assertThat(p).matches("$table inmemory text (d) inmemory text (j)")
        assertThat(p).matches("$table inmemory memcompress for query inmemory text (d)")
        assertThat(p).matches("$table inmemory text (d) no inmemory")
        assertThat(p).matches("$table inmemory (id) inmemory text (d) no inmemory (j)")
        assertThat(p).matches("$table inmemory text (d) inmemory (id) inmemory priority low")
        // Oracle rejects a second table-level INMEMORY afterwards as a duplicate option (ORA-64350).
        assertThat(p).matches("$table inmemory priority high inmemory priority low")
    }

    @Test
    fun rejectsMalformedInmemoryText() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // TEXT must directly follow INMEMORY (ORA-00922).
        assertThat(p).notMatches("$table inmemory priority critical text (d, j)")
        assertThat(p).notMatches("$table inmemory memcompress for query text (d)")
        assertThat(p).notMatches("$table inmemory text (d, j) priority critical")
        assertThat(p).notMatches("$table inmemory text d")
        assertThat(p).notMatches("$table inmemory text ()")
        assertThat(p).notMatches("$table inmemory text (d,)")
        assertThat(p).notMatches("$table inmemory text (d) (j)")
        assertThat(p).notMatches("$table inmemory text (d), (j)")
        assertThat(p).notMatches("$table inmemory text (d j)")
        assertThat(p).notMatches("$table inmemory text (upper(d))")
        // The policy name must be a string literal (ORA-01780).
        assertThat(p).notMatches("$table inmemory text (d using p1)")
        assertThat(p).notMatches("$table inmemory text (d) using 'p'")
        assertThat(p).notMatches("$table no inmemory text")
        assertThat(p).notMatches("$table inmemory all (id)")
    }

    @Test
    fun matchesForStaging() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table staging_table (col1 number, col2 varchar2(100)) for staging;")
        assertThat(p).matches(
            "create table part_staging_table (col1 number, col2 varchar2(100)) " +
                "partition by range (col1) (partition p1 values less than (100), " +
                "partition pmax values less than (maxvalue)) for staging;")
        assertThat(p).matches("$table for staging partition by hash (id) partitions 2")
        assertThat(p).matches("$table for staging tablespace users parallel 2 enable row movement")
        assertThat(p).matches("$table tablespace users for staging annotations (a 'b')")
        assertThat(p).matches("$table annotations (a 'b') for staging")
        assertThat(p).matches("$table inmemory priority critical for staging inmemory text (d)")
    }

    @Test
    fun rejectsMalformedForStaging() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // NOT FOR STAGING is ALTER-only (ORA-00922 in CREATE).
        assertThat(p).notMatches("$table not for staging")
        assertThat(p).notMatches("$table for garbage")
        assertThat(p).notMatches("$table staging")
    }

    @Test
    fun matchesAlterTableInmemoryText() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter table t inmemory priority critical inmemory text (d);")
        assertThat(p).matches("alter table t no inmemory text (d, j);")
        assertThat(p).matches("alter table t inmemory text (d using 'p1');")
        assertThat(p).matches("alter table t not for staging;")
        assertThat(p).notMatches("alter table t inmemory priority critical text (d);")
        assertThat(p).notMatches("alter table t inmemory text (d using p1);")
    }
}
