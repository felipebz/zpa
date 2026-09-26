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

class TablespaceTest : RuleTest() {

    @Test
    fun matchesPermanentTablespace() {
        setRootRule(DdlGrammar.CREATE_TABLESPACE)
        assertThat(p).matches("create tablespace omf_ts1;")
        assertThat(p).matches("create tablespace ts datafile autoextend off")
        assertThat(p).matches("create bigfile tablespace ts datafile 'a.dbf' size 20m autoextend on;")
        assertThat(p).matches("create tablespace if not exists ts datafile 'a.dbf' size 10m, 'b.dbf' size 10m")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 500k reuse autoextend on next 500k maxsize 100m")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 5 autoextend on maxsize unlimited")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 1m extent management local uniform size 128k")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 1m extent management local segment space management auto")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 1m encryption using 'AES256' encrypt")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 1m encryption using 'AES256' mode 'XTS' encrypt")
        assertThat(p).matches("create tablespace ts logging datafile 'a.dbf' size 1m online")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' blocksize 8k minimum extent 64k force logging " +
            "nologging offline flashback off extent management dictionary")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' default table compress for oltp " +
            "index compress advanced low storage (initial 1m)")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' default compress for query high")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' default storage (initial 1m next 1m)")
        assertThat(p).matches("create tablespace ts datafile 'a.dbf' size 1m lost write protection in shardspace s")
    }

    @Test
    fun matchesUndoAndTemporaryTablespaces() {
        setRootRule(DdlGrammar.CREATE_TABLESPACE)
        assertThat(p).matches("create undo tablespace u datafile 'u.dbf' size 10m autoextend on retention guarantee;")
        assertThat(p).matches("create bigfile undo tablespace u datafile 'u.dbf' retention noguarantee " +
            "extent management local encryption encrypt")
        assertThat(p).matches("create temporary tablespace t;")
        assertThat(p).matches("create smallfile temporary tablespace t tempfile 't.dbf' size 5m autoextend on " +
            "tablespace group g extent management local uniform")
        assertThat(p).matches("create temporary tablespace t tablespace group ''")
        assertThat(p).matches("create local temporary tablespace for leaf t tempfile 't.dbf' size 1m")
    }

    @Test
    fun rejectsCreateFormsOracleRejects() {
        setRootRule(DdlGrammar.CREATE_TABLESPACE)
        // ORA-02236: file names are character literals.
        assertThat(p).notMatches("create tablespace ts datafile ts.df size 10m")
        // ORA-02180: file specification parts keep their order; IF NOT EXISTS precedes the name.
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' reuse size 5")
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' autoextend on maxsize 1g next 1m")
        assertThat(p).notMatches("create tablespace ts if not exists datafile 'a.dbf'")
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' extent management local uniform 1m")
        // ORA-02236 for a parenthesized file list, which only redo log files accept.
        assertThat(p).notMatches("create tablespace ts datafile ('a.dbf', 'b.dbf') size 1m")
        // Each kind of tablespace has its own options (ORA-30044, ORA-30024, ORA-25139).
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' retention guarantee")
        assertThat(p).notMatches("create undo tablespace u datafile 'u.dbf' logging")
        assertThat(p).notMatches("create temporary tablespace t datafile 't.dbf'")
        assertThat(p).notMatches("create temporary tablespace t tempfile 't.dbf' logging")
        // Bare LOST WRITE PROTECTION ends the options (ORA-65480); ENABLE is ALTER-only (ORA-02180).
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' lost write protection logging")
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' enable lost write protection")
        // ORA-00905 for an empty DEFAULT; STORAGE must be last (ORA-02180).
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' default")
        assertThat(p).notMatches("create tablespace ts datafile 'a.dbf' default storage (initial 1m) table nocompress")
    }

    @Test
    fun matchesAlterTablespaceAttributes() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        assertThat(p).matches("alter tablespace ts begin backup;")
        assertThat(p).matches("alter tablespace ts end backup")
        assertThat(p).matches("alter tablespace ts offline normal")
        assertThat(p).matches("alter tablespace ts online")
        assertThat(p).matches("alter tablespace if exists ts read only")
        assertThat(p).matches("alter tablespace ts permanent")
        assertThat(p).matches("alter tablespace ts rename datafile 'a.dbf', 'b.dbf' to 'c.dbf', 'd.dbf'")
        assertThat(p).matches("alter tablespace ts rename to ts2")
        assertThat(p).matches("alter tablespace ts add datafile 'a.dbf' size 100k autoextend on next 10k maxsize 100k")
        assertThat(p).matches("alter tablespace ts add datafile;")
        assertThat(p).matches("alter tablespace ts add tempfile 't.dbf' size 5 autoextend on")
        assertThat(p).matches("alter tablespace ts drop datafile 'a.dbf'")
        assertThat(p).matches("alter tablespace ts drop tempfile 5")
        assertThat(p).matches("alter tablespace ts datafile offline")
        assertThat(p).matches("alter tablespace ts shrink space keep 10m")
        assertThat(p).matches("alter tablespace ts shrink tempfile 't.dbf' keep 1m")
        assertThat(p).matches("alter tablespace ts resize 10m")
        assertThat(p).matches("alter tablespace ts coalesce")
        assertThat(p).matches("alter tablespace ts no force logging")
        assertThat(p).matches("alter tablespace ts nologging")
        assertThat(p).matches("alter tablespace ts tablespace group ''")
        assertThat(p).matches("alter tablespace ts autoextend on next 1m maxsize unlimited")
        assertThat(p).matches("alter tablespace ts flashback on")
        assertThat(p).matches("alter tablespace ts retention noguarantee")
        assertThat(p).matches("alter tablespace ts default table nocompress")
        assertThat(p).matches("alter tablespace ts enable lost write protection")
        assertThat(p).matches("alter tablespace ts suspend lost write protection")
        assertThat(p).matches("alter tablespace ts encryption online using 'AES256' encrypt " +
            "file_name_convert = ('a', 'b') keep")
        assertThat(p).matches("alter tablespace ts encryption finish rekey")
        assertThat(p).matches("alter tablespace ts encryption offline decrypt")
        assertThat(p).matches("alter tablespace ts encryption encrypt")
    }

    @Test
    fun rejectsAlterFormsOracleRejects() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        assertThat(p).notMatches("alter tablespace ts")
        // ORA-03049: only one attribute per statement.
        assertThat(p).notMatches("alter tablespace ts online nologging")
        assertThat(p).notMatches("alter tablespace ts begin backup end backup")
        assertThat(p).notMatches("alter tablespace ts add datafile 'a.dbf' size 1m logging")
        // ORA-02142: ALTER needs ENABLE, REMOVE or SUSPEND.
        assertThat(p).notMatches("alter tablespace ts lost write protection")
        assertThat(p).notMatches("alter tablespace ts rename datafile 'a.dbf'")
    }
}
