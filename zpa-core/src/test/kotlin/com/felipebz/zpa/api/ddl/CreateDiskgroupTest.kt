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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateDiskgroupTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_DISKGROUP)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    @Test
    fun matchesTheDocumentedStatement() {
        assertThat(p).matches("create diskgroup dgroup_01 external redundancy disk '/devices/disks/c*';")
    }

    @Test
    fun matchesRedundancyAndDiskGroups() {
        listOf(
            "disk '/a'", "external redundancy disk '/a'", "normal redundancy disk '/a'", "high redundancy disk '/a'",
            "flex redundancy disk '/a'", "FLEX REDUNDANCY disk '/a'", "extended redundancy disk '/a'",
            "normal redundancy quorum disk '/a'", "normal redundancy regular disk '/a'", "normal redundancy failgroup f1 disk '/a'",
            "normal redundancy quorum failgroup f1 disk '/a'",
            "normal redundancy regular failgroup f1 disk '/a' failgroup f2 disk '/b'",
            "normal redundancy failgroup f1 disk '/a', '/b' failgroup f2 disk '/c'",
            "normal redundancy failgroup f1 disk '/a' quorum failgroup f3 disk '/q'",
            "normal redundancy disk '/a' disk '/b'", "normal redundancy disk '/a' regular disk '/b'", "normal redundancy disk '/a', '/b'",
            "extended redundancy site s1 failgroup f1 disk '/a' site s2 failgroup f2 disk '/b'",
            "extended redundancy site s1 quorum failgroup f1 disk '/a'", "extended redundancy site s1 disk '/a' site s2 disk '/b'",
            "site s1 disk '/a'", "external redundancy site s1 disk '/a'",
        ).forEach {
            val source = "create diskgroup dg $it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "redundancy disk '/a'", "bogus redundancy disk '/a'", "external disk '/a'", "external redundancy external redundancy disk '/a'",
            "normal redundancy high redundancy disk '/a'", "normal redundancy disk '/a' external redundancy", "normal site s1 redundancy disk '/a'",
            "extended site s1 redundancy disk '/a'", "normal redundancy failgroup f1 quorum disk '/a'",
            "normal redundancy failgroup f1 failgroup f2 disk '/a'", "normal redundancy quorum regular disk '/a'",
            "normal redundancy quorum quorum disk '/a'", "normal redundancy failgroup disk '/a'", "normal redundancy failgroup f1, f2 disk '/a'",
            "extended redundancy quorum site s1 failgroup f1 disk '/a'", "extended redundancy site s1 failgroup f1 failgroup f2 disk '/a'",
            "extended redundancy site 's1' disk '/a'", "extended redundancy site disk '/a'", "site s1 site s2 disk '/a'",
            "external redundancy", "", "attribute 'a' = '1'", "external redundancy attribute 'a' = '1' disk '/a'",
            "external redundancy disk", "external redundancy disk '/a' disk", "external redundancy disk '/a',",
        ).forEach {
            val source = "create diskgroup dg $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).matches("create diskgroup \"dg\" external redundancy disk '/a'")
        listOf(
            "create diskgroup external redundancy disk '/a'", "create diskgroup s.dg external redundancy disk '/a'",
            "create diskgroup if not exists dg external redundancy disk '/a'", "create or replace diskgroup dg disk '/a'",
            "create diskgroup",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesDiskOptionsAndAttributes() {
        listOf(
            "disk '/a' name n1", "disk '/a' name n1 size 10g", "disk '/a' size 10g force", "disk '/a' noforce", "disk '/a' size 10",
            "disk '/a' name n1, '/b' size 1g force, '/c'", "disk '/a', '/b' name n force",
            "disk '/a' attribute 'compatible.asm' = '19.0'", "disk '/a' attribute 'a' = '1', 'b' = '2'",
            "disk '/a' size 10g force attribute 'a' = '1'", "failgroup f1 disk '/a' attribute 'a' = '1';",
        ).forEach {
            val source = "create diskgroup dg external redundancy $it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "disk '/a' size 10g name n1", "disk '/a' force noforce", "disk '/a' force force", "disk '/a' name n1 name n2",
            "disk '/a' force size 10g", "disk '/a' force name n1", "disk '/a' name 'n1'", "disk /a", "disk a", "disk '/a' name",
            "disk '/a' size", "disk '/a' size 1g size 2g",
            "disk '/a' attributes 'a' = '1'", "disk '/a' attribute a = '1'", "disk '/a' attribute 'a' = 1", "disk '/a' attribute 'a' '1'",
            "disk '/a' attribute", "disk '/a' attribute 'a' = '1' 'b' = '2'", "disk '/a' attribute 'a' = '1' attribute 'b' = '2'",
            "disk '/a' attribute 'a' = '1', disk '/b'", "disk '/a' attribute 'a' = '1' failgroup f disk '/b'",
            "disk '/a' attribute 'a' = '1',",
        ).forEach {
            val source = "create diskgroup dg external redundancy $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun buildsStructureAndReusesTheAlterDiskClause() {
        val node = p.parse(
            "create diskgroup dg high redundancy failgroup f1 disk '/a' name n1 size 1g force, '/b' noforce " +
                "quorum failgroup f2 disk '/c' attribute 'a' = '1', 'b' = '2';")
        assertThatAst(node.texts().take(5)).containsExactly("create", "diskgroup", "dg", "high", "redundancy")
        val disks = node.getChildren(DdlGrammar.QUALIFIED_DISK_CLAUSE)
        assertThatAst(disks.map { it.texts() }).containsExactly(
            listOf("'/a'", "name", "n1", "size", "1", "g", "force"), listOf("'/b'", "noforce"), listOf("'/c'"))
        assertThatAst(node.texts().takeLast(9)).containsExactly(
            "attribute", "'a'", "=", "'1'", ",", "'b'", "=", "'2'", ";")
    }

    @Test
    fun keepsAlterDiskgroupAndOtherCreateStatementsUnchanged() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val create = p.parse("create diskgroup dg external redundancy disk '/a';")
        assertThatAst(create.getDescendants(DdlGrammar.CREATE_DISKGROUP)).hasSize(1)
        assertThatAst(create.getDescendants(DdlGrammar.ALTER_DISKGROUP)).isEmpty()
        val alter = p.parse("alter diskgroup dg add failgroup f1 disk '/a' name n1;")
        assertThatAst(alter.getDescendants(DdlGrammar.ALTER_DISKGROUP)).hasSize(1)
        assertThatAst(alter.getDescendants(DdlGrammar.CREATE_DISKGROUP)).isEmpty()
        listOf(
            "create database link l connect to u identified by p using 'x';", "create table dg (a number);",
            "create user dg identified by pw;", "create directory dg as '/tmp';",
        ).forEach {
            assertThat(p).describedAs(it).matches(it)
            assertThatAst(p.parse(it).getDescendants(DdlGrammar.CREATE_DISKGROUP)).describedAs(it).isEmpty()
        }
    }
}
