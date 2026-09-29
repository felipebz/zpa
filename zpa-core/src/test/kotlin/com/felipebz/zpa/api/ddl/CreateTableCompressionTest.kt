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

class CreateTableCompressionTest : RuleTest() {

    private val table = "create table t (a number, b clob)"

    @Test
    fun matchesCompressionForms() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table compress")
        assertThat(p).matches("$table nocompress")
        assertThat(p).matches("$table compress basic")
        assertThat(p).matches("$table row store compress")
        assertThat(p).matches("$table row store compress basic")
        assertThat(p).matches("$table row store compress advanced")
        assertThat(p).matches("$table row store nocompress")
        assertThat(p).matches("$table row store")
    }

    @Test
    fun matchesLegacyCompressFor() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table compress for oltp")
        assertThat(p).matches("$table compress for query")
        assertThat(p).matches("$table compress for query low")
        assertThat(p).matches("$table compress for archive high")
        assertThat(p).matches("$table compress for all operations")
        assertThat(p).matches("$table compress for direct_load operations")
    }

    @Test
    fun matchesColumnStore() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table column store compress")
        assertThat(p).matches("$table column store compress for query low")
        assertThat(p).matches("$table column store compress for memspeed archive high")
        assertThat(p).matches("$table column store compress for query row level locking")
        assertThat(p).matches("$table column store compress no row level locking")
        assertThat(p).matches("$table column store")
        assertThat(p).matches("$table column store row level locking")
        assertThat(p).matches("$table column store no row level locking")
    }

    @Test
    fun rejectsMalformedCompression() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("$table compress advanced")
        assertThat(p).notMatches("$table compress basic advanced")
        assertThat(p).notMatches("$table compress basic for oltp")
        assertThat(p).notMatches("$table compress for")
        assertThat(p).notMatches("$table compress for garbage")
        assertThat(p).notMatches("$table compress for staging")
        assertThat(p).notMatches("$table compress for oltp low")
        assertThat(p).notMatches("$table compress for all")
        assertThat(p).notMatches("$table compress for direct_load")
        assertThat(p).notMatches("$table compress for all direct_load operations")
        assertThat(p).notMatches("$table compress garbage")
        assertThat(p).notMatches("$table row store compress basic advanced")
        assertThat(p).notMatches("$table row store compress for query")
        assertThat(p).notMatches("$table row store garbage")
        assertThat(p).notMatches("$table row compress")
        assertThat(p).notMatches("$table store compress")
        assertThat(p).notMatches("$table column compress")
        assertThat(p).notMatches("$table column store for query")
        assertThat(p).notMatches("$table column store garbage")
        assertThat(p).notMatches("$table column store compress for query memspeed")
        assertThat(p).notMatches("$table column store compress for archive memspeed")
        assertThat(p).notMatches("$table column store compress for memspeed")
        assertThat(p).notMatches("$table row level locking")
        assertThat(p).notMatches("$table compress row level locking")
        assertThat(p).notMatches("$table nocompress row level locking")
    }

    @Test
    fun placesCompressionAmongTableProperties() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$table nocompress tablespace users")
        assertThat(p).matches("$table tablespace users nocompress")
        assertThat(p).matches("$table pctfree 10 nocompress")
        assertThat(p).matches("$table nocompress pctfree 10")
        assertThat(p).matches("$table pctused 40 nocompress initrans 1")
        assertThat(p).matches("$table maxtrans 255 nocompress logging")
        assertThat(p).matches("$table logging nocompress")
        assertThat(p).matches("$table storage (initial 64k) compress")
        assertThat(p).matches("$table compress storage (initial 64k)")
        assertThat(p).matches("$table segment creation immediate pctfree 10 compress")
        assertThat(p).matches("$table compress parallel 2 enable row movement")
        assertThat(p).matches("$table compress annotations (k 'v')")
        assertThat(p).matches("$table compress for oltp for staging")
        assertThat(p).matches("$table compress inmemory")
        assertThat(p).matches("$table compress lob (b) store as securefile")
        assertThat(p).matches("$table compress partition by hash (a) partitions 2")
        assertThat(p).matches("$table partition by hash (a) partitions 2 compress tablespace users")
        // Segment creation must come first (ORA-00922).
        assertThat(p).notMatches("$table compress segment creation immediate")
    }

    @Test
    fun matchesWithoutColumnList() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table t nocompress tablespace users as select 1 a from dual")
        assertThat(p).matches("create table t compress as select 1 a from dual")
        assertThat(p).matches("create table t compress for oltp as select 1 a from dual")
        assertThat(p).matches(
            "create table \"DVSADM\".CMP3\$58238005 nocompress tablespace \"DVS_PROOF\" nologging lob (VALUE) " +
                "store as (tablespace \"DVS_PROOF\" enable storage in row nocache nologging) " +
                "as select * from \"DVSADM\".\"DVS_ARCHIVE\" mytab;")
        assertThat(p).matches(
            "create table c (id number) segment creation immediate pctfree 10 pctused 40 initrans 1 maxtrans 255 " +
                "nocompress logging storage(initial 65536 next 1048576 minextents 1 maxextents 2147483645) tablespace \"USERS\";")
    }

    @Test
    fun acceptsRepeatsAndRestrictionsCheckedAfterParsing() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // Oracle 26 fails these afterwards (ORA-14460, ORA-14451, ORA-25193, ORA-30657, ORA-64307).
        assertThat(p).matches("$table compress nocompress")
        assertThat(p).matches("$table compress for oltp compress basic")
        assertThat(p).matches("create global temporary table t (a number) on commit preserve rows compress")
        assertThat(p).matches("create table t (a number primary key, b number) organization index nocompress")
        assertThat(p).matches("create table t of xmltype compress")
        assertThat(p).matches("create table t of xmltype xmltype store as binary xml compress")
    }

    @Test
    fun keepsStoreClausesWhole() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // ORA-00922: a store clause is never split into two compression properties.
        assertThat(p).notMatches("$table column store nocompress")
        assertThat(p).notMatches("$table column store nocompress tablespace users")
        assertThat(p).notMatches("$table column store compress nocompress")
        assertThat(p).notMatches("$table column store row level locking nocompress")
        assertThat(p).notMatches("$table column store no row level locking nocompress")
        assertThat(p).notMatches("$table column store row store")
        assertThat(p).notMatches("$table column store row store compress")
        assertThat(p).notMatches("$table column store compress for oltp")
        assertThat(p).notMatches("$table column store compress for all operations")
        assertThat(p).notMatches("$table column store compress for staging")
        assertThat(p).notMatches("$table row store compress for staging")
        assertThat(p).notMatches("$table row store compress for query")
        assertThat(p).notMatches("$table row store compress for archive")
        assertThat(p).notMatches("$table row store compress basic for oltp")
        assertThat(p).notMatches("$table row store compress for oltp low")
        assertThat(p).matches("$table column store for staging")
        assertThat(p).matches("$table row store for staging")
        assertThat(p).matches("$table row store compress for oltp for staging")
        assertThat(p).matches("$table column store compress for query for staging")
        assertThat(p).matches("$table column store row level locking compress for query")
        assertThat(p).matches("$table column store row level locking tablespace users")
        assertThat(p).matches("$table row store compress for oltp")
        assertThat(p).matches("$table row store compress for all operations")
        assertThat(p).matches("$table row store compress for direct_load operations")
        // Whole repeated clauses fail with ORA-14460 only after parsing.
        assertThat(p).matches("$table column store column store")
        assertThat(p).matches("$table column store compress compress")
        assertThat(p).matches("$table row store compress nocompress")
        assertThat(p).matches("$table row store column store")
        assertThat(p).matches("$table compress column store")
    }

    @Test
    fun readsCompressAfterBareLobStorageTypeAsSegmentName() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // ORA-00906: COMPRESS directly after SECUREFILE is taken as the segment name.
        assertThat(p).notMatches("$table lob (b) store as securefile compress")
        assertThat(p).notMatches("$table lob (b) store as securefile nocompress")
        assertThat(p).notMatches("$table lob (b) store as compress")
        assertThat(p).matches("$table lob (b) store as securefile seg1 compress")
        assertThat(p).matches("$table lob (b) store as securefile (tablespace users) compress")
        assertThat(p).matches("$table lob (b) store as seg1 compress")
        assertThat(p).matches("$table lob (b) store as (tablespace users) nocompress")
    }

    @Test
    fun matchesPartitionAndAlterCompression() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches(
            "create table t (a number) partition by range (a) (" +
                "partition p1 values less than (10) compress for oltp, " +
                "partition p2 values less than (maxvalue) row store compress advanced);")
        assertThat(p).matches("alter table t compress for oltp;")
        assertThat(p).matches("alter table t column store compress for query low no row level locking;")
        assertThat(p).matches("alter table t row store compress basic;")
        assertThat(p).notMatches("alter table t compress advanced;")
    }
}
