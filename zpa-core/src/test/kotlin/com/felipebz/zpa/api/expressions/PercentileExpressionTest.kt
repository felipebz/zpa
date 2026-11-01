//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class PercentileExpressionTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesAggregateAndAnalyticPercentiles() {
        listOf(
            "percentile_disc(0.5) within group (order by value)",
            "percentile_cont(0.5) within group (order by value desc)",
            "percentile_disc(:b7) within group (order by obs_value asc) over (partition by metric_id, bsln_guid, timegroup)",
            "percentile_cont(0.5) within group (order by obs_value asc) over (partition by metric_id, bsln_guid, timegroup)",
            "percentile_disc(0.5) within group (order by value) over ()",
            "percentile_cont(0.5) within group (order by value desc nulls first)",
            "percentile_disc(0.5) within group (order by value nulls last)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun keepsDistinctAstBoundariesForBothFunctions() {
        listOf(
            "percentile_disc(0.5) within group (order by value)" to AggregateSqlFunctionsGrammar.PERCENTILE_DISC_EXPRESSION,
            "percentile_cont(0.5) within group (order by value)" to AggregateSqlFunctionsGrammar.PERCENTILE_CONT_EXPRESSION
        ).forEach { (source, kind) ->
            val node = p.parse(source)
            assertThatAst(node.getDescendants(kind)).hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)
        }
    }

    @Test
    fun matchesFilterBeforeAnalyticClause() {
        for (function in listOf("percentile_disc", "percentile_cont")) {
            assertThat(p).matches("$function(0.5) within group (order by value) filter (where grp = 1)")
            assertThat(p).matches("$function(0.5) within group (order by value) filter (where value > 10) over (partition by grp)")
            assertThat(p).notMatches("$function(0.5) within group (order by value) over (partition by grp) filter (where value > 10)")
        }
    }

    @Test
    fun rejectsUnsupportedOrderingAndAnalyticClauses() {
        for (function in listOf("percentile_disc", "percentile_cont")) {
            assertThat(p).notMatches("$function(0.5) within group (order by value, grp)")
            assertThat(p).notMatches("$function(0.5) within group (order siblings by value)")
            assertThat(p).notMatches("$function(0.5) within group (order by value) over (partition by grp order by value)")
            assertThat(p).notMatches("$function(0.5) within group (order by value) over (partition by grp rows unbounded preceding)")
        }
    }

    @Test
    fun keepsQualifiedCustomCallsAvailable() {
        assertThat(p).matches("custom_pkg.percentile_disc(0.5)")
        assertThat(p).matches("\"percentile_cont\"(0.5)")
    }

    @Test
    fun rejectsIncompletePercentileSyntax() {
        for (function in listOf("percentile_disc", "percentile_cont")) {
            assertThat(p).notMatches("$function(")
            assertThat(p).notMatches("$function() within group (order by value)")
            assertThat(p).notMatches("$function(0.5)")
            assertThat(p).notMatches("$function(0.5) within group")
            assertThat(p).notMatches("$function(0.5) within group (order by)")
        }
    }
}
