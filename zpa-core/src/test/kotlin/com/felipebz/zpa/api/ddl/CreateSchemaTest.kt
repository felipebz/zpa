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
import com.felipebz.zpa.api.DclGrammar
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateSchemaTest : RuleTest() {

    private val head = "create schema authorization u"
    private val table = "create table t (id number)"
    private val table2 = "create table t2 (id number)"
    private val view = "create view v as select id from t"
    private val view2 = "create view v2 as select 1 x from dual"
    private val grant = "grant select on v to other"

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_SCHEMA)
    }

    @Test
    fun matchesTheExactOracleFixture() {
        assertThat(p).matches(
            """
            CREATE SCHEMA AUTHORIZATION oe
               CREATE TABLE new_product
                  (color VARCHAR2(10) PRIMARY KEY, quantity NUMBER)
               CREATE VIEW new_product_view
                  AS SELECT color, quantity FROM new_product WHERE color = 'RED'
               GRANT select ON new_product_view TO hr;
            """.trimIndent())
    }

    @Test
    fun acceptsAnyNumberOfElements() {
        assertThat(p).matches(head)
        assertThat(p).matches("$head;")
        assertThat(p).matches("$head $table")
        assertThat(p).matches("$head $view")
        assertThat(p).matches("$head $grant")
        assertThat(p).matches("$head $table $table2 $view $view2 $grant $grant;")
    }

    @Test
    fun acceptsEveryElementTransition() {
        val elements = listOf(table, view, grant)
        for (first in elements) for (second in elements) {
            assertThat(p).describedAs("$first -> $second").matches("$head $first $second")
            assertThat(p).describedAs("$first -> $second;").matches("$head $first $second;")
        }
    }

    @Test
    fun acceptsTheSharedElementSyntaxOracleAccepts() {
        listOf(
            "$head create table t (id number) tablespace users",
            "$head create table t (id number) parallel 2 create table t2 (id number) noparallel",
            "$head create table t (id number) parallel 4 $view",
            "$head create table t (id number primary key, n varchar2(10) not null) compress logging",
            "$head create table t as select 1 id from dual $grant",
            "$head create table t (id number) partition by hash (id) partitions 2 $view",
            "$head create view v (a) as select id from t with read only create view v2 as select id from t with check option $grant",
            "$head create view v as select id from t a where id = 1 order by id $grant",
            "$head create view v as with q as (select 1 id from dual) select id from q $grant",
            "$head create view v as select id from t where id in (select id from t) $grant",
            "$head create view v bequeath definer as select 1 x from dual $table",
            "$head grant select, update (id) on t to a, b with grant option $table",
            "$head grant all on u.t to public $table",
            "$head grant r to public $table",
            "$head grant grant any privilege to other",
            "$head grant grant any privilege to other grant select on t to other $table",
            "$head grant select any table to other grant alter any table to other grant drop any table to other",
            "$head $table grant grant any object privilege to other $view",
            "create or replace schema authorization u $table",
            "CREATE SCHEMA AUTHORIZATION \"U\" $table",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun keepsSelectAndGrantBoundariesAtTheNextElement() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("$head create view v as select 1 x from dual $grant $table;")
        val schema = tree.getFirstDescendant(DdlGrammar.CREATE_SCHEMA)
        assertThatAst(schema.children.map { it.type }).containsSubsequence(
            PlSqlGrammar.CREATE_VIEW, DclGrammar.GRANT_STATEMENT, DdlGrammar.CREATE_TABLE)
        assertThatAst(schema.getFirstChild(PlSqlGrammar.CREATE_VIEW).tokens.map { it.originalValue })
            .containsExactly("create", "view", "v", "as", "select", "1", "x", "from", "dual")
        assertThatAst(schema.getFirstChild(DclGrammar.GRANT_STATEMENT).tokens.map { it.originalValue })
            .containsExactly("grant", "select", "on", "v", "to", "other")
    }

    @Test
    fun rejectsElementsOracleDoesNotAllow() {
        listOf(
            "$head grant create session to other",
            "$head grant create any table to other",
            "$head grant select on t to other grant create session to other",
            "$head grant select, create session to other",
            "$head create global temporary table t (id number)",
            "$head create private temporary table ora\$ptt_t (id number)",
            "$head create immutable table t (id number) no drop no delete",
            "$head create json collection table t",
            "$head create or replace view v as select 1 x from dual",
            "$head create force view v as select 1 x from dual",
            "$head create no force view v as select 1 x from dual",
            "$head create editionable view v as select 1 x from dual",
            "$head create index i on t (id)",
            "$head create sequence s",
            "$head create synonym s for t",
            "$head create materialized view mv as select 1 x from dual",
            "$head $table create index i on t (id)",
            "$head alter table t add x number",
            "$head insert into t values (1)",
            "$head drop table t",
            "$head comment on table t is 'x'",
            "$head revoke select on t from other",
            "$head select 1 from dual",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun rejectsMalformedHeaders() {
        listOf(
            "create schema authorization",
            "create schema authorization $table",
            "create schema u $table",
            "create schema t authorization u",
            "create schema authorization s.u $table",
            "create schema authorization u, v $table",
            "create schema authorization u authorization u",
            "create schema authorization u@l $table",
            "create schema authorization 1 $table",
            "create schema if not exists authorization u $table",
            "create or replace schema u $table",
            "create schema",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun doesNotConsumeSemicolonsBetweenElements() {
        assertThat(p).notMatches("$head $table; $view")
        assertThat(p).notMatches("$head $view; $grant")
        assertThat(p).notMatches("$head $grant; $table")
        assertThat(p).notMatches("$head $table; $table2;")

        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("$head $table; $view;")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_SCHEMA)).hasSize(1)
        assertThatAst(tree.getFirstDescendant(DdlGrammar.CREATE_SCHEMA).getDescendants(PlSqlGrammar.CREATE_VIEW)).isEmpty()
        assertThatAst(tree.getDescendants(PlSqlGrammar.CREATE_VIEW)).hasSize(1)
    }

    @Test
    fun finalSemicolonBelongsToCreateSchema() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val schema = p.parse("$head $table $view $grant;").getFirstDescendant(DdlGrammar.CREATE_SCHEMA)
        assertThatAst(schema.lastChild.type).isEqualTo(PlSqlPunctuator.SEMICOLON)
        listOf(DdlGrammar.CREATE_TABLE, PlSqlGrammar.CREATE_VIEW, DclGrammar.GRANT_STATEMENT).forEach {
            assertThatAst(schema.getFirstChild(it).getDescendants(PlSqlPunctuator.SEMICOLON)).describedAs("$it").isEmpty()
            assertThatAst(schema.getFirstChild(it).lastChild.type).describedAs("$it").isNotEqualTo(PlSqlPunctuator.SEMICOLON)
        }
    }

    @Test
    fun keepsEmbeddedStatementsAsOrdinaryNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val schema = p.parse("$head $table $view $grant;").getFirstDescendant(DdlGrammar.CREATE_SCHEMA)
        assertThatAst(schema.children.map { it.type }).containsExactly(
            com.felipebz.zpa.api.PlSqlKeyword.CREATE, com.felipebz.zpa.api.PlSqlKeyword.SCHEMA,
            com.felipebz.zpa.api.PlSqlKeyword.AUTHORIZATION, PlSqlGrammar.IDENTIFIER_NAME,
            DdlGrammar.CREATE_TABLE, PlSqlGrammar.CREATE_VIEW, DclGrammar.GRANT_STATEMENT, PlSqlPunctuator.SEMICOLON)
        assertThatAst(schema.getFirstChild(PlSqlGrammar.CREATE_VIEW).getFirstChild(DmlGrammar.SELECT_EXPRESSION)).isNotNull
    }

    @Test
    fun keepsTopLevelTerminatorsOutsideCreateSchema() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create table t (id number); create view v as select 1 x from dual; grant select on v to other;")
        assertThatAst(tree.getFirstDescendant(DdlGrammar.CREATE_TABLE).lastChild.type).isEqualTo(PlSqlPunctuator.SEMICOLON)
        assertThatAst(tree.getFirstDescendant(PlSqlGrammar.CREATE_VIEW).lastChild.type).isEqualTo(PlSqlPunctuator.SEMICOLON)
        assertThatAst(tree.getFirstDescendant(DclGrammar.GRANT_STATEMENT).lastChild.type).isEqualTo(PlSqlPunctuator.SEMICOLON)
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_SCHEMA)).isEmpty()
        assertThat(p).matches("grant create session to other;")
        assertThat(p).matches("create or replace view v as select 1 x from dual;")
    }

    @Test
    fun keepsSchemaAndAuthorizationUsableAsNames() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("select schema, authorization from t;")
        assertThat(p).matches("create table authorization (schema number);")
    }
}
