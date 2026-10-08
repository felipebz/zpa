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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateJsonRelationalDualityViewTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    @Test
    fun matchesTheExactOracleFixture() {
        val source = """
              CREATE OR REPLACE JSON RELATIONAL DUALITY VIEW student_ov AS 
                student { # this is a valid single line GraphQL comment
                _id: stuid
                Name: name
                -- SQL comments can be placed within GraphQL expression as well
            };
            """.trimIndent()
        assertThat(p).matches(source)
        val view = p.parse(source).getFirstDescendant(DdlGrammar.CREATE_JSON_RELATIONAL_DUALITY_VIEW)
        assertThatAst(view.children.map { it.type }).containsExactly(
            PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.JSON, PlSqlKeyword.RELATIONAL,
            PlSqlKeyword.DUALITY, PlSqlKeyword.VIEW, PlSqlGrammar.UNIT_NAME, PlSqlKeyword.AS,
            PlSqlTokenType.GRAPHQL_DUALITY_SOURCE, PlSqlPunctuator.SEMICOLON)
        assertThatAst(view.getFirstChild(PlSqlGrammar.UNIT_NAME).tokenOriginalValue).isEqualTo("student_ov")
        assertThatAst(view.getFirstChild(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE).tokenOriginalValue)
            .startsWith("student {").contains("# this is a valid single line GraphQL comment").endsWith("as well\n}")
    }

    @Test
    fun exposesTheColumnListTokensInTheOuterStatement() {
        val view = p.parse("create json duality view v (a, \"b\") disable logical replication as s.t { _id: id };")
            .getFirstDescendant(DdlGrammar.CREATE_JSON_RELATIONAL_DUALITY_VIEW)
        assertThatAst(view.children.map { it.type }).containsExactly(
            PlSqlKeyword.CREATE, PlSqlKeyword.JSON, PlSqlKeyword.DUALITY, PlSqlKeyword.VIEW, PlSqlGrammar.UNIT_NAME,
            PlSqlPunctuator.LPARENTHESIS, PlSqlGrammar.IDENTIFIER_NAME, PlSqlPunctuator.COMMA, PlSqlGrammar.IDENTIFIER_NAME,
            PlSqlPunctuator.RPARENTHESIS, PlSqlKeyword.DISABLE, PlSqlKeyword.LOGICAL, PlSqlKeyword.REPLICATION, PlSqlKeyword.AS,
            PlSqlTokenType.GRAPHQL_DUALITY_SOURCE, PlSqlPunctuator.SEMICOLON)
        assertThatAst(view.getFirstChild(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE).tokenOriginalValue).isEqualTo("s.t { _id: id }")
    }

    @Test
    fun matchesTheHeaderFormsOracleAccepts() {
        listOf(
            "create json relational duality view v as t { _id: id }",
            "create json duality view v as t { _id: id };",
            "create or replace json relational duality view v as t { _id: id }",
            "create force json relational duality view v as t { _id: id }",
            "create or replace force json relational duality view v as t { _id: id }",
            "create editionable json relational duality view v as t { _id: id }",
            "create or replace force nonEditionable json duality view s.\"v\" as t { _id: id }",
            "create json relational duality view if not exists v as t { _id: id }",
            "create json relational duality view v enable logical replication as t { _id: id }",
            "create json relational duality view v (a) as t { _id: id }",
            "create json relational duality view v (\"a\", b) as t { _id: id };",
            "create json relational duality view if not exists v (a) disable logical replication as t { _id: id }",
            "create or replace json relational duality view s.v (a) enable logical replication as t { _id: id }",
            "create json relational duality view v as s.t { _id: id }",
            "create json relational duality view v as \"S\".\"t\" @insert { _id: id }",
            "create or replace json duality view v disable logical replication as t { _id: id }",
            "create json relational duality view if not exists s.v disable logical replication as t @insert @update { _id: id }",
            "create json relational duality view v as t(check:{id:{_gt:1}}) { _id: id child [ { _id: id } ] };",
            "select 1 from dual;\ncreate json duality view v as t { _id: id name }\n/\nselect 2 from dual;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsWhatOracleRejects() {
        listOf(
            "create no force json relational duality view v as t { _id: id }",
            "create or replace json relational duality view if not exists v as t { _id: id }",
            "create json relational duality view if exists v as t { _id: id }",
            "create relational duality view v as t { _id: id }",
            "create duality view v as t { _id: id }",
            "create json relational duality view a.b.v as t { _id: id }",
            "create json relational duality view v@l as t { _id: id }",
            "create json relational duality view v as (t { _id: id })",
            "create json relational duality view v () as t { _id: id }",
            "create json relational duality view v (a,) as t { _id: id }",
            "create json relational duality view v (a b) as t { _id: id }",
            "create json relational duality view v disable logical replication (a) as t { _id: id }",
            "create json relational duality view v as a.b.t { _id: id }",
            "create json relational duality view v as al: t { _id: id }",
            "create json relational duality view v as t { _id: id } enable logical replication",
            "create json relational duality view enable logical replication v as t { _id: id }",
            "create json relational duality view v as enable logical replication t { _id: id }",
            "create json relational duality view v enable replication as t { _id: id }",
            "create json relational duality view v disable logical replication enable logical replication as t { _id: id }",
            "create json relational duality view v as t { _id: id } garbage",
            "create json relational duality view v as t { _id: id } t2 { _id: id }",
            "create json relational duality view v as t { _id: id }}",
            "create json relational duality view v as t u { _id: id }",
            "create json relational duality view v as t { _id: id",
            "create json relational duality view v as t _id: id }",
            "create json relational duality view v as",
            "create json relational duality view v as ;",
            "create json relational duality view v as # c\n t { _id: id }",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun leavesOrdinaryViewsAlone() {
        listOf(
            "create view v as select student from t;",
            "create or replace view v as select 1 x from dual with read only;",
            "create json collection table c;",
            "select '# not a comment' from dual;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        assertThat(p).notMatches("#")
        assertThat(p).notMatches("select 1 from dual # c\n;")
    }
}
