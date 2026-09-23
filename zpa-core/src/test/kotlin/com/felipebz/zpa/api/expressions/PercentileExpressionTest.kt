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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
