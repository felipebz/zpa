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

class DataMiningFunctionsTest : RuleTest() {
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

    @Test
    fun parsesClusterDistanceAndProbability() {
        assertMatches(
            "cluster_distance(km_sh_clus_sample using *)",
            "cluster_distance(schema_name.model, 2 using t.*)",
            "cluster_distance(into 3 using a, b) over ()",
            "cluster_distance(into 3, 1 using a) over (partition by grp order by id)",
            "cluster_probability(km_sh_clus_sample, 2 using *)",
            "cluster_probability(into 3, 1 using a as alias) over w"
        )
        assertNotMatches(
            "cluster_distance(model, 1, 2 using *)",
            "cluster_distance(into 3 using *)",
            "cluster_probability(model desc using *)"
        )
    }

    @Test
    fun parsesFeatureFunctions() {
        assertMatches(
            "feature_id(nmf_sh_sample using *)",
            "feature_id(into 2 using a) over ()",
            "feature_value(nmf_sh_sample, 3 using *)",
            "feature_value(into 2, 1 using a) over ()",
            "feature_set(nmf_sh_sample, 10 using *)",
            "feature_set(model, 3, 0.1 using *)",
            "feature_set(into 6 using *) over ()",
            "feature_details(nmf_sh_sample, s.feature_id, 5 using t.*)",
            "feature_details(model abs using *)",
            "feature_details(into 2, 1, 3 desc using a) over (partition by grp)",
            "feature_compare(esa_wiki_mod using 'text one' text and using 'text two' text)",
            "feature_compare(model using * and using *)"
        )
        assertNotMatches(
            // ORA-00939: FEATURE_ID takes no extra argument.
            "feature_id(model, 1 using *)",
            "feature_value(model, 1, 2 using *)",
            // ORA-00936: FEATURE_COMPARE needs two attribute clauses; ORA-30483: no analytic form.
            "feature_compare(model using *)",
            "feature_compare(into 2 using a and using b) over ()"
        )
    }

    @Test
    fun parsesPredictionModelFormsAndCostMatrix() {
        assertMatches(
            "prediction(mymodel using *)",
            "prediction(/*+ grouping */ schema_name.model using *)",
            "prediction(dt_sh_clas_sample cost model using cust_marital_status, education, household_size)",
            "prediction(model cost model auto using *)",
            "prediction(model cost ('a', 'b') values ((0, 1), (1, 0)) using *)",
            "prediction(model cost (0, 1, 2) values ((0, 1, 1), (1, 0, 1), (1, 1, 0)) using *)",
            "prediction_bounds(glmr_sh_regr_sample, 0.98 using *)",
            "prediction_bounds(model, 0.98, 1 using *)",
            "prediction_cost(dt_sh_clas_sample, 1 cost model using *)",
            "prediction_cost(model cost ('a', 'b') values ((0, 1), (1, 0)) using *)",
            "prediction_details(svmr_sh_regr_sample, null, 3 using *)",
            "prediction_details(model, 1, 3 desc using *)",
            "prediction_probability(dt_sh_clas_sample, 1 using *)",
            "prediction_set(dt_sh_clas_sample cost model using *)",
            "prediction_set(model, 2, 0.1 cost model using *)",
            "ora_dm_partition_name(mymodel using *)"
        )
        assertNotMatches(
            // ORA-00939: PREDICTION has no class argument.
            "prediction(model, 1 using *)",
            // ORA-40283: PREDICTION_COST requires a cost matrix.
            "prediction_cost(model, 1 using *)",
            // ORA-02000: a cost matrix needs at least two classes, two rows and two costs per row.
            "prediction(model cost ('a') values ((0)) using *)",
            "prediction(model cost ('a', 'b') values ((0, 1)) using *)",
            "prediction(model cost ('a', 'b') values ((0), (1)) using *)",
            "prediction(model cost model model using *)",
            "prediction(model using)",
            // ORA-40281: INTO is not a PREDICTION form.
            "prediction(into 2 using a) over ()"
        )
    }

    @Test
    fun parsesOrderedModelForms() {
        assertMatches(
            "prediction(model using *) over (order by id)",
            "prediction(model using *) over (order by id desc nulls last, age)",
            "prediction_cost(model, 1 cost model using *) over (order by id)",
            "prediction_bounds(model using *) over (order by id)",
            "prediction_set(model using *) over (order by id)",
            "cluster_id(model using *) over (order by id)",
            "feature_compare(model using * and using *) over (order by id)"
        )
        assertNotMatches(
            // ORA-02000: model forms only accept OVER (ORDER BY ...).
            "prediction(model using *) over ()",
            "prediction(model using *) over (partition by grp)",
            "prediction(model using *) over (partition by grp order by id)",
            "prediction(model using *) over w",
            "prediction(model using *) over (order by id rows unbounded preceding)",
            "prediction(model using *) over (order by id, order by age)",
            // ORA-30929
            "prediction(model using *) over (order siblings by id)",
            // ORA-00923: ORA_DM_PARTITION_NAME has no OVER clause.
            "ora_dm_partition_name(model using *) over (order by id)"
        )
    }

    @Test
    fun parsesPredictionAnalyticForms() {
        assertMatches(
            "prediction(for age using *) over ()",
            "prediction(for age + 1 using a) over (partition by grp order by id)",
            "prediction(of anomaly using a, b) over w",
            "prediction(for b cost model using a) over ()",
            "prediction_details(for age abs using *) over ()",
            "prediction_details(of anomaly, 0, 3 using *) over (partition by cust_marital_status)",
            "prediction_details(for b, 'x1', 2 desc using a) over ()",
            "prediction_probability(of anomaly, 0 using *) over (partition by cust_marital_status)",
            "prediction_probability(for b, 'x1' using a) over ()",
            "prediction_set(for b, 2, 0.1 using a) over ()",
            "prediction_cost(of anomaly, 1 cost model using a) over ()",
            "prediction_bounds(for age, 0.9 using a) over ()"
        )
        assertNotMatches(
            // ORA-30484: the analytic forms require OVER.
            "prediction(for age using a)",
            "prediction_details(of anomaly using a)",
            // ORA-00939: PREDICTION takes no class after OF ANOMALY.
            "prediction(of anomaly, 1 using a) over ()",
            "prediction(of using a) over ()",
            "prediction(for using a) over ()"
        )
    }

    @Test
    fun parsesMemberAccessOnPredictionBounds() {
        assertMatches(
            "prediction_bounds(glmr_sh_regr_sample, 0.98 using *).lower",
            "prediction_bounds(glmr_sh_regr_sample, 0.98 using *).upper"
        )
    }

    @Test
    fun createsDedicatedAstBoundaries() {
        for ((source, rule) in listOf(
            "prediction(model using *)" to AggregateSqlFunctionsGrammar.PREDICTION_EXPRESSION,
            "prediction_details(for age abs using *) over ()" to AggregateSqlFunctionsGrammar.PREDICTION_DETAILS_EXPRESSION,
            "feature_compare(model using * and using *)" to AggregateSqlFunctionsGrammar.FEATURE_COMPARE_EXPRESSION,
            "cluster_distance(into 3 using a) over ()" to AggregateSqlFunctionsGrammar.CLUSTER_DISTANCE_EXPRESSION
        )) {
            val node = p.parse(source)
            assertThatAst(node.getDescendants(rule)).describedAs(source).hasSize(1)
            assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION))
                .describedAs(source).hasSize(1)
        }
    }

    @Test
    fun preservesUserFunctionsWithMiningNames() {
        for (source in listOf(
            "prediction(1)",
            "prediction(model)",
            "prediction(1) over (partition by grp)",
            "feature_id(x, y)",
            "custom_pkg.prediction_set(1)",
            "\"PREDICTION\"(1)",
            "s.prediction",
            "feature_id"
        )) {
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            for (rule in AggregateSqlFunctionsGrammar.miningFunctions) {
                assertThatAst(node.getDescendants(rule)).describedAs(source).isEmpty()
            }
        }
    }
}
