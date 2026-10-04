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

class CreateExternalTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    private val head = "create table t (a number) organization external"

    @Test
    fun matchesTheDocumentedStatements() {
        listOf(
            """create table json_file_contents (data json) organization external
                 (type oracle_bigdata access parameters (com.oracle.bigdata.fileformat = jsondoc)
                  location (order_entry_dir:'PurchaseOrders.json')) parallel reject limit unlimited;""",
            """create table dept_external (deptno number(6), dname varchar2(20), loc varchar2(25)) organization external
                 (type oracle_loader default directory admin access parameters
                   (records delimited by newline badfile 'ulcase1.bad' discardfile 'ulcase1.dis' logfile 'ulcase1.log' skip 20
                    fields terminated by "," optionally enclosed by '"'
                    (deptno integer external(6), dname char(20), loc char(25)))
                  location ('ulcase1.ctl')) reject limit unlimited;""",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesStructuralClausesInOrderWithEachOptional() {
        listOf(
            "(type oracle_loader default directory d access parameters (a b) location ('f'))", "(default directory d access parameters (a) location ('f'))",
            "(type oracle_loader)", "(type oracle_loader default directory d)", "(type oracle_loader location ('f'))",
            "(type oracle_loader access parameters (a))", "(location ('f'))", "(default directory d)", "(access parameters (a))", "()",
            "(type oracle_datapump default directory d location ('f'))", "(type \"ORACLE_LOADER\" location ('f'))",
            "(type a.b location ('f'))", "(type oracle_loader default directory d location (d:'f'))", "(location (d : 'f'))",
            "(location (d:'f','g'))", "(location (d:'f', e:'g'))", "(location (\"d\":'f'))", "(location (s.d:'f'))", "(location ('f', 'g'))",
            "(type oracle_loader default directory d access parameters using clob select 'x' from dual)",
            "(type oracle_loader default directory d access parameters using clob (select 'x' from dual))",
            "(type oracle_loader access parameters using clob ((select 'x' from dual)))",
            "(access parameters using clob select 'x' from dual union all select 'y' from dual)",
            "(access parameters using clob (select 'x' from dual union all select 'y' from dual))",
            "(access parameters using clob with q as (select 'x' c from dual) select c from q)",
            "(default directory d location ('f'))",
        ).forEach {
            val source = "$head $it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "(type oracle_loader location ('f') default directory d)", "(type oracle_loader access parameters (a) default directory d location ('f'))",
            "(default directory d type oracle_loader location ('f'))", "(access parameters (a) default directory d location ('f'))",
            "(type oracle_loader type oracle_loader)", "(default directory d default directory d)", "(location ('f') location ('f'))",
            "(access parameters (a) access parameters (b))", "(type 'x' location ('f'))", "(type 1 location ('f'))", "(type default directory d)",
            "(location ())", "(location 'f')", "(location)", "(location (f))", "(location ('f',))", "(location (,'f'))", "(location (d:))",
            "(location (1))", "(location ('f' 'g'))", "(location (d:'f' e:'g'))",
            "(access parameters)", "(access parameters x)", "(access parameters (a)", "(access parameters (a)) location ('f'))",
            "(access parameter (a))", "(access (a))", "(access parameters using clob)",
            "(access parameters using clob 'x')", "(access parameters using clob (a b))", "(access parameters using blob (select 'x' from dual))",
            "(access parameters using (select 'x' from dual))",
            "(access parameters using clob select 'x' from dual location ('f'))",
            "(access parameters using clob (select 'x' from dual) location ('f'))",
            "(default directory d access parameters using clob select 'x' x from dual location ('f'))",
            "", "type oracle_loader", "(type oracle_loader", "()) (",
        ).forEach {
            val source = "$head $it"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun matchesRejectLimitAndTrailingProperties() {
        listOf(
            " reject limit 5", " reject limit unlimited", " reject limit 0", " reject limit 1 reject limit 2",
            " reject limit 1 reject limit 2 reject limit 3", " parallel parallel", " parallel reject limit 1 parallel",
            " reject limit 1 parallel reject limit 2", " parallel 2 parallel 3", " parallel noparallel", " noparallel noparallel",
            " parallel reject limit 1 reject limit 2", " reject limit 1 parallel inmemory", " reject limit 1 inmemory parallel",
            " reject limit 1 inmemory", " inmemory reject limit 1", " parallel inmemory reject limit 1", " inmemory parallel reject limit 1",
            " inmemory reject limit 1 parallel", " inmemory reject limit 1 reject limit 2", " reject limit 1 inmemory reject limit 2",
            " inmemory reject limit 1 partition by range (a) (partition p1 values less than (10))",
            " parallel reject limit unlimited", " reject limit 5 parallel", " reject limit 5 parallel 4", " parallel 4 reject limit 5",
            " noparallel reject limit 2", " reject limit 2 inmemory", " partition by range (a) (partition p1 values less than (10))",
            " parallel",
        ).forEach {
            val source = "$head (type oracle_loader location ('f'))$it"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            " reject limit", " reject", " parallel reject", " reject limit 1 reject", " reject limit 'x'", " reject limit -1", " reject unlimited", " reject limit 1.5", " reject limit x",
        ).forEach {
            val source = "$head (type oracle_loader location ('f'))$it"
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).matches("create table t organization external (type oracle_loader location ('f')) reject limit 1 as select 1 a from dual")
        assertThat(p).matches("create table t (a number) segment creation immediate organization external (location ('f'))")
        listOf(
            "create table t (a number) reject limit 1", "create table t (a number) inmemory reject limit 1",
            "create table t (a number) organization heap reject limit 1", "create table t (a number primary key) organization index reject limit 1",
            "create table t (a number) parallel reject limit 1",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        val external = p.parse("$head (location ('f')) inmemory parallel reject limit 1")
        assertThatAst(external.getFirstDescendant(DdlGrammar.EXTERNAL_TABLE_CLAUSE).getDescendants(DdlGrammar.EXTERNAL_TABLE_REJECT_LIMIT)).hasSize(1)
        assertThat(p).notMatches("create table t (a number) organization external (location ('f')) organization heap")
        assertThat(p).notMatches("create table t (a number) organization heap organization external (location ('f'))")
        assertThat(p).notMatches("create table t (a number) partition by range (a) (partition p1 values less than (10)) organization external (location ('f'))")
    }

    @Test
    fun keepsAccessParametersOpaqueAndBalanced() {
        listOf(
            "access parameters ()", "access parameters (a (b (c)) 'x)' \"y)\")", "access parameters (using)", "access parameters ((((a))))",
            "access parameters (a = ')' b)",
            "access parameters (fields terminated by \",\" optionally enclosed by '\"' (c1 integer external(6), c2 char(20)))",
            "access parameters (com.oracle.bigdata.fileformat = jsondoc)", "access parameters (records delimited by newline skip 20)",
        ).forEach {
            val source = "$head (type oracle_loader default directory d $it location ('f'))"
            assertThat(p).describedAs(source).matches(source)
        }
        val node = p.parse(
            "$head (type oracle_loader access parameters (fields (a char(1), b char(2)) ')') location ('f')) reject limit 3")
        val clause = node.getFirstDescendant(DdlGrammar.EXTERNAL_TABLE_CLAUSE)
        val access = clause.getFirstChild(DdlGrammar.EXTERNAL_TABLE_ACCESS_PARAMETERS)
        assertThatAst(access.texts()).containsExactly(
            "access", "parameters", "(", "fields", "(", "a", "char", "(", "1", ")", ",", "b", "char", "(", "2", ")", ")", "')'", ")")
        val outer = access.getFirstChild(DdlGrammar.EXTERNAL_TABLE_OPAQUE_FORMAT_SPEC)
        assertThatAst(outer.getDescendants(DdlGrammar.EXTERNAL_TABLE_OPAQUE_FORMAT_SPEC)).hasSize(3)
        assertThatAst(clause.getFirstChild(DdlGrammar.EXTERNAL_TABLE_LOCATION).texts()).containsExactly("location", "(", "'f'", ")")
        assertThatAst(clause.getFirstChild(DdlGrammar.EXTERNAL_TABLE_REJECT_LIMIT).texts()).containsExactly("reject", "limit", "3")
        assertThatAst(clause.texts().take(5)).containsExactly("organization", "external", "(", "type", "oracle_loader")
    }

    @Test
    fun matchesHeapOrganizationAndKeepsIndexOrganizedTables() {
        listOf(
            "create table t (a number) organization heap", "create table t (a number) organization heap tablespace ts",
            "create table t (a number) organization heap pctfree 10", "create table t (a number) organization heap compress",
            "create table t (a number) organization heap nologging", "create table t (a number) organization heap parallel",
            "create table t (a number) segment creation immediate organization heap", "create table t (a number) organization heap inmemory",
            "create table t (a number) organization heap enable row movement",
            "create table t (a number) organization heap partition by hash (a) partitions 2",
            "create table t organization heap as select 1 a from dual", "create global temporary table t (a number) organization heap",
            "create table t (a number primary key) organization index", "create table t (a number primary key) organization index pctthreshold 10",
            "create table t (a number primary key) organization index including a overflow", "create table t (a number)",
            "create table t (a number) tablespace ts pctfree 10",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create table t (a number) organization heap organization heap", "create table t (a number) organization heap organization index",
            "create table t (a number) organization heap including a", "create table t (a number) organization heap overflow",
            "create table t (a number) organization heap mapping table", "create table t (a number) organization heap pctthreshold 10",
            "create table t (a number) organization index organization external (location ('f'))",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }

        assertThatAst(p.parse("create table t (a number) organization heap").getFirstDescendant(DdlGrammar.HEAP_ORGANIZED_TABLE_CLAUSE).texts())
            .containsExactly("organization", "heap")
        val index = p.parse("create table t (a number primary key) organization index pctthreshold 10")
        assertThatAst(index.getDescendants(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)).hasSize(1)
        assertThatAst(index.getDescendants(DdlGrammar.EXTERNAL_TABLE_CLAUSE)).isEmpty()
        assertThatAst(index.getDescendants(DdlGrammar.HEAP_ORGANIZED_TABLE_CLAUSE)).isEmpty()
        val plain = p.parse("create table t (a number)")
        assertThatAst(plain.getDescendants(DdlGrammar.HEAP_ORGANIZED_TABLE_CLAUSE)).isEmpty()
        assertThatAst(plain.getDescendants(DdlGrammar.EXTERNAL_TABLE_CLAUSE)).isEmpty()
    }

    @Test
    fun keepsLocationAsAnOrdinaryColumnName() {
        val source = "create table t (location varchar2(10), heap number, type varchar2(5), directory number)"
        assertThat(p).matches(source)
        assertThat(p).matches("create table t (location varchar2(10)) organization external (location ('f'))")
    }
}
