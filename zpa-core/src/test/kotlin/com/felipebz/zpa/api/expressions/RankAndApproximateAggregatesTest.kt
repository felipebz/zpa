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
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RankAndApproximateAggregatesTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun assertMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).matches(source)
        }
    }

    private fun assertNotMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    private fun assertParsesAs(source: String, ruleKey: AggregateSqlFunctionsGrammar) {
        assertThatAst(p.parse(source).getDescendants(ruleKey)).describedAs(source).hasSize(1)
    }

    private fun assertGenericCall(source: String) {
        val node = p.parse(source)
        assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
            .describedAs(source).isEmpty()
        assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL)).describedAs(source).isNotEmpty
    }

    @Test
    fun parsesHypotheticalSetAggregates() {
        assertParsesAs("rank(15500, .05) within group (order by salary, commission_pct)",
            AggregateSqlFunctionsGrammar.RANK_AGGREGATE_EXPRESSION)
        assertParsesAs("dense_rank(15500, .05) within group (order by salary desc, commission_pct)",
            AggregateSqlFunctionsGrammar.DENSE_RANK_AGGREGATE_EXPRESSION)
        assertParsesAs("cume_dist(15500, .05) within group (order by salary, commission_pct)",
            AggregateSqlFunctionsGrammar.CUME_DIST_AGGREGATE_EXPRESSION)
        assertParsesAs("percent_rank(15000, .05) within group (order by salary, commission_pct)",
            AggregateSqlFunctionsGrammar.PERCENT_RANK_AGGREGATE_EXPRESSION)
        assertMatches(
            "rank(15500) within group (order by salary desc nulls last)",
            "rank(150) within group (order by 1)",
            // ORA-00909 for mismatched counts is raised after parsing.
            "rank(1, 2) within group (order by s)",
            "rank(150) within group (order by s) filter (where d = 1)",
            "dense_rank(150) within group (order by s) filter (where d = 1)"
        )
    }

    @Test
    fun keepsAnalyticAndGenericFormsOnGenericCalls() {
        assertGenericCall("rank() over (order by s)")
        assertGenericCall("dense_rank() over (partition by d order by s)")
        assertGenericCall("cume_dist() over (order by s)")
        assertGenericCall("percent_rank() over w")
        assertGenericCall("rank(1)")
        assertGenericCall("approx_median(a, b, c)")
        assertGenericCall("approx_count(a, b, c)")
        assertGenericCall("approx_rank()")
    }

    @Test
    fun rejectsInvalidHypotheticalSetAggregates() {
        assertNotMatches(
            // ORA-00923
            "rank(150) within group (order by s) over (partition by d)",
            "rank(150) within group (order by s) keep (dense_rank first order by s)",
            "cume_dist(150) within group (order by s) filter (where d = 1)",
            "percent_rank(150) within group (order by s) filter (where d = 1)",
            // ORA-30484
            "rank() within group (order by s)",
            // ORA-30491
            "rank(150) within group (s)",
            // ORA-30929
            "rank(150) within group (order siblings by s)"
        )
    }

    @Test
    fun parsesApproximateMedianAndPercentile() {
        assertParsesAs("approx_median(salary deterministic)", AggregateSqlFunctionsGrammar.APPROX_MEDIAN_EXPRESSION)
        assertParsesAs("approx_percentile(0.25 deterministic) within group (order by salary asc)",
            AggregateSqlFunctionsGrammar.APPROX_PERCENTILE_EXPRESSION)
        assertMatches(
            "approx_median(s)",
            "approx_median(s deterministic, 'ERROR_RATE')",
            "approx_median(s, 'CONFIDENCE') filter (where d = 1)",
            "approx_percentile(0.25) within group (order by s)",
            "approx_percentile(0.25) within group (order by s desc nulls last)",
            "approx_percentile(0.25 deterministic, 'CONFIDENCE') within group (order by s) filter (where d = 1)"
        )
        assertNotMatches(
            // ORA-00907
            "approx_median(deterministic s)",
            "approx_median(s deterministic deterministic)",
            // ORA-00923
            "approx_median(s) over (partition by d)",
            "approx_percentile(0.25) within group (order by s) over (partition by d)",
            // ORA-00909
            "approx_percentile(0.25) within group (order by s, c)",
            // ORA-02000
            "approx_percentile(0.25 deterministic)"
        )
    }

    @Test
    fun parsesApproximateTopFunctions() {
        assertParsesAs("approx_rank(partition by department_id order by approx_sum(salary) desc)",
            AggregateSqlFunctionsGrammar.APPROX_RANK_EXPRESSION)
        val nested = p.parse("approx_rank(partition by department_id order by approx_count(*) desc)")
        assertThatAst(nested.getDescendants(AggregateSqlFunctionsGrammar.APPROX_COUNT_EXPRESSION)).hasSize(1)
        assertThatAst(nested.getDescendants(DmlGrammar.PARTITION_BY_CLAUSE)).hasSize(1)
        assertMatches(
            "approx_rank(order by approx_sum(s) desc)",
            "approx_rank(partition by d, j order by approx_count(*) desc) filter (where d = 1)",
            // ORA-62231 / ORA-62232 / ORA-62233 are raised after parsing.
            "approx_rank(partition by d order by sum(s))",
            "approx_rank(partition by d order by approx_count(*) desc, approx_sum(s) desc)",
            "approx_count(*)",
            "approx_count(*, 'MAX_ERROR')",
            "approx_count(s, 'MAX_ERROR') filter (where d = 1)",
            "approx_sum(s)",
            "approx_sum(s, 'MAX_ERROR') filter (where d = 1)"
        )
        assertNotMatches(
            // ORA-00907
            "approx_rank(partition by d)",
            "approx_rank(s partition by d order by approx_sum(s) desc)",
            "approx_rank(order by approx_count(*) desc partition by d)",
            "approx_rank(partition by d partition by j order by approx_count(*) desc)",
            "approx_rank(partition by d order by approx_count(*) desc order by approx_sum(s) desc)",
            "approx_sum(s deterministic)",
            // ORA-30929
            "approx_rank(partition by d order siblings by approx_sum(s) desc)",
            // ORA-00923
            "approx_rank(partition by d order by approx_sum(s) desc) over (order by d)",
            "approx_count(s) over ()",
            "approx_sum(s) over ()"
        )
    }

    @Test
    fun parsesKeepWithPartitionBy() {
        val node = p.parse("max(dummy) keep (dense_rank first partition by dummy)")
        assertThatAst(node.getFirstDescendant(DmlGrammar.KEEP_CLAUSE).getDescendants(DmlGrammar.PARTITION_BY_CLAUSE))
            .hasSize(1)
        assertMatches(
            "max(d) keep (dense_rank last partition by d, s)",
            "max(d) keep (dense_rank first partition by d) over (partition by s)"
        )
        assertNotMatches(
            // ORA-00907
            "max(d) keep (dense_rank first partition by d order by s)",
            "max(d) keep (dense_rank first order by s partition by d)",
            // ORA-00924
            "max(d) keep (dense_rank first)"
        )
    }
}
