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

class ClusterSetExpressionTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun parsesModelFormAndOptionalArguments() {
        listOf(
            "cluster_set(km_sh_clus_sample, null, 0.2 using *)",
            "cluster_set(em_sh_clus_sample, null, 0.2 using *)",
            "cluster_set(model using *)",
            "cluster_set(model, 3 using *)",
            "cluster_set(model, 3, 0.2 using *)",
            "cluster_set(schema_name.model using *)",
            "cluster_set(model using t.*)",
            "cluster_set(model using schema_name.t.*)",
            "cluster_set(model using value as value_alias)",
            "cluster_set(model using value value_alias)",
            "cluster_set(model using 1 as first_value, 2 as second_value)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun parsesAnalyticFormAndItsSupportedClauses() {
        listOf(
            "cluster_set(into 2 using *) over ()",
            "cluster_set(into 2, 3, 0.2 using t.*) over (partition by grp)",
            "cluster_set(into 2 using value as value_alias) over (order by value desc nulls last)",
            "cluster_set(into 2 using *) over (partition by grp order by value asc)",
            "cluster_set(into 2 using *) over cluster_window",
            "cluster_set(into 2 using *) over (cluster_window partition by grp order by value)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun createsDedicatedAstBoundaryForSpecialSyntax() {
        val source = "cluster_set(em_sh_clus_sample, null, 0.2 using *)"
        val node = p.parse(source)

        assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_SET_EXPRESSION)).hasSize(1)
        assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).hasSize(1)
    }

    @Test
    fun preservesGenericAndQualifiedUserFunctionCalls() {
        for (source in listOf(
            "cluster_set(1)",
            "cluster_set(model)",
            "custom_pkg.cluster_set(1)",
            "\"cluster_set\"(1)"
        )) {
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_SET_EXPRESSION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun rejectsMalformedMiningAttributeAndAnalyticSyntax() {
        for (source in listOf(
            "cluster_set(model, using *)",
            "cluster_set(model, 1, using *)",
            "cluster_set(model using *, value)",
            "cluster_set(into using *) over ()",
            "cluster_set(into 2 using *)",
            "cluster_set(into 2 using *) over (order siblings by grp)",
            "cluster_set(into 2 using *) over (partition by grp rows unbounded preceding)",
            "cluster_set(model using *) over ()"
        )) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }
}
