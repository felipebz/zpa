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

class LobStorageClauseTest : RuleTest() {

    @Test
    fun matchesSingleColumnForms() {
        setRootRule(DdlGrammar.LOB_STORAGE_CLAUSE)
        assertThat(p).matches("lob (a) store as securefile")
        assertThat(p).matches("lob (a) store as basicfile")
        assertThat(p).matches("lob (a) store as securefile seg_a")
        assertThat(p).matches("lob (a) store as basicfile seg_a")
        assertThat(p).matches("lob (a) store as securefile seg_a (tablespace users)")
        assertThat(p).matches("lob (a) store as securefile (tablespace users)")
        assertThat(p).matches("lob (a) store as seg_a")
        assertThat(p).matches("lob (a) store as \"Seg A\" (tablespace users)")
        assertThat(p).matches("lob (a) store as (tablespace users)")
    }

    @Test
    fun matchesMultipleColumnForms() {
        setRootRule(DdlGrammar.LOB_STORAGE_CLAUSE)
        assertThat(p).matches("lob (a, b) store as securefile")
        assertThat(p).matches("lob (a, b) store as basicfile (tablespace users)")
        assertThat(p).matches("lob (a, b) store as (tablespace users)")
    }

    @Test
    fun rejectsInvalidStoreAs() {
        setRootRule(DdlGrammar.LOB_STORAGE_CLAUSE)
        assertThat(p).notMatches("lob (a) store as")
        assertThat(p).notMatches("lob (a, b) store as")
        // Order is fixed: storage type, segment name, parameters (ORA-00922).
        assertThat(p).notMatches("lob (a) store as seg_a securefile")
        assertThat(p).notMatches("lob (a) store as (tablespace users) securefile")
        assertThat(p).notMatches("lob (a) store as (tablespace users) seg_a")
        assertThat(p).notMatches("lob (a) store as (tablespace users) (chunk 8192)")
        assertThat(p).notMatches("lob (a) store as seg_a seg_b")
        assertThat(p).notMatches("lob (a) store as securefile seg_a securefile")
        assertThat(p).notMatches("lob (a) store as s.seg_a")
        assertThat(p).notMatches("lob (a) store as securefile s.seg_a")
        // ORA-43852 / ORA-22850.
        assertThat(p).notMatches("lob (a) store as securefile basicfile")
        assertThat(p).notMatches("lob (a) store as basicfile securefile")
        assertThat(p).notMatches("lob (a) store as securefile securefile")
        // ORA-22855: no segment name for several columns.
        assertThat(p).notMatches("lob (a, b) store as seg_a")
        assertThat(p).notMatches("lob (a, b) store as securefile seg_a")
        assertThat(p).notMatches("lob (a, b) store as seg_a (tablespace users)")
        assertThat(p).notMatches("lob (a, b) store as (tablespace users) securefile")
    }

    @Test
    fun keepsTablePropertiesAfterBareStorageType() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table t (x number, a clob) lob (a) store as securefile tablespace users")
        assertThat(p).matches("create table t (x number, a clob) lob (a) store as securefile nologging parallel 2")
        assertThat(p).matches("create table t (x number, a clob) lob (a) store as securefile enable row movement")
        assertThat(p).matches("create table t (x number, a clob, b blob) lob (a) store as securefile lob (b) store as basicfile")
        assertThat(p).matches(
            "create table t (x number, a clob) lob (a) store as securefile partition by hash (x) partitions 2")
        // Oracle reads PCTFREE as the segment name here, so the statement fails on the number.
        assertThat(p).notMatches("create table t (x number, a clob) lob (a) store as securefile pctfree 10")
    }

    @Test
    fun matchesPartitionAndAlterTableContexts() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches(
            "create table t (x number, a clob, b blob) partition by range (x) (" +
                "partition p1 values less than (10) lob (a) store as securefile seg_p1 (tablespace users), " +
                "partition p2 values less than (maxvalue) lob (a, b) store as basicfile);")
        assertThat(p).matches("alter table t add (b blob) lob (b) store as securefile seg_b (tablespace users);")
        assertThat(p).matches("alter table t move lob (a, b) store as securefile (tablespace users);")
        assertThat(p).notMatches("alter table t move lob (a, b) store as seg_x;")
    }

    @Test
    fun matchesLobParameters() {
        setRootRule(DdlGrammar.LOB_STORAGE_CLAUSE)
        listOf(
            "lob (x) store as securefile x_seg (enable storage in row deduplicate compress medium nocache logging)",
            "lob (\"X\") store as securefile \"X_SEG\" (enable storage in row deduplicate compress medium nocache logging)",
            "lob (x) store as basicfile x_seg (enable storage in row chunk 8192 retention nocache logging)",
            "lob (x) store as securefile (compress high deduplicate)",
            "lob (x) store as securefile (keep_duplicates nocompress)",
            "lob (x) store as securefile (compress)", "lob (x) store as securefile (compress low)",
            "lob (x) store as securefile (decrypt)",
            "lob (x) store as securefile (encrypt using 'AES256' identified by pw1)",
            "lob (x) store as securefile (encrypt)",
            "lob (x) store as securefile (retention max)", "lob (x) store as securefile (retention min 100)",
            "lob (x) store as securefile (retention auto)", "lob (x) store as securefile (retention none)",
            "lob (x) store as basicfile (pctversion 10 freepools 4)",
            "lob (x) store as securefile (enable storage in row 4000)",
            "lob (x) store as basicfile (disable storage in row chunk 8192)",
            "lob (x) store as securefile (cache)", "lob (x) store as securefile (cache logging)",
            "lob (x) store as securefile (cache reads nologging)", "lob (x) store as securefile (nocache nologging)",
            "lob (x) store as securefile (storage (initial 1m) tablespace users)",
            "lob (x) store as securefile (tablespace users chunk 8192 retention deduplicate compress cache)",
            "lob (x) store as securefile (cache compress tablespace users deduplicate retention chunk 8192)",
            "lob (a, b) store as securefile (deduplicate compress medium)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsInvalidAndRepeatedLobParameters() {
        setRootRule(DdlGrammar.LOB_STORAGE_CLAUSE)
        listOf(
            "lob (x) store as securefile (deduplicate keep_duplicates)",
            "lob (x) store as securefile (deduplicate deduplicate)",
            "lob (x) store as securefile (compress nocompress)",
            "lob (x) store as securefile (compress high compress low)",
            "lob (x) store as securefile (compress high medium)",
            "lob (x) store as securefile (cache nocache)", "lob (x) store as securefile (nocache nocache)",
            "lob (x) store as securefile (retention retention)",
            "lob (x) store as securefile (retention min)",
            "lob (x) store as securefile (enable storage in row disable storage in row)",
            "lob (x) store as securefile (chunk 8192 chunk 8192)",
            "lob (x) store as securefile (tablespace users tablespace users)",
            "lob (x) store as securefile (encrypt decrypt)",
            "lob (x) store as securefile (encrypt no salt)",
            "lob (x) store as securefile (disable storage in row 4000)",
            "lob (x) store as securefile (logging)", "lob (x) store as securefile (nologging)",
            "lob (x) store as securefile (chunk)", "lob (x) store as securefile (compress foo)",
            "lob (x) store as securefile ()",
            "lob (a, b) store as seg_x (deduplicate)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesLobParametersInTableStatements() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        listOf(
            "create table t (id number, x clob) lob (x) store as securefile (deduplicate compress medium cache);",
            "alter table t move lob (\"X\") store as securefile \"X_SEG\" (enable storage in row deduplicate compress medium nocache logging);",
            "alter table t move lob (\"X\") store as basicfile \"X_SEG\" (enable storage in row chunk 8192 retention nocache logging);",
            "alter table t move online lob (\"X\") store as securefile \"X_SEG\" (enable storage in row deduplicate compress medium nocache logging);",
            "alter table t move online lob (\"X\") store as basicfile \"X_SEG\" (enable storage in row chunk 8192 retention nocache logging);",
            "alter table t modify lob (x) (deduplicate compress high retention auto)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        assertThat(p).notMatches("alter table t move lob (x) store as securefile x_seg (deduplicate keep_duplicates);")
    }
}
