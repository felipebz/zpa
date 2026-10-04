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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AggregateAllModifierTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesAllInCommonAggregates() {
        listOf(
            "count", "sum", "avg", "min", "max", "stddev", "stddev_pop", "stddev_samp", "variance", "var_pop", "var_samp", "median",
        ).forEach {
            listOf("$it(all x)", "${it.uppercase()}(ALL x + 1)", "$it(all nvl(x, 'a'))", "$it(all (x))", "$it(all x) over ()").forEach { source ->
                assertThat(p).describedAs(source).matches(source)
            }
        }
        listOf(
            "count(all *)", "max(all x) over (partition by y order by z)", "max(all x) keep (dense_rank first order by y)",
            "max(all x) over (order by z rows between unbounded preceding and current row)", "max(all max(all x)) over ()",
            "1 + max(all x) * 2", "nvl(max(all x), 0)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsAllInOrdinaryFunctionsAndMalformedModifiers() {
        listOf(
            "nvl(all x)", "upper(all x)", "lower(all x)", "abs(all x)", "round(all x)", "to_char(all x)", "length(all x)", "pkg.max(all x)",
            "nvl(all x, 0)", "f(all x)", "max(all distinct x)", "max(distinct all x)", "max(all all x)", "max(all)", "max(all x, y)",
            "sum(all distinct x)", "min(all x", "max all x",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsExistingDistinctUniqueAndPlainCalls() {
        listOf(
            "max(distinct x)", "max(unique x)", "max(x)", "count(distinct x)", "count(unique x)", "count(*)", "count(x)", "sum(x) over ()",
            "avg(distinct x) over (partition by y)", "stddev(x)", "median(x)", "variance(x) keep (dense_rank last order by y)",
            "nvl(x, 0)", "upper(x)", "pkg.max(x)", "listagg(all x) within group (order by y)", "listagg(distinct x, ',') within group (order by y)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf("max(distinct x)", "count(*)", "sum(x)", "nvl(x, 0)").forEach {
            assertThatAst(p.parse(it).getDescendants(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)).describedAs(it).isEmpty()
        }
    }

    @Test
    fun buildsAggregateAllNodeAndFixtureStatement() {
        val call = p.parse("max(all miles)")
        val node = call.getFirstDescendant(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)
        assertThatAst(node.tokens.map { it.originalValue }).containsExactly("max", "(", "all", "miles", ")")
        val analytic = p.parse("max(all x) over (partition by y)")
        assertThatAst(analytic.getDescendants(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)).hasSize(1)
        assertThatAst(analytic.getDescendants(DmlGrammar.ANALYTIC_CLAUSE)).hasSize(1)
        assertThatAst(p.parse("count(all *)").getFirstDescendant(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION).tokens.map { it.originalValue })
            .containsExactly("count", "(", "all", "*", ")")

        setRootRule(PlSqlGrammar.FILE_INPUT)
        val statement = """SELECT
            MAX (DISTINCT miles),
            MAX (ALL miles)
            FROM Flights"""
        assertThat(p).matches(statement)
        assertThatAst(p.parse(statement).getDescendants(PlSqlGrammar.AGGREGATE_ALL_EXPRESSION)).hasSize(1)
    }
}
