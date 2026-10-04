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
package com.felipebz.zpa.lexer

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class GraphQlDualitySourceChannelTest {

    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))
    private val header = "CREATE JSON RELATIONAL DUALITY VIEW v AS"

    private fun tokens(source: String) = lexer.lex(source).dropLast(1)

    private fun graphQlOf(source: String) =
        tokens(source).singleOrNull { it.type == PlSqlTokenType.GRAPHQL_DUALITY_SOURCE }?.originalValue

    @Test
    fun keepsTheOracleFixtureInOneOpaqueToken() {
        val definition = "student { # this is a valid single line GraphQL comment\n" +
            "  _id: stuid\n  Name: name\n  -- SQL comments can be placed within GraphQL expression as well\n}"
        val source = "CREATE OR REPLACE JSON RELATIONAL DUALITY VIEW student_ov AS \n  $definition;"
        assertThat(tokens(source).map { it.type }).containsExactly(
            PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.JSON, PlSqlKeyword.RELATIONAL,
            PlSqlKeyword.DUALITY, PlSqlKeyword.VIEW, GenericTokenType.IDENTIFIER, PlSqlKeyword.AS,
            PlSqlTokenType.GRAPHQL_DUALITY_SOURCE, PlSqlPunctuator.SEMICOLON)
        val token = tokens(source)[9]
        assertThat(token.originalValue).isEqualTo(definition)
        assertThat(token.line).isEqualTo(2)
        assertThat(token.column).isEqualTo(2)
    }

    @Test
    fun endsAtTheBraceClosingTheRootSelectionSet() {
        val definitions = listOf(
            "t { _id: id name }",
            "t {\n  idAlias: id\n  child { _id: id\n    grand { x } }\n}",
            "t { child [ { _id: id } ] }",
            "t @insert @update @delete { _id: id }",
            "t(check:{id:{_gt:1}}) @insert { _id: id }",
            "t { _id: id name(check:{name:{_eq:\"}\"}}) }",
            "t { \"na}me\": name _id: id }",
            "t { _id: id\n  \"\"\" has } and { inside; \"\"\"\n  name }",
            "t { _id: id # } ; {\n  name }",
            "t { _id: id -- } ; {\n  name }",
            "t { _id: id /* } ; { */ name }",
            "\"t\" { _id: id }",
            "t { é: id, ñ }",
        )
        definitions.forEach { definition ->
            val source = "$header $definition;\nselect 1 from dual;"
            assertThat(graphQlOf(source)).describedAs(definition).isEqualTo(definition)
            assertThat(tokens(source).map { it.type }.takeLast(6)).describedAs(definition).containsExactly(
                PlSqlPunctuator.SEMICOLON, PlSqlKeyword.SELECT, PlSqlTokenType.INTEGER_LITERAL, PlSqlKeyword.FROM,
                GenericTokenType.IDENTIFIER, PlSqlPunctuator.SEMICOLON)
        }
    }

    @Test
    fun doesNotLeakGraphQlPunctuationOrComments() {
        val source = "$header t(check:{id:{_gt:1}}) @insert { _id: id # c\n  list [ { x: y } ] -- d\n};"
        val types = tokens(source).map { it.type }
        assertThat(types).doesNotContain(
            PlSqlPunctuator.COLON, PlSqlPunctuator.LBRACKET, PlSqlPunctuator.LPARENTHESIS, PlSqlPunctuator.HASH)
        assertThat(types).containsOnlyOnce(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE)
    }

    @Test
    fun includesHashCommentsThatFollowTheRootSelectionSet() {
        assertThat(graphQlOf("$header t { _id: id } # a ;\n -- b\n # c\n")).isEqualTo("t { _id: id } # a ;\n -- b\n # c")
        assertThat(graphQlOf("$header t { _id: id } -- a\n # b\n;")).isEqualTo("t { _id: id } -- a\n # b")
        assertThat(graphQlOf("$header t { _id: id } -- a\n;")).isEqualTo("t { _id: id }")
        assertThat(graphQlOf("$header t { _id: id } /* a */ ;")).isEqualTo("t { _id: id }")
        val trailing = lexer.lex("$header t { _id: id } -- a\n;")
        assertThat(trailing.first { it.type == PlSqlPunctuator.SEMICOLON }.hasTrivia()).isTrue()
    }

    @Test
    fun leavesCommentsBeforeTheRootToTheSqlLexer() {
        val source = "$header -- leading\n  /* c */ t { _id: id };"
        assertThat(graphQlOf(source)).isEqualTo("t { _id: id }")
        val hash = tokens("$header # leading\n t { _id: id };").map { it.type }
        assertThat(hash).doesNotContain(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE)
        assertThat(hash).contains(PlSqlPunctuator.HASH)
    }

    @Test
    fun admitsOnlyAfterADualityViewHeader() {
        listOf(
            "CREATE JSON DUALITY VIEW v AS t { _id: id }",
            "create or replace force editionable json relational duality view s.v as t { _id: id }",
            "CREATE NONEDITIONABLE JSON RELATIONAL DUALITY VIEW \"v\" AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW IF NOT EXISTS v AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v ENABLE LOGICAL REPLICATION AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (a) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW s.v (\"a\", b) DISABLE LOGICAL REPLICATION AS t { _id: id }",
            "CREATE JSON DUALITY VIEW IF NOT EXISTS v (a, b, c) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS s.t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS \"S\" . \"t\" @insert(x: 1) { _id: id }",
            "CREATE OR REPLACE JSON DUALITY VIEW v DISABLE LOGICAL REPLICATION AS t { _id: id }",
        ).forEach { assertThat(graphQlOf(it)).describedAs(it).endsWith("{ _id: id }") }
    }

    @Test
    fun rejectsEverythingOutsideTheDefinitionBoundary() {
        listOf(
            "CREATE VIEW v AS t { _id: id }",
            "CREATE JSON COLLECTION VIEW v AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS SELECT JSON {'_id': t.id} FROM t WITH INSERT",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS (t { _id: id })",
            "CREATE NO FORCE JSON RELATIONAL DUALITY VIEW v AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v () AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (a,) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (a b) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (a) (b) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (1) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v (a.b) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v DISABLE LOGICAL REPLICATION (a) AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS a.b.t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS s.t.x { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS al: t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS s. { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW a.b.v AS t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t { _id: id",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t; { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t { _id: \"id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS enable logical replication t { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t u { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t @ { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t(check: { { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS t { /* }",
            "CREATE JSON RELATIONAL DUALITY VIEW v AS 1 { _id: id }",
            "CREATE JSON RELATIONAL DUALITY VIEW AS t { _id: id }",
            "SELECT 1 AS t FROM dual WHERE x = { }",
        ).forEach { assertThat(graphQlOf(it)).describedAs(it).isNull() }
    }

    @Test
    fun keepsHashInvalidOutsideTheDefinition() {
        assertThat(tokens("#").map { it.type }).containsExactly(PlSqlPunctuator.HASH)
        assertThat(tokens("select '# not a comment' from dual;").map { it.type }).containsExactly(
            PlSqlKeyword.SELECT, PlSqlTokenType.STRING_LITERAL, PlSqlKeyword.FROM, GenericTokenType.IDENTIFIER, PlSqlPunctuator.SEMICOLON)
        assertThat(tokens("select student from t;").map { it.type }).doesNotContain(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE)
        assertThat(tokens("create view v as select student { } from t;").map { it.type })
            .doesNotContain(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE)
        assertThat(tokens("select 1 from dual # c\n;").map { it.type }).contains(PlSqlPunctuator.HASH)
    }
}
