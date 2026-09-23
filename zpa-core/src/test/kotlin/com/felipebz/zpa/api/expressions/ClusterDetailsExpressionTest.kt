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

class ClusterDetailsExpressionTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun parsesModelFormWithNestedArgumentsModifiersAndAttributes() {
        listOf(
            "cluster_details(model using *)",
            "cluster_details(model, 1 using *)",
            "cluster_details(model, 1, 5 using *)",
            "cluster_details(model, 1, 5 desc using *)",
            "cluster_details(model, 1, 5 asc using *)",
            "cluster_details(model, 1, 5 abs using *)",
            "cluster_details(model desc using *)",
            "cluster_details(model asc using *)",
            "cluster_details(model abs using *)",
            "cluster_details(model, 1 desc using *)",
            "cluster_details(schema_name.model using *)",
            "cluster_details(em_sh_clus_sample, s.cluster_id, 5 using t.*)",
            "cluster_details(model using schema_name.t.*)",
            "cluster_details(model using value)",
            "cluster_details(model using value as value_alias)",
            "cluster_details(model using value value_alias)",
            "cluster_details(model using 1 as first_value, 2 as second_value)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun parsesAnalyticFormAndSharedMiningWindows() {
        listOf(
            "cluster_details(into 2 using *) over ()",
            "cluster_details(into 2, 1 using *) over ()",
            "cluster_details(into 2, 1, 5 using t.*) over (partition by grp)",
            "cluster_details(into 2 using value as value_alias) over (order by value)",
            "cluster_details(into 2, 1, 5 abs using *) over (partition by grp order by value)",
            "cluster_details(into 2 desc using *) over ()",
            "cluster_details(into 2 asc using *) over ()",
            "cluster_details(into 2 abs using *) over ()",
            "cluster_details(into 2 using *) over details_window",
            "cluster_details(into 2 using *) over (details_window)"
        ).forEach { source ->
            assertThat(p).describedAs(source).matches(source)
        }
    }

    @Test
    fun createsDedicatedAstBoundaryForModelAndAnalyticForms() {
        for (source in listOf(
            "cluster_details(em_sh_clus_sample, s.cluster_id, 5 using t.*)",
            "cluster_details(into 2, 1, 5 abs using *) over (partition by grp order by value)"
        )) {
            val node = p.parse(source)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_DETAILS_EXPRESSION))
                .describedAs(source)
                .hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source)
                .hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_SET_EXPRESSION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun preservesGenericAndQualifiedUserFunctionCalls() {
        for (source in listOf(
            "cluster_details(1)",
            "cluster_details(model)",
            "custom_pkg.cluster_details(1)",
            "\"cluster_details\"(1)",
            "abs(1)"
        )) {
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.CLUSTER_DETAILS_EXPRESSION))
                .describedAs(source)
                .isEmpty()
        }
    }

    @Test
    fun rejectsOracleInvalidArgumentAndAnalyticBoundaries() {
        for (source in listOf(
            "cluster_details(model using)",
            "cluster_details(model, , 5 using *)",
            "cluster_details(model, 1, 5, 7 using *)",
            "cluster_details(model, 1, 5 desc asc using *)",
            "cluster_details(model, 1, 5 des using *)",
            "cluster_details(model, 1, 5 desc)",
            "cluster_details(model using *, value)",
            "cluster_details(into using *) over ()",
            "cluster_details(into 2 using *)",
            "cluster_details(model using *) over ()"
        )) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }
}
