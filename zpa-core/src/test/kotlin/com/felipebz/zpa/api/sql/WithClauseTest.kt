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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class WithClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.WITH_CLAUSE)
    }

    @Test
    fun matchesSimpleWith() {
        assertThat(p).matches("with q as (select 1 from dual)")
    }

    @Test
    fun matchesMultipleSubqueries() {
        assertThat(p).matches("with q as (select 1 from dual), q2 as (select 1 from dual)")
    }

    @Test
    fun matchesRecursiveSimple() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual)")
    }

    @Test
    fun matchesRecursiveWithSearch() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual) search depth first by a set order1")
    }

    @Test
    fun matchesRecursiveWithSearchAndCycle() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual) search depth first by a set order1 cycle id set cycle to 1 default 0")
    }

    @Test
    fun matchesWithValues() {
        listOf(
            "with q(a) as (values (1))", "with q(a) as (values (1), (2))", "with q(a, b) as (values (1, 'foo'), (2, 'bar'))",
            "with x(foo, bar, baz) as (values (0, 1, 2), (3, 4, 5), (6, 7, 8))", "with q as (values (1))",
            "with q(a, b) as (values (1))", "with q(a) as (values (1, 2))", "with q(a, b) as (values (1, 2), (3))",
            "with q(a) as (values ('a'), (sysdate), (null), (1 + 1), (:b), ((select 1 from dual)))",
            "with q(a) as (values (1)) search depth first by a set s",
            "with q(a) as (values (1)), r(b) as (values (2))", "with q(a) as (values (1)), r as (select 1 from dual)",
            "with r as (select 1 from dual), q(a) as (values (1))",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "with q() as (values (1))", "with q(a) as (values (1),)", "with q(a) as (values (1) (2))", "with q(a) as (values)",
            "with q(a) as (values 1)", "with q(a) as values (1)", "with q(a) as ((values (1)))", "with q(a) as (values (1,))",
            "with q(a) as (values (1)) y", "with q(a) as (values (1)) as y", "with q(a) as (values (1)) y(b)",
            "with q(a) as (values (1)) as t(a)", "with q(a) as (values (,))", "with q(a) as (values (1), , (2))",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsValuesCteStructure() {
        val clause = p.parse("with q(a, b) as (values (1, 'foo'), (2, 'bar')), r as (select 1 from dual)")
        val values = clause.getDescendants(DmlGrammar.CTE_VALUES_CLAUSE)
        assertThatAst(values.size).isEqualTo(1)
        assertThatAst(values[0].tokens.map { it.originalValue }).containsExactly(
            "(", "values", "(", "1", ",", "'foo'", ")", ",", "(", "2", ",", "'bar'", ")", ")")
        assertThatAst(clause.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE).size).isEqualTo(2)
        assertThatAst(clause.getDescendants(DmlGrammar.VALUES_EXPRESSION_CLAUSE)).isEmpty()
    }

    @Test
    fun keepsAliasedValuesRowSourcesUnchanged() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        listOf(
            "select * from (values (1, 2)) v(a, b)", "select * from (values (1, 2)) as v(a, b)",
            "select * from (values (1, 'foo'), (2, 'bar')) as t(a, b)", "with q(a) as (select 1 from dual) select * from (values (1)) v(a), q",
            "with q(a) as (values (1)) select * from q, (values (2)) v(b)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        assertThat(p).notMatches("select * from (values (1, 2))")
        val tree = p.parse("with q(a) as (values (1)) select * from (values (2)) v(b)")
        assertThatAst(tree.getDescendants(DmlGrammar.CTE_VALUES_CLAUSE).size).isEqualTo(1)
        assertThatAst(tree.getDescendants(DmlGrammar.VALUES_EXPRESSION_CLAUSE).size).isEqualTo(1)
    }

    @Test
    fun matchesFunctionDeclaration() {
        assertThat(p).matches("with function func return number is begin return 1; end;")
    }

    @Test
    fun matchesProcedureDeclaration() {
        assertThat(p).matches("with procedure proc is begin null; end;")
    }

    @Test
    fun matchesFunctionAndProcedureDeclaration() {
        assertThat(p).matches("with function func return number is begin return 1; end; " +
            "procedure proc is begin null; end;")
    }

    @Test
    fun matchesFunctionDeclarationAndQuery() {
        assertThat(p).matches("with function func return number is begin return 1; end; " +
            "q as (select 1 from dual)")
    }

}
