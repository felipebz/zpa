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

    private val customers = "create sharded table customers (custno number not null, name varchar2(50) not null, " +
        "signup date default null, class varchar2(3) not null, constraint cust_pk primary key(custno,name))"

    @Test
    fun matchesTheDocumentedPartitionsetStatement() {
        assertThat(p).matches(
            """CREATE SHARDED TABLE customers (
                custno         NUMBER NOT NULL,
                name           VARCHAR2(50) NOT NULL,
                signup         DATE DEFAULT NULL,
                class          VARCHAR2(3) NOT NULL,
            CONSTRAINT cust_pk PRIMARY KEY(custno,name))
            PARTITIONSET BY LIST (class)
            PARTITION BY CONSISTENT HASH (custno,name)
            PARTITIONS AUTO
            (PARTITIONSET gold VALUES ('gld') TABLESPACE SET tbs1,
             PARTITIONSET silver VALUES ('slv') TABLESPACE SET tbs2);""")
    }

    @Test
    fun matchesPartitionsetAndConsistentHashForms() {
        listOf(
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset gold values ('gld'))",
            "partitionset by list (class) partition by consistent hash (custno, name) partitions auto " +
                "(partitionset gold values ('gld', 'g2') tablespace set t1, partitionset other values (default) tablespace set t2)",
            "partitionset by range (class) partition by consistent hash (custno) partitions auto " +
                "(partitionset p1 values less than ('M') tablespace set t1, partitionset p2 values less than (maxvalue))",
            "partitionset by range (class, signup) partition by consistent hash (custno, name) partitions auto " +
                "(partitionset p1 values less than (10, 100) tablespace set t1, partitionset p2 values less than (20, 200))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto " +
                "(partitionset gold values ('gld') tablespace set t1 lob (signup) store as (cache))",
            "partition by consistent hash (custno, name) partitions auto tablespace set ts1",
            "partition by consistent hash (custno) tablespace set ts1", "partition by consistent hash (custno) partitions auto",
            "partition by consistent hash (custno)",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a')) enable row movement",
            // Oracle reports multi-column list partitionsets only after parsing (ORA-02514).
            "partitionset by list (class, class2) partition by consistent hash (custno, name) partitions auto " +
                "(partitionset silver values (('SLV', 1), ('BRZ', 2)) tablespace set ts1, " +
                "partitionset gold values (('GLD', 3), ('OTH', 4)) tablespace set ts2)",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values (('a')))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values ((null, 1)))",
        ).forEach {
            val source = "$customers $it"
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun rejectsMalformedPartitionsetAndConsistentHashForms() {
        listOf(
            "partitionset by list (class) partition by consistent hash (custno) (partitionset g values ('a'))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto ()",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a'),)",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto " +
                "(partitionset g values ('a') partitionset h values ('b'))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g)",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values 'a')",
            "partitionset by list (class,) partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list () partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (()))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', 1))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', 1)))) ",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a' 1)))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', 1) ('b', 2)))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', 1),))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', ), ('b', 2)))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto (partitionset g values (('a', (1)))))",
            "partitionset by list (class, class2) partition by consistent hash (custno) partitions auto " +
                "(partitionset g values (('a', 1)) partitionset h values (('b', 2)))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partition g values ('a'))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset values ('a'))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a') tablespace t1)",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a') tablespace set)",
            "partitionset by list (class) partition by hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list (class) partition by consistent (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list (class) partition by consistent hash () partitions auto (partitionset g values ('a'))",
            "partitionset by list (class) partition by consistent hash custno partitions auto (partitionset g values ('a'))",
            "partitionset by list class partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by listx (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset list (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list (class) partitions auto (partitionset g values ('a'))",
            "partitionset by range (class) partition by consistent hash (custno) partitions auto (partitionset g values ('a'))",
            "partitionset by list (class) partition by consistent hash (custno) partitions auto (partitionset g values less than (1))",
            "partition by consistent hash custno partitions auto", "partition by consistent hash () partitions auto",
            "partition by consistent (custno) partitions auto", "partition by consistent hash (custno) partitions manual",
            "partition by consistent hash (custno) partitions auto foo",
        ).forEach {
            val source = "$customers $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun keepsPartitionsetAndConsistentHashOutOfOrdinaryTables() {
        listOf(
            "create table t (a number, b varchar2(3)) partitionset by list (b) partition by consistent hash (a) partitions auto " +
                "(partitionset g values ('a') tablespace set t1)",
            "create table t (a number) partition by consistent hash (a) partitions auto tablespace set ts1",
            "create table t (a number) partition by consistent hash (a)",
            "create global temporary table t (a number) partition by consistent hash (a)",
            "create private temporary table ora\$ptt_t (a number) partition by consistent hash (a)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsPartitionsetAndConsistentHashStructure() {
        val partitionset = p.parse(
            "$customers partitionset by list (class) partition by consistent hash (custno, name) partitions auto " +
                "(partitionset gold values ('gld') tablespace set tbs1, partitionset silver values ('slv') tablespace set tbs2);")
        val list = partitionset.getFirstDescendant(DdlGrammar.PARTITIONSET_BY_LIST)!!
        assertThatAst(list.texts()).containsExactly(
            "partitionset", "by", "list", "(", "class", ")", "partition", "by", "consistent", "hash", "(", "custno", ",", "name", ")",
            "partitions", "auto", "(", "partitionset", "gold", "values", "(", "'gld'", ")", "tablespace", "set", "tbs1", ",",
            "partitionset", "silver", "values", "(", "'slv'", ")", "tablespace", "set", "tbs2", ")")
        assertThatAst(list.getChildren(DdlGrammar.LIST_VALUES_CLAUSE)).hasSize(2)
        assertThatAst(list.getChildren(DdlGrammar.PARTITIONSET_TUPLE_VALUES_CLAUSE)).isEmpty()

        val tuples = p.parse(
            "$customers partitionset by list (class, class2) partition by consistent hash (custno) partitions auto " +
                "(partitionset silver values (('SLV', 1), ('BRZ', 2)) tablespace set ts1);")
        val tupleList = tuples.getFirstDescendant(DdlGrammar.PARTITIONSET_BY_LIST)!!
        assertThatAst(tupleList.getChildren(DdlGrammar.LIST_VALUES_CLAUSE)).isEmpty()
        assertThatAst(tupleList.getFirstChild(DdlGrammar.PARTITIONSET_TUPLE_VALUES_CLAUSE).texts()).containsExactly(
            "values", "(", "(", "'SLV'", ",", "1", ")", ",", "(", "'BRZ'", ",", "2", ")", ")")
        assertThatAst(partitionset.getDescendants(DdlGrammar.PARTITION_BY_LIST)).isEmpty()
        assertThatAst(partitionset.getDescendants(DdlGrammar.PARTITION_BY_CONSISTENT_HASH)).isEmpty()

        val range = p.parse(
            "$customers partitionset by range (class) partition by consistent hash (custno) partitions auto " +
                "(partitionset p1 values less than (10));")
        assertThatAst(range.getDescendants(DdlGrammar.PARTITIONSET_BY_RANGE)).hasSize(1)
        assertThatAst(range.getFirstDescendant(DdlGrammar.PARTITIONSET_BY_RANGE)!!.getChildren(DdlGrammar.RANGE_VALUES_CLAUSE)).hasSize(1)

        val consistent = p.parse("$customers partition by consistent hash (custno) partitions auto tablespace set ts1;")
        assertThatAst(consistent.getFirstDescendant(DdlGrammar.PARTITION_BY_CONSISTENT_HASH)!!.texts()).containsExactly(
            "partition", "by", "consistent", "hash", "(", "custno", ")", "partitions", "auto", "tablespace", "set", "ts1")
        assertThatAst(consistent.getDescendants(DdlGrammar.PARTITIONSET_BY_LIST)).isEmpty()
    }
}
