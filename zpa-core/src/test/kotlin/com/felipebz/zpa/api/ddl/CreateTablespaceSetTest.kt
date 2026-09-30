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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CreateTablespaceSetTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLESPACE_SET)
    }

    @Test
    fun matchesOptionalShardspaceAndTemplate() {
        for (statement in listOf(
            "create tablespace set ts",
            "create tablespace set ts in shardspace s;",
            "create tablespace set ts using template (datafile size 100m)",
            "create tablespace set ts in shardspace s using template (datafile size 100m " +
                "extent management local segment space management auto);",
            "create tablespace set shardspace in shardspace shardspace using template (logging)",
            "create tablespace set \"Shard Space\" in shardspace \"Shard Space\" using template (offline)",
            "create tablespace set contents using template (datafile size 20m)",
            "create tablespace set datafiles using template (datafile autoextend off)"
        )) {
            assertThat(p).describedAs(statement).matches(statement)
        }
    }

    @Test
    fun matchesOnlyTemplateFileSpecifications() {
        for (files in listOf(
            "datafile",
            "datafile size 20m",
            "datafile autoextend off",
            "datafile autoextend on",
            "datafile size 20m autoextend on next 1m",
            "datafile autoextend on maxsize unlimited",
            "datafile size 20m autoextend on next 1m maxsize 2g",
            "datafile size 20m, size 30m autoextend off",
            "datafile autoextend off, autoextend on next 1m maxsize unlimited"
        )) {
            assertThat(p).describedAs(files).matches("create tablespace set ts using template ($files)")
        }
    }

    @Test
    fun matchesPermanentAttributeSubsetAndOrdering() {
        for (attributes in listOf(
            "logging",
            "nologging force logging",
            "blocksize 8k online",
            "offline flashback off",
            "flashback on",
            "encryption",
            "encryption encrypt",
            "encryption decrypt",
            "encryption using 'AES256' mode 'XTS' encrypt",
            "extent management local",
            "extent management local autoallocate",
            "extent management local uniform",
            "extent management local uniform size 1m",
            "extent management dictionary",
            "segment space management auto",
            "segment space management auto nologging extent management local blocksize 8k",
            "logging force logging default table compress for oltp extent management local " +
                "segment space management auto flashback off",
            "lost write protection"
        )) {
            assertThat(p).describedAs(attributes).matches("create tablespace set ts using template ($attributes)")
            assertThat(p).describedAs("datafile $attributes")
                .matches("create tablespace set ts using template (datafile size 20m $attributes)")
        }
        // Oracle reports duplicate attributes as option validation (ORA-25119/ORA-25146), not a
        // different production. The documented attribute repetition needs no parser context state.
        assertThat(p).matches("create tablespace set ts using template (logging logging)")
        assertThat(p).matches("create tablespace set ts using template (extent management local extent management local)")
    }

    @Test
    fun matchesAllDefaultParameterFamilies() {
        for (parameters in listOf(
            "table compress for oltp",
            "compress for query high",
            "table nocompress",
            "index compress advanced low",
            "index compress advanced high table nocompress",
            "index nocompress",
            "storage (encrypt initial 1m next 1m minextents 1 maxextents unlimited maxsize 1g pctincrease 0)",
            "inmemory",
            "no inmemory",
            "inmemory memcompress for dml priority none distribute auto duplicate",
            "inmemory priority critical memcompress for capacity high distribute by rowid range " +
                "for service svc duplicate all",
            "inmemory no memcompress priority medium distribute for service all no duplicate",
            "inmemory priority high distribute for service default",
            "inmemory distribute for service none",
            "inmemory memcompress auto",
            "inmemory text (body)",
            "inmemory memcompress for query high text (body using 'text_policy', app.docs.title)",
            "inmemory spatial shape",
            "inmemory spatial shape text (body using 'text_policy')",
            "inmemory table nocompress",
            "table compress for archive low index nocompress inmemory ilm enable_all storage (initial 1m)",
            "ilm add policy row store compress advanced segment after 30 days of no modification",
            "ilm add policy column store compress for query low group on app.policy_fn",
            "ilm add policy row store compress advanced row after 1 month of no modification",
            "ilm add policy column store compress for query row after 1 year of no modification",
            "ilm add policy tier to archive_ts",
            "ilm add policy tier to archive_ts group on app.policy_fn",
            "ilm add policy tier to archive_ts read only segment after 2 months of creation",
            "ilm add policy set inmemory memcompress for query low segment after 2 days of no access",
            "ilm add policy modify inmemory no memcompress on app.policy_fn",
            "ilm add policy no inmemory after 2 years of creation",
            "ilm delete policy p",
            "ilm enable policy p",
            "ilm disable policy p",
            "ilm delete_all",
            "ilm enable_all",
            "ilm disable_all"
        )) {
            assertThat(p).describedAs(parameters).matches("create tablespace set ts using template (default $parameters)")
        }
    }

    @Test
    fun rejectsOrdinaryTablespaceOptionsAndMisplacedTemplateParts() {
        for (template in listOf(
            "datafile 'a.dbf' size 20m",
            "datafile size 20m reuse",
            "datafile ('a.dbf')",
            "tempfile size 20m",
            "minimum extent 1m",
            "segment space management manual",
            "retention guarantee",
            "tablespace group g",
            "undo",
            "temporary",
            "in shardspace s",
            "logging datafile size 20m",
            "datafile size 20m datafile size 30m",
            "lost write protection logging",
            "default index compress",
            "default index compress 2",
            "default storage (freelists 1)",
            "default storage (buffer_pool keep)",
            "default storage (initial 1m) table nocompress",
            "default inmemory distribute by partition",
            "default inmemory distribute by subpartition",
            "default inmemory (x)",
            "default no inmemory (x)",
            "default inmemory text ()",
            "default inmemory text (body,)",
            "default inmemory text (body using)",
            "default inmemory text (body using text_policy)",
            "default inmemory text (body + 1)",
            "default no inmemory text (body)",
            "default inmemory spatial",
            "default ilm add policy",
            "default ilm delete policy",
            "default ilm add policy tier to archive_ts read only",
            "default ilm add policy row store compress advanced row after 1 day of no access",
            "default ilm add policy modify inmemory after 1 day of no access",
            "default ilm add policy no inmemory after 1 day of no creation"
        )) {
            assertThat(p).describedAs(template).notMatches("create tablespace set ts using template ($template)")
        }
    }

    @Test
    fun rejectsMalformedHeadersTemplatesAndAutoextension() {
        for (statement in listOf(
            "create tablespace set",
            "create tablespace set if not exists ts",
            "create bigfile tablespace set ts",
            "create undo tablespace set ts",
            "create temporary tablespace set ts",
            "create tablespace set ts in shardspace",
            "create tablespace set ts in shardspace s in shardspace s2",
            "create tablespace set ts using template (logging) in shardspace s",
            "create tablespace set ts datafile size 20m",
            "create tablespace set ts logging",
            "create tablespace set ts using",
            "create tablespace set ts using template",
            "create tablespace set ts using template ()",
            "create tablespace set ts using template (datafile size 20m",
            "create tablespace set ts using template (datafile size 20m,) ",
            "create tablespace set ts using template (datafile , size 20m)",
            "create tablespace set ts using template (datafile size 20m,, size 30m)",
            "create tablespace set ts using template (datafile size)",
            "create tablespace set ts using template (datafile autoextend)",
            "create tablespace set ts using template (datafile autoextend off next 1m)",
            "create tablespace set ts using template (datafile autoextend on next)",
            "create tablespace set ts using template (datafile autoextend on maxsize)",
            "create tablespace set ts using template (datafile autoextend on maxsize 2g next 1m)",
            "create tablespace set ts using template (default)",
            "create tablespace set ts using template (segment space management)",
            "create tablespace set ts using template (logging) using template (offline)"
        )) {
            assertThat(p).describedAs(statement).notMatches(statement)
        }
    }

    @Test
    fun parsesAllSetStatementsWithoutOrdinaryOrGenericFallback() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create tablespace set shardspace in shardspace shardspace using template " +
            "(datafile size 20m extent management local segment space management auto); " +
            "alter tablespace set shardspace force logging; " +
            "drop tablespace set shardspace keep quota including contents and datafiles cascade constraints; " +
            "create table contents (datafiles number);")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_TABLESPACE_SET)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_TABLESPACE_SET)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.DROP_TABLESPACE_SET)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_TABLESPACE)).isEmpty()
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_TABLESPACE)).isEmpty()
        assertThatAst(tree.getDescendants(DdlGrammar.DROP_COMMAND)).isEmpty()
        assertThat(p).notMatches("create tablespace set ts using template (segment space management manual);")
        assertThat(p).notMatches("alter tablespace set ts permanent;")
        assertThat(p).notMatches("alter tablespace set ts online drop datafile 'a';")
        assertThat(p).notMatches("create tablespace set ts drop quota;")
    }
}
