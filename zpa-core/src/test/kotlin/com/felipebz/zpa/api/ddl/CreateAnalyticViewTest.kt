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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AnalyticViewGrammar
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateAnalyticViewTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_ANALYTIC_VIEW)
    }

    private fun AstNode.texts() = tokens.map { it.originalValue }

    private val using = "using f dimension by (d key k references a hierarchies (h default))"

    private fun view(measures: String, tail: String = "") =
        "create analytic view v $using measures ($measures) $tail"

    @Test
    fun matchesTheDocumentedStatements() {
        listOf(
            """create or replace analytic view sales_av using sales_fact dimension by
                 (time_attr_dim key month_id references month_id hierarchies (time_hier default),
                  product_attr_dim key category_id references category_id hierarchies (product_hier default),
                  geography_attr_dim key state_province_id references state_province_id hierarchies (geography_hier default))
               measures (sales fact sales, units fact units,
                 sales_prior_period as (lag(sales) over (hierarchy time_hier offset 1)),
                 sales_year_ago as (lag(sales) over (hierarchy time_hier offset 1 across ancestor at level year)),
                 chg_sales_year_ago as (lag_diff(sales) over (hierarchy time_hier offset 1 across ancestor at level year)),
                 pct_chg as (lag_diff_percent(sales) over (hierarchy time_hier offset 1 across ancestor at level quarter)))
               default measure sales;""",
            """create or replace analytic view sales_av using av.sales_fact dimension by
                 (time_attr_dim key month_id references month_id hierarchies (time_hier default))
               measures (sales fact sales, avg_sales fact sales aggregate by avg, count_sales fact sales aggregate by count,
                 max_sales fact sales aggregate by max, min_sales fact sales aggregate by min,
                 stddev_sales fact sales aggregate by stddev, variance_sales fact sales aggregate by variance,
                 units fact units, avg_units fact units aggregate by avg)
               default measure sales default aggregate by sum;""",
            "create or replace analytic view sales_av using av.sales_fact dimension by (t key m references m hierarchies (h default)) " +
                "measures (sales fact sales, y as (round(lag_diff_percent(sales) over (hierarchy h offset 1 across ancestor at level year), 2))) " +
                "default measure sales;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesHeaderAndSourceOptions() {
        listOf(
            "create analytic view v", "create or replace analytic view v", "create force analytic view v",
            "create noforce analytic view v", "create or replace force analytic view v", "create or replace noforce analytic view v",
            "create analytic view if not exists s.v", "create analytic view v sharing = metadata", "create analytic view v sharing = none",
            "create analytic view v caption 'c'", "create analytic view v caption 'c' description 'd'",
            "create analytic view v description 'd'", "create analytic view v classification c1 value 'v' language 'en'",
            "create analytic view if not exists v sharing = none caption 'c'",
        ).forEach { head ->
            val source = "$head $using measures (m fact s)"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "using s.f", "using f remote", "using f x", "using f as x", "using f remote x", "using f remote as x",
        ).forEach {
            val source = "create analytic view v $it dimension by (d key k references a hierarchies (h)) measures (m)"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "create force or replace analytic view v", "create analytic view v sharing metadata",
            "create analytic view v caption 'c' sharing = none", "create analytic view v description 'd' caption 'c'",
        ).forEach {
            val source = "$it $using measures (m fact s)"
            assertThat(p).describedAs(source).notMatches(source)
        }
        listOf(
            "using f()", "using f (a)", "using (f)", "using f x y", "using f@lnk", "using f, g", "using f x remote", "using f as dimension",
            "using",
        ).forEach {
            val source = "create analytic view v $it dimension by (d key k references a hierarchies (h)) measures (m)"
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun matchesDimensionKeysReferencesAndHierarchies() {
        listOf(
            "d key k references a hierarchies (h default)", "d as da key k references a hierarchies (h)", "d da key k references a hierarchies (h)",
            "s.d key k references a hierarchies (h)", "d caption 'c' key k references a hierarchies (h)",
            "d as da classification c1 key k references a hierarchies (h)",
            "d key (k) references a hierarchies (h)", "d key a.k references a hierarchies (h)",
            "d key k references distinct a hierarchies (h)", "d key k references (a) hierarchies (h)",
            "d key k references distinct (a) hierarchies (h)",
            "d key (k1, k2) references (a1, a2) hierarchies (h)", "d key (a.k1, f.k2) references (a1, a2) hierarchies (h)",
            "d key (k1, k2) references distinct (a1, a2) hierarchies (h)",
            "d key k references a hierarchies (h as ha)", "d key k references a hierarchies (h ha)",
            "d key k references a hierarchies (h ha default)", "d key k references a hierarchies (h as ha default)",
            "d key k references a hierarchies (s.h as ha default)", "d key k references a hierarchies (h1 default, h2 default)",
            "d key k references a hierarchies (h1 default, h2)",
            "d key k references a hierarchies (h), e key k2 references a2 hierarchies (h2 default)",
        ).forEach {
            val source = "create analytic view v using f dimension by ($it) measures (m)"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "d key k1, k2 references a hierarchies (h)", "d key ((k)) references a hierarchies (h)", "d key () references a hierarchies (h)",
            "d key k references (a, b) hierarchies (h)", "d key (k) references (a1, a2) hierarchies (h)",
            "d key (k1, k2) references a hierarchies (h)", "d key (k1, k2) references (a) hierarchies (h)",
            "d key k references a, b hierarchies (h)", "d key k references x.a hierarchies (h)", "d key k references () hierarchies (h)",
            "d key k references distinct distinct a hierarchies (h)", "d key k references distinct hierarchies (h)",
            "d key k references a", "d key k hierarchies (h)", "d references a hierarchies (h)", "key k references a hierarchies (h)",
            "d key k classification c1 references a hierarchies (h)", "d key k references a hierarchies ()",
            "d key k references a hierarchies (h default default)", "d key k references a hierarchies (h default as ha)",
            "d key k references a hierarchies (a.b.h)", "d key k references a hierarchies (h caption 'c')",
            "d key k references a hierarchies (h default),", "",
        ).forEach {
            val source = "create analytic view v using f dimension by ($it) measures (m)"
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).notMatches("create analytic view v using f dimension by d key k references a hierarchies (h) measures (m)")
    }

    @Test
    fun matchesEveryBaseMeasureCombination() {
        listOf(
            "m", "m fact s", "m aggregate by sum", "m fact s aggregate by sum", "m fact (s)", "m fact s + 1", "m fact d.s aggregate by avg",
            "m fact s aggregate by approx_count_distinct_agg", "m, n", "m, n fact s", "m fact s1, n fact s2",
            "m fact s aggregate by sum, n as (1)", "m as (1), n", "m as (lag(s) over (hierarchy h offset 1))",
            "m as (rank() over (hierarchy h order by s))",
            "m caption 'c'", "m description 'd'", "m caption 'c' description 'd'", "m fact s caption 'c'", "m as (1) caption 'c'",
            "m fact s aggregate by sum caption 'c'", "m aggregate by sum caption 'c' description 'd' classification c1",
            "m caption 'c', n",
        ).forEach {
            val source = view(it)
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "m fact", "m fact aggregate by sum", "m as (1) aggregate by sum", "m as (1) fact s", "m fact s as (1)", "m as 1", "m as ()",
            "m aggregate sum", "m aggregate by", "m aggregate by sum aggregate by avg", "m caption 'c' fact s",
            "m fact s description 'd' caption 'c'", "m fact s aggregate by sum(s)", "m fact distinct s", "",
        ).forEach {
            val source = view(it)
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).notMatches("create analytic view v $using measures m")
        assertThat(p).notMatches("create analytic view v $using")
        assertThat(p).notMatches("create analytic view v using f measures (m fact s) dimension by (d key k references a hierarchies (h))")
    }

    @Test
    fun acceptsAnySingleTokenAsDefinitionAggregateFunction() {
        listOf("bogus", "median", "1", "1.5", "'x'", "a", "\"Q\"", "*", "select", "null", "default", "measures", "count").forEach {
            val perMeasure = view("m aggregate by $it")
            assertThat(p).describedAs(perMeasure).matches(perMeasure)
            val withFact = view("m fact s aggregate by $it, n")
            assertThat(p).describedAs(withFact).matches(withFact)
            val default = view("m", "default aggregate by $it")
            assertThat(p).describedAs(default).matches(default)
        }
        listOf("sum", "avg", "approx_count_distinct_agg", "var_samp").forEach {
            assertThat(p).matches(view("m aggregate by $it caption 'c'"))
            assertThat(p).matches(view("m", "default aggregate by $it enable query transform"))
        }
        val bogus = p.parse(view("m aggregate by bogus", "default aggregate by 1"))
        assertThatAst(bogus.getFirstDescendant(AnalyticViewGrammar.DEFINITION_AGGREGATE_BY_CLAUSE).texts())
            .containsExactly("aggregate", "by", "bogus")
        assertThatAst(bogus.getFirstChild(AnalyticViewGrammar.DEFAULT_AGGREGATE_CLAUSE).texts()).containsExactly("default", "aggregate", "by", "1")
        listOf("m aggregate by sum(s)", "m aggregate by", "m aggregate by ,", "m aggregate by )", "m aggregate by sum avg").forEach {
            assertThat(p).describedAs(it).notMatches(view(it))
        }
        assertThat(p).notMatches(view("m", "default aggregate by"))
        assertThat(p).notMatches(view("m", "default aggregate by sum(s)"))
    }

    @Test
    fun matchesTrailingClausesInOrder() {
        listOf(
            "", "default measure m", "default aggregate by sum", "default measure m default aggregate by avg",
            "default aggregate by var_samp", "enable query transform", "enable query transform rely", "enable query transform norely",
            "default measure m enable query transform rely", "default measure m default aggregate by sum enable query transform norely", ";",
        ).forEach {
            val source = view("m fact s", it)
            assertThat(p).describedAs(source).matches(source)
        }
        listOf(
            "default aggregate by avg default measure m", "default measure m default measure n", "default aggregate by sum default aggregate by avg",
            "default measure", "default aggregate", "default aggregate sum", "default measure s.m", "default measure 1", "default measure m, n",
            "default", "enable query transform default measure m", "enable query transform enable query transform", "enable", "caption 'c'",
            "using g",
        ).forEach {
            val source = view("m fact s", it)
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun buildsStructureWithSharedAndCreateSpecificNodes() {
        val node = p.parse(
            "create or replace analytic view s.v sharing = none caption 'c' using s.f remote x dimension by " +
                "(d as da key (k1, k2) references distinct (a1, a2) hierarchies (h1 as ha default, h2)) " +
                "measures (b fact f1 + 1 aggregate by avg, c as (lag(b) over (hierarchy h1 offset 1)), e) " +
                "default measure b default aggregate by sum enable query transform rely;")
        val dimension = node.getFirstChild(AnalyticViewGrammar.AV_DIMENSION_REFERENCE)
        assertThatAst(dimension.texts()).containsExactly(
            "d", "as", "da", "key", "(", "k1", ",", "k2", ")", "references", "distinct", "(", "a1", ",", "a2", ")",
            "hierarchies", "(", "h1", "as", "ha", "default", ",", "h2", ")")
        assertThatAst(dimension.getChildren(AnalyticViewGrammar.AV_HIERARCHY_REFERENCE).map { it.texts() })
            .containsExactly(listOf("h1", "as", "ha", "default"), listOf("h2"))

        val measures = node.getChildren(AnalyticViewGrammar.AV_DEFINITION_MEASURE)
        assertThatAst(measures.map { it.firstChild.tokenOriginalValue }).containsExactly("b", "c", "e")
        val base = measures[0].getFirstChild(AnalyticViewGrammar.AV_DEFINITION_BASE_MEASURE)
        assertThatAst(base.texts()).containsExactly("fact", "f1", "+", "1", "aggregate", "by", "avg")
        assertThatAst(base.getFirstChild(AnalyticViewGrammar.DEFINITION_AGGREGATE_BY_CLAUSE).texts()).containsExactly("aggregate", "by", "avg")
        assertThatAst(measures[1].hasDirectChildren(AnalyticViewGrammar.CALC_MEASURE_CLAUSE)).isTrue()
        assertThatAst(measures[1].hasDirectChildren(AnalyticViewGrammar.AV_DEFINITION_BASE_MEASURE)).isFalse()
        assertThatAst(measures[1].getFirstDescendant(AnalyticViewGrammar.AV_LEAD_LAG_EXPRESSION)).isNotNull()
        assertThatAst(measures[2].getFirstChild(AnalyticViewGrammar.AV_DEFINITION_BASE_MEASURE).children).isEmpty()

        assertThatAst(node.getFirstChild(AnalyticViewGrammar.DEFAULT_MEASURE_CLAUSE).texts()).containsExactly("default", "measure", "b")
        assertThatAst(node.getFirstChild(AnalyticViewGrammar.DEFAULT_AGGREGATE_CLAUSE).texts()).containsExactly("default", "aggregate", "by", "sum")
        assertThatAst(node.getFirstChild(AnalyticViewGrammar.QUERY_TRANSFORM_CLAUSE).texts())
            .containsExactly("enable", "query", "transform", "rely")
    }

    @Test
    fun keepsQueryBaseMeasuresStricterThanDefinitionBaseMeasures() {
        setRootRule(com.felipebz.zpa.api.DmlGrammar.SELECT_EXPRESSION)
        val definitionOnly = listOf("m", "m fact s", "m aggregate by sum")
        definitionOnly.forEach {
            assertThat(p).describedAs(it).notMatches("select 1 from analytic view (using sales_av add measures ($it))")
        }
        assertThat(p).matches("select 1 from analytic view (using sales_av add measures (m fact s aggregate by sum))")
        listOf("bogus", "median", "1", "'x'", "select").forEach {
            assertThat(p).describedAs(it).notMatches("select 1 from analytic view (using sales_av add measures (m fact s aggregate by $it))")
        }
    }
}
