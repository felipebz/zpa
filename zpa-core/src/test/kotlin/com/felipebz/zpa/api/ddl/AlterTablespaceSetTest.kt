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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class AlterTablespaceSetTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE_SET)
    }

    @Test
    fun matchesDefaultParameters() {
        for (attribute in listOf(
            "default table nocompress",
            "default compress for oltp",
            "default table compress for query high index compress advanced low storage (initial 1m next 2m)",
            "default index nocompress",
            "default storage (initial 1m)",
            "default inmemory memcompress for query high priority low distribute by rowid range duplicate all",
            "default no inmemory",
            "default table compress for archive low index nocompress inmemory ilm enable_all storage (initial 1m)",
            "default ilm add policy row store compress advanced segment after 30 days of no modification",
            "default ilm delete policy p",
            "default ilm enable_all"
        )) {
            assertThat(p).describedAs(attribute).matches("alter tablespace set ts $attribute;")
        }
    }

    @Test
    fun matchesSpaceRenameAndBackupAttributes() {
        for (attribute in listOf(
            "resize 10m",
            "coalesce",
            "rename to ts2",
            "rename to \"Shard Space\"",
            "begin backup",
            "end backup",
            "rename datafile 'a.dbf' to 'b.dbf'",
            "rename datafile 'a.dbf', 'b.dbf' to 'c.dbf', 'd.dbf'",
            "datafile online",
            "datafile offline"
        )) {
            assertThat(p).describedAs(attribute).matches("alter tablespace set ts $attribute")
        }
    }

    @Test
    fun matchesLoggingStateAndAutoextension() {
        for (attribute in listOf(
            "logging",
            "nologging",
            "force logging",
            "no force logging",
            "online",
            "offline",
            "offline normal",
            "offline temporary",
            "offline immediate",
            "offline for recover",
            "read only",
            "read write",
            "enable lost write protection",
            "remove lost write protection",
            "suspend lost write protection",
            "autoextend off",
            "autoextend on",
            "autoextend on next 1m",
            "autoextend on maxsize 2g",
            "autoextend on next 1m maxsize unlimited"
        )) {
            assertThat(p).describedAs(attribute).matches("alter tablespace set ts $attribute;")
        }
    }

    @Test
    fun matchesEncryptionOperations() {
        for (attribute in listOf(
            "encryption online encrypt",
            "encryption online using 'AES256' mode 'XTS' encrypt file_name_convert = ('old', 'new') keep",
            "encryption online using 'AES256' rekey file_name_convert = ('a', 'b', 'c', 'd')",
            "encryption online decrypt file_name_convert = ('old', 'new')",
            "encryption finish encrypt file_name_convert = ('old', 'new') keep",
            "encryption finish rekey",
            "encryption finish decrypt",
            "encryption offline encrypt",
            "encryption offline using 'AES128' encrypt",
            "encryption offline decrypt",
            "encryption encrypt",
            "encryption using 'AES256' mode 'XTS' encrypt",
            "encryption decrypt"
        )) {
            assertThat(p).describedAs(attribute).matches("alter tablespace set ts $attribute;")
        }
    }

    @Test
    fun rejectsOrdinaryOnlyAttributes() {
        for (attribute in listOf(
            "minimum extent 64k",
            "shrink space",
            "shrink space keep 1m",
            "shrink tempfile 't.dbf' keep 1m",
            "tablespace group g",
            "flashback off",
            "retention guarantee",
            "add datafile 'a.dbf' size 1m",
            "add tempfile 't.dbf'",
            "drop datafile 'a.dbf'",
            "drop tempfile 5",
            "tempfile online",
            "tempfile offline",
            "permanent",
            "temporary",
            "lost write protection",
            "enable lost write",
            "remove lost protection"
        )) {
            assertThat(p).describedAs(attribute).notMatches("alter tablespace set ts $attribute")
        }
    }

    @Test
    fun rejectsMissingExtraAndMalformedAttributes() {
        for (statement in listOf(
            "alter tablespace set",
            "alter tablespace set ts",
            "alter tablespace set if exists ts online",
            "alter tablespace set ts if exists online",
            "alter tablespace set ts online nologging",
            "alter tablespace set ts begin backup end backup",
            "alter tablespace set ts rename to ts2 resize 1m",
            "alter tablespace set ts default",
            "alter tablespace set ts default table",
            "alter tablespace set ts default storage (initial)",
            "alter tablespace set ts default storage (initial 1m) table nocompress",
            "alter tablespace set ts default inmemory distribute by partition",
            "alter tablespace set ts default inmemory distribute by subpartition",
            "alter tablespace set ts default inmemory memcompress for",
            "alter tablespace set ts default ilm add policy",
            "alter tablespace set ts default ilm delete policy",
            "alter tablespace set ts resize",
            "alter tablespace set ts rename",
            "alter tablespace set ts rename to",
            "alter tablespace set ts rename datafile 'a.dbf'",
            "alter tablespace set ts rename datafile 1 to 2",
            "alter tablespace set ts rename datafile ('a.dbf') to 'b.dbf'",
            "alter tablespace set ts rename datafile 'a.dbf', to 'b.dbf'",
            "alter tablespace set ts rename datafile 'a.dbf' to 'b.dbf',",
            "alter tablespace set ts datafile 'a.dbf' online",
            "alter tablespace set ts offline normal immediate",
            "alter tablespace set ts offline for",
            "alter tablespace set ts offline for recover normal",
            "alter tablespace set ts autoextend",
            "alter tablespace set ts autoextend off next 1m",
            "alter tablespace set ts autoextend on next",
            "alter tablespace set ts autoextend on maxsize",
            "alter tablespace set ts autoextend on maxsize 2g next 1m",
            "alter tablespace set ts encryption",
            "alter tablespace set ts encryption online",
            "alter tablespace set ts encryption online using 'AES256'",
            "alter tablespace set ts encryption online mode 'XTS' encrypt",
            "alter tablespace set ts encryption online using AES256 encrypt",
            "alter tablespace set ts encryption online using 'AES256' mode encrypt",
            "alter tablespace set ts encryption offline rekey",
            "alter tablespace set ts encryption offline encrypt file_name_convert = ('a', 'b')",
            "alter tablespace set ts encryption finish using 'AES256' rekey",
            "alter tablespace set ts encryption finish encrypt file_name_convert ('a', 'b')",
            "alter tablespace set ts encryption finish encrypt file_name_convert = ()",
            "alter tablespace set ts encryption finish encrypt file_name_convert = ('a')",
            "alter tablespace set ts encryption finish encrypt file_name_convert = ('a', 'b', 'c')",
            "alter tablespace set ts encryption finish encrypt keep"
        )) {
            assertThat(p).describedAs(statement).notMatches(statement)
        }
    }

    @Test
    fun acceptsShardspaceAsAnIdentifier() {
        assertThat(p).matches("alter tablespace set shardspace rename to shardspace;")
        assertThat(p).matches("alter tablespace set \"shardspace\" read only")
        assertThat(p).matches("alter tablespace set recover offline for recover")
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        assertThat(p).matches("alter tablespace shardspace online")
    }

    @Test
    fun retainsOrdinaryOnlyAttributeBranches() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        for (attribute in listOf(
            "minimum extent 64k",
            "tempfile online",
            "tempfile offline",
            "temporary",
            "offline for recover"
        )) {
            assertThat(p).describedAs(attribute).matches("alter tablespace ts $attribute;")
        }
    }

    @Test
    fun sharesDefaultOnlineEncryptionAndRejectsSpecsForDecryption() {
        for (rule in listOf(DdlGrammar.ALTER_TABLESPACE, DdlGrammar.ALTER_TABLESPACE_SET)) {
            setRootRule(rule)
            val prefix = if (rule == DdlGrammar.ALTER_TABLESPACE_SET) "alter tablespace set ts" else "alter tablespace ts"
            for (attribute in listOf(
                "encryption encrypt file_name_convert = ('old', 'new') keep",
                "encryption rekey",
                "encryption using 'AES256' rekey file_name_convert = ('old', 'new')",
                "encryption decrypt file_name_convert = ('old', 'new')"
            )) {
                assertThat(p).describedAs("$rule: $attribute").matches("$prefix $attribute;")
            }
            for (attribute in listOf(
                "encryption using 'AES256' decrypt",
                "encryption offline using 'AES256' decrypt",
                "encryption online using 'AES256' decrypt"
            )) {
                assertThat(p).describedAs("$rule: $attribute").notMatches("$prefix $attribute;")
            }
        }
    }

    @Test
    fun preservesOrdinaryEncryptionAstBoundaries() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        val node = p.parse("alter tablespace ts encryption online decrypt;")
        assertThatAst(node.children.map { it.type }).containsExactly(
            PlSqlKeyword.ALTER,
            PlSqlKeyword.TABLESPACE,
            PlSqlGrammar.IDENTIFIER_NAME,
            PlSqlKeyword.ENCRYPTION,
            PlSqlKeyword.ONLINE,
            PlSqlKeyword.DECRYPT,
            PlSqlPunctuator.SEMICOLON
        )
    }
}
