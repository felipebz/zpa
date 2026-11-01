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

class ClusterIdExpressionTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun parsesModelFormAndSharedMiningAttributes() {
        listOf(
            "cluster_id(km_sh_clus_sample using *)",
            "cluster_id(schema_name.model using *)",
            "cluster_id(model using t.*)",
            "cluster_id(model using schema_name.t.*)",
            "cluster_id(model using value)",
            "cluster_id(model using value as value_alias)",
            "cluster_id(model using value value_alias)",
            "cluster_id(model using expr1 alias1, expr2 as alias2)",
            "cluster_id(/*+ grouping */ model using *)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun parsesAnalyticFormAndSharedMiningWindows() {
        listOf(
            "cluster_id(into 4 using *) over ()",
            "cluster_id(into 4 using *) over (partition by grp)",
            "cluster_id(into 4 using *) over (order by value)",
            "cluster_id(into 4 using *) over (partition by grp order by value)",
            "cluster_id(into 4 using *) over cluster_window",
            "cluster_id(into 4 using *) over (cluster_window)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun createsDedicatedAstBoundaryForModelAndAnalyticForms() {
        for (source in listOf(
            "cluster_id(km_sh_clus_sample using *)",
            "cluster_id(into 4 using *) over (partition by grp order by value)"
        )) {
            val node = p.parse(source)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_ID_EXPRESSION))
                .describedAs(source)
                .hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_SET_EXPRESSION))
                .describedAs(source)
                .isEmpty()
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_DETAILS_EXPRESSION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun preservesGenericAndQualifiedUserFunctionCalls() {
        for (source in listOf(
            "cluster_id(2)",
            "cluster_id(model)",
            "custom_pkg.cluster_id(1)",
            "\"cluster_id\"(1)"
        )) {
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_ID_EXPRESSION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun rejectsOracleInvalidMiningAndAnalyticBoundaries() {
        for (source in listOf(
            "cluster_id(model, 1 using *)",
            "cluster_id(model using *) over ()",
            "cluster_id(into 4 using *)",
            "cluster_id(into using *) over ()",
            "cluster_id(into 4) over ()",
            "cluster_id(model desc using *)",
            "cluster_id(model asc using *)",
            "cluster_id(model abs using *)"
        )) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }
}
