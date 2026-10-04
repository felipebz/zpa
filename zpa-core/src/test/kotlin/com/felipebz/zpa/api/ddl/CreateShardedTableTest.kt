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
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateShardedTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    private val columns = "(department_id number(6), department_name varchar2(30) constraint dept_name_nn not null, " +
        "constraint dept_id_pk primary key (department_id))"

    @Test
    fun matchesTheDocumentedStatement() {
        val source = """create sharded table departments
            (department_id number(6), department_name varchar2(30) constraint dept_name_nn not null,
             manager_id number(6), location_id number(4), constraint dept_id_pk primary key(department_id))
            partition by directory (department_id)
            (partition p_1 tablespace tbs1, partition p_2 tablespace tbs2);"""
        assertThat(p).matches(source)
    }

    @Test
    fun matchesDirectoryPartitionForms() {
        listOf(
            "partition by directory (a) (partition p1)", "partition by directory (a, b) (partition p1, partition p2)",
            "partition by directory (a) (partition p1 tablespace t1, partition p2 tablespace t2, partition p3)",
            "partition by directory (a) (partition p1 tablespace t1 pctfree 5 nologging)",
            "partition by directory (a) (partition p1 read only, partition p2 indexing off)",
            "partition by directory (a) (partition p1 compress, partition p2 inmemory)",
            "partition by directory (a) (partition tablespace t1)",
            "partition by directory (a) (partition p1) directory tablespace dt",
            "partition by directory (a) (partition p1 tablespace t1, partition p2) directory tablespace dt",
            "partition by directory (a) (partition p1) enable row movement", "partition by directory (a) (partition p1) parallel 2",
        ).forEach {
            val source = "create sharded table t (a number, b number) $it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "partition by directory a (partition p1)", "partition by directory () (partition p1)", "partition by directory (a,) (partition p1)",
            "partition by directory (a) partition p1", "partition by directory (a) ()", "partition by directory (a) (partition p1,)",
            "partition by directory (a) (partition p1 partition p2)", "partition by directory (a) (partition p1 values (1))",
            "partition by directory (a) (partition p1 values less than (1))", "partition by directory (a) (p1)",
            "partition by directory (a) (partition p1) directory tablespace", "partition by directory (a) (partition p1) directory dt",
            "partition by directory (a) directory tablespace dt", "partition by directory (a)", "partition by directory",
            "partition by directory (a) (partition p1) tablespace",
        ).forEach {
            val source = "create sharded table t (a number, b number) $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun placesShardedBeforeTableOnly() {
        listOf(
            "create sharded table t (a number)", "create sharded table t (a number) partition by directory (a) (partition p1)",
            "create sharded table s.t $columns partition by directory (department_id) (partition p1)",
            "create sharded table t (a number) partition by range (a) (partition p1 values less than (10))",
            "create sharded table t (a number) partition by hash (a) partitions 4",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create table sharded t (a number)", "create sharded t (a number)", "create sharded sharded table t (a number)",
            "create global temporary sharded table t (a number)", "create sharded global temporary table t (a number)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsDirectoryPartitioningOutOfOrdinaryTables() {
        listOf(
            "create table t (a number) partition by directory (a) (partition p1)",
            "create global temporary table t (a number) partition by directory (a) (partition p1)",
            "create private temporary table ora\$ptt_t (a number) partition by directory (a) (partition p1)",
            "create table t (a number) partition by directory (a) (partition p1 tablespace t1)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        listOf(
            "create table t (a number) partition by range (a) (partition p1 values less than (10), partition p2 values less than (maxvalue))",
            "create table t (a number) partition by list (a) (partition p1 values (1), partition p2 values (default))",
            "create table t (a number) partition by hash (a) partitions 4", "create table t (a number) partition by hash (a) (partition p1, partition p2)",
            "create table t (a number) partition by reference (fk)", "create table t (a number) tablespace ts",
            "create global temporary table t (a number) on commit delete rows",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun buildsDirectoryPartitioningStructure() {
        val node = p.parse(
            "create sharded table t $columns partition by directory (department_id, region_id) " +
                "(partition p_1 tablespace tbs1, partition p_2 read only, partition p_3) directory tablespace dts;")
        val directory = node.getFirstDescendant(DdlGrammar.PARTITION_BY_DIRECTORY)
        assertThatAst(directory.texts().take(8)).containsExactly("partition", "by", "directory", "(", "department_id", ",", "region_id", ")")
        assertThatAst(directory.texts().takeLast(3)).containsExactly("directory", "tablespace", "dts")
        assertThatAst(directory.getChildren(DdlGrammar.TABLE_PARTITION_DESCRIPTION).size).isEqualTo(3)
        assertThatAst(directory.getFirstDescendant(DdlGrammar.SEGMENT_ATTRIBUTES_CLAUSE).texts()).containsExactly("tablespace", "tbs1")
        assertThatAst(node.getDescendants(DdlGrammar.PARTITION_BY_RANGE)).isEmpty()
        assertThatAst(node.getDescendants(DdlGrammar.PARTITION_BY_LIST)).isEmpty()

        val range = p.parse("create table t (a number) partition by range (a) (partition p1 values less than (10))")
        assertThatAst(range.getDescendants(DdlGrammar.PARTITION_BY_DIRECTORY)).isEmpty()
        assertThatAst(range.getDescendants(DdlGrammar.PARTITION_BY_RANGE)).hasSize(1)
    }
}
