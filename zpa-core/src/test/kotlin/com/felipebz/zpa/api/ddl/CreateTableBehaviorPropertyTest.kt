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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateTableBehaviorPropertyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    private fun assertMatches(vararg statements: String) =
        statements.forEach { assertThat(p).describedAs(it).matches(it) }

    private fun assertNotMatches(vararg statements: String) =
        statements.forEach { assertThat(p).describedAs(it).notMatches(it) }

    @Test
    fun matchesCacheAndNocache() {
        assertMatches(
            "create table t (id number) cache;", "create table t (id number) nocache;",
            "create table t (id number) logging cache;", "create table t (id number) cache logging;",
            "create table t (id number) nologging nocache;", "create table t (id number) tablespace users cache;",
            "create table t (id number) cache tablespace users;", "create table t (id number) pctfree 10 cache;",
            "create table t (id number) compress cache;", "create table t (id number) cache compress;",
            "create table t (id number) parallel cache;", "create table t (id number) cache parallel 2;",
            "create table t (id number) cache enable row movement;",
            "create table t (id number) cache partition by hash (id) partitions 2;",
            "create table t (id number) partition by hash (id) partitions 2 cache;",
        )
        assertNotMatches("create table t (id number) cache reads;", "create table t (id number) no cache;")
    }

    @Test
    fun matchesReadOnlyAndReadWrite() {
        assertMatches(
            "create table t (id number) read only;", "create table t (id number) read write;",
            "create table t (id number) tablespace users read only;", "create table t (id number) read only tablespace users;",
            "create table t (id number) logging read only;", "create table t (id number) read only logging;",
            "create table t (id number) cache read only;", "create table t (id number) read only cache;",
            "create table t (id number) parallel read only;", "create table t (id number) read only parallel 2;",
            "create table t (id number) read only enable row movement;",
            "create table t (id number) read only partition by hash (id) partitions 2;",
            "create table t (id number) partition by hash (id) partitions 2 read only;",
        )
        assertNotMatches("create table t (id number) read;", "create table t (id number) read only write;")
    }

    @Test
    fun matchesFlashbackArchive() {
        assertMatches(
            "create table t (id number) flashback archive;", "create table t (id number) flashback archive fa;",
            "create table t (id number) flashback archive \"fa\";", "create table t (id number) no flashback archive;",
            "create table t (id number) tablespace users flashback archive fa;",
            "create table t (id number) flashback archive fa tablespace users;",
            "create table t (id number) logging flashback archive;", "create table t (id number) cache flashback archive;",
            "create table t (id number) read only flashback archive;", "create table t (id number) flashback archive read only;",
            "create table t (id number) parallel flashback archive;", "create table t (id number) flashback archive parallel 2;",
            "create table t (id number) flashback archive tablespace users;",
            "create table t (id number) flashback archive enable row movement;",
            "create table t (id number) flashback archive fa enable row movement;",
            "create table t (id number) flashback archive partition by hash (id) partitions 2;",
            "create table t (id number) partition by hash (id) partitions 2 flashback archive fa;",
            "create table t (id number) no flashback archive enable row movement;",
        )
        assertNotMatches(
            "create table t (id number) no flashback archive fa;", "create table t (id number) flashback archive fa.x;",
        )
    }

    @Test
    fun matchesPropertiesOfSelectAndObjectTables() {
        assertMatches(
            "create table t cache as select 1 a from dual;", "create table t (a) read only as select 1 a from dual;",
            "create table t no flashback archive as select 1 a from dual;",
            "create table t flashback archive as select 1 a from dual;",
            "create table t (a) flashback archive fa as select 1 a from dual;",
            "create table t of ty cache;", "create table t of ty read only;", "create table t of ty flashback archive fa;",
            "create table t of xmltype nocache;", "create table t of xmltype no flashback archive;",
            "create json collection table t cache;", "create json collection table t read only;",
            "create json collection table t flashback archive fa;",
            "create immutable table t (id number) no drop until 16 days idle no delete until 16 days after insert cache;",
            "create immutable table t (id number) no drop until 16 days idle no delete until 16 days after insert read only;",
        )
        assertNotMatches(
            "create immutable table t (id number) cache no drop until 16 days idle no delete until 16 days after insert;",
        )
    }

    @Test
    fun placesPropertiesAfterOrganizationClauses() {
        val external = "organization external (type oracle_loader default directory d access parameters (x) location ('a'))"
        assertMatches(
            "create table t (id number) $external cache;", "create table t (id number) $external read only;",
            "create table t (id number) $external flashback archive fa;",
            "create table t (id number primary key) organization index cache;",
            "create table t (id number primary key) organization index read only;",
            "create table t (id number primary key) organization index no flashback archive;",
        )
        assertNotMatches(
            "create table t (id number) cache $external;", "create table t (id number) read only $external;",
            "create table t (id number primary key) cache organization index;",
            "create table t (id number primary key) flashback archive fa organization index;",
        )
    }

    @Test
    fun acceptsPropertiesInsideIndexOrganizedClause() {
        val iot = "create table t (id number primary key) organization index"
        assertMatches(
            "$iot read only;", "$iot read only overflow;", "$iot read write overflow;",
            "$iot cache overflow;", "$iot nocache overflow;", "$iot no flashback archive overflow;",
            "$iot flashback archive overflow;", "$iot flashback archive fa overflow;",
            "$iot overflow read only;", "$iot overflow cache;", "$iot overflow no flashback archive;",
            "$iot overflow flashback archive fa;",
            "$iot read only including id overflow;", "$iot including id read only overflow;",
            "$iot including id overflow read only;", "$iot cache including id overflow;",
            "$iot including id cache overflow;", "$iot flashback archive fa including id overflow;",
            "$iot including id flashback archive overflow;", "$iot flashback archive including id overflow;",
            "$iot flashback archive pctthreshold 10 overflow;", "$iot flashback archive mapping table overflow;",
            "$iot read only pctfree 10 overflow;", "$iot pctfree 10 read only overflow;",
            "$iot tablespace users read only overflow;", "$iot read only tablespace users overflow;",
            "$iot read only nocompress overflow;", "$iot nocompress read only overflow;",
            "$iot read only pctthreshold 10 overflow;", "$iot read only mapping table overflow;",
            "$iot mapping table read only overflow;", "$iot read only cache overflow;",
        )
        assertNotMatches(
            "$iot read overflow;", "$iot read only write overflow;", "$iot no cache overflow;",
            "$iot no flashback archive fa overflow;", "$iot flashback archive fa.x overflow;",
            "create table t (id number primary key) read only organization index overflow;",
            "create table t (id number primary key) cache organization index overflow;",
            "create table t (id number primary key) no flashback archive organization index overflow;",
        )
    }

    @Test
    fun insideIndexOrganizedClauseTheyStayInTheClauseNode() {
        val tree = p.parse("create table t (id number primary key) organization index read only overflow cache")
        val clause = tree.getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)
        assertThatAst(clause.hasDirectChildren(PlSqlKeyword.READ)).isTrue()
        assertThatAst(clause.hasDirectChildren(PlSqlKeyword.CACHE)).isTrue()
        assertThatAst(tree.getDescendants(DdlGrammar.TABLE_BEHAVIOR_PROPERTY)).isEmpty()
    }

    @Test
    fun placesPropertiesAfterOnCommit() {
        assertMatches(
            "create global temporary table t (id number) on commit delete rows cache;",
            "create global temporary table t (id number) on commit preserve rows nocache;",
            "create global temporary table t (id number) on commit delete rows read only;",
            "create global temporary table t (id number) on commit delete rows flashback archive fa;",
        )
        assertNotMatches(
            "create global temporary table t (id number) cache on commit delete rows;",
            "create global temporary table t (id number) read only on commit delete rows;",
        )
    }

    @Test
    fun privateTemporaryTablesTakeOnlyCache() {
        assertMatches(
            "create private temporary table ora\$ptt_t (id number) on commit drop definition cache;",
            "create private temporary table ora\$ptt_t (id number) on commit preserve definition nocache;",
        )
        assertNotMatches(
            "create private temporary table ora\$ptt_t (id number) cache on commit drop definition;",
            "create private temporary table ora\$ptt_t (id number) on commit drop definition read only;",
            "create private temporary table ora\$ptt_t (id number) on commit drop definition read write;",
            "create private temporary table ora\$ptt_t (id number) on commit drop definition no flashback archive;",
            "create private temporary table ora\$ptt_t (id number) on commit drop definition flashback archive fa;",
        )
    }

    private fun nodeAfterArchive(container: com.felipebz.flr.api.AstNode): String {
        val children = container.children
        val archive = children.indexOfFirst { it.name == "ARCHIVE" }
        return children[archive + 1].name
    }

    @Test
    fun followingPropertiesAreNotReadAsTheFlashbackArchiveName() {
        listOf(
            "cache", "nocache", "logging", "nologging", "parallel 2", "parallel", "noparallel", "compress", "nocompress",
            "compress for oltp", "row store compress basic", "column store compress for query high",
            "enable row movement", "disable row movement", "pctfree 10", "pctused 10", "initrans 2", "maxtrans 2",
            "storage (initial 1m)", "tablespace users", "read only", "read write", "no flashback archive", "inmemory",
            "no inmemory", "annotations (a 'b')", "for staging", "partition by hash (id) partitions 2",
            "lob (x) store as (cache)", "nested table x store as y", "varray x store as lob y",
        ).forEach { property ->
            val sql = "create table t (id number, x clob) flashback archive $property"
            val tree = p.parse(sql)
            assertThatAst(nodeAfterArchive(tree)).describedAs(sql).isNotEqualTo("IDENTIFIER_NAME")
        }
        assertThatAst(nodeAfterArchive(p.parse("create table t flashback archive as select 1 id from dual")))
            .isNotEqualTo("IDENTIFIER_NAME")
        assertThatAst(nodeAfterArchive(p.parse("create global temporary table t (id number) on commit delete rows flashback archive cache")))
            .isEqualTo("CACHE")
    }

    @Test
    fun followingPropertiesAreNotReadAsTheArchiveNameInsideIndexOrganizedClause() {
        listOf(
            "nocompress", "nomapping", "logging", "nologging", "cache", "nocache", "compress", "mapping table",
            "pctthreshold 10", "including id", "tablespace users", "pctfree 10", "read only", "no flashback archive",
        ).forEach { attribute ->
            val sql = "create table t (id number primary key) organization index flashback archive $attribute overflow"
            val clause = p.parse(sql).getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)
            assertThatAst(nodeAfterArchive(clause)).describedAs(sql).isNotEqualTo("IDENTIFIER_NAME")
        }
        val clause = p.parse("create table t (id number primary key) organization index flashback archive nocompress overflow")
            .getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)
        assertThatAst(nodeAfterArchive(clause)).isEqualTo("NOCOMPRESS")
    }

    @Test
    fun rejectLimitAfterFlashbackArchiveIsNotTheArchiveName() {
        val external = "create table t (id number) organization external " +
            "(type oracle_loader default directory d location ('x.dat'))"
        listOf("flashback archive reject limit 10", "flashback archive reject limit unlimited",
            "flashback archive fa reject limit 10", "reject limit 10 flashback archive",
            "reject limit 10 flashback archive fa").forEach { tail ->
            assertMatches("$external $tail;")
        }
        val bare = p.parse("$external flashback archive reject limit 10")
            .getFirstDescendant(DdlGrammar.EXTERNAL_TABLE_CLAUSE)
        assertThatAst(nodeAfterArchive(bare)).isEqualTo("EXTERNAL_TABLE_REJECT_LIMIT")
        assertThatAst(bare.getFirstChild(DdlGrammar.EXTERNAL_TABLE_REJECT_LIMIT).tokens.map { it.originalValue })
            .containsExactly("reject", "limit", "10")
        val named = p.parse("$external flashback archive fa reject limit 10")
            .getFirstDescendant(DdlGrammar.EXTERNAL_TABLE_CLAUSE)
        assertThatAst(nodeAfterArchive(named)).isEqualTo("IDENTIFIER_NAME")
        assertThatAst(named.hasDirectChildren(DdlGrammar.EXTERNAL_TABLE_REJECT_LIMIT)).isTrue()
        assertNotMatches("$external flashback archive limit 10;")
    }

    @Test
    fun unquotedKeywordsAreNotArchiveNames() {
        assertNotMatches(
            *listOf(
                "reject", "mapping", "nomapping", "including", "overflow", "pctthreshold", "as", "on", "partition",
                "limit", "segment", "key", "data", "user", "index", "type", "version", "archive", "text",
            ).map { "create table t (id number) flashback archive $it;" }.toTypedArray(),
        )
    }

    @Test
    fun namedFlashbackArchivesStillKeepTheirName() {
        listOf(
            "my_archive", "fa", "\"My Archive\"", "archive_cache", "prefix", "reject_limit", "overflow_x", "mapping1", "as_x",
            "\"REJECT\"", "\"MAPPING\"", "\"OVERFLOW\"", "\"AS\"", "\"ON\"", "\"SEGMENT\"", "\"LIMIT\"",
        ).forEach { name ->
            val sql = "create table t (id number) flashback archive $name"
            assertThatAst(nodeAfterArchive(p.parse(sql))).describedAs(sql).isEqualTo("IDENTIFIER_NAME")
        }
        val iot = p.parse("create table t (id number primary key) organization index flashback archive my_archive overflow")
            .getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)
        assertThatAst(nodeAfterArchive(iot)).isEqualTo("IDENTIFIER_NAME")
        assertMatches("create table t (id number) flashback archive my_archive cache;")
    }

    @Test
    fun propertiesDoNotAddAstNodes() {
        val tree = p.parse("create table t (id number) cache read only flashback archive fa no flashback archive")
        assertThatAst(tree.hasDirectChildren(PlSqlKeyword.CACHE)).isTrue()
        assertThatAst(tree.hasDirectChildren(PlSqlKeyword.READ)).isTrue()
        assertThatAst(tree.hasDirectChildren(PlSqlKeyword.FLASHBACK)).isTrue()
        assertThatAst(tree.getChildren(PlSqlKeyword.FLASHBACK)).hasSize(2)
        assertThatAst(tree.hasDirectChildren(DdlGrammar.TABLE_BEHAVIOR_PROPERTY)).isFalse()
        assertThatAst(tree.getDescendants(DdlGrammar.TABLE_BEHAVIOR_PROPERTY)).isEmpty()
    }
}
