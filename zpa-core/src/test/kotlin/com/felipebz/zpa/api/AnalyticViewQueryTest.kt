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
package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalyticViewQueryTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
    }

    private fun measure(expression: String) =
        "select 1 from analytic view (using sales_av add measures (m as ($expression)))"

    private fun AstNode.texts() = tokens.map { it.originalValue }

    @Test
    fun matchesTheDocumentedQueries() {
        listOf(
            """select time_hier.member_name as time, sales, units from sales_av hierarchies(time_hier)
               where time_hier.level_name = 'YEAR' order by time_hier.hier_order""",
            """with my_av analytic view as (using sales_av hierarchies (time_hier)
                 add measures (lag_sales as (lag(sales) over (hierarchy time_hier offset 1))))
               select time_hier.member_name time, sales, lag_sales from my_av hierarchies (time_hier)
               where time_hier.level_name = 'YEAR' order by time_hier.hier_order""",
            """with my_av analytic view as (using sales_av hierarchies (time_hier)
                 filter fact (time_hier to quarter_of_year in (1, 2) and year_name in ('CY2011', 'CY2012')))
               select time_hier.member_name time, sales from my_av hierarchies (time_hier)
               where time_hier.level_name in ('YEAR', 'QUARTER')""",
            """select time_hier.member_name time, sales, lag_sales from analytic view (
                 using sales_av hierarchies (time_hier)
                 add measures (lag_sales as (lag(sales) over (hierarchy time_hier offset 1))))
               where time_hier.level_name = 'YEAR' order by time_hier.hier_order""",
            """select geography_hier.member_name as "Region", units as "Units", units_geog_rank_level as "Rank"
               from analytic view (using sales_av hierarchies (geography_hier)
                 add measures (units_geog_rank_level as (rank() over (hierarchy geography_hier
                   order by units desc nulls last within level))))
               where geography_hier.level_name in ('REGION') order by units_geog_rank_level""",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun matchesQueryBodyClauses() {
        listOf(
            "select 1 from analytic view (using sales_av)", "select 1 from analytic view (using s.sales_av hierarchies (d.h1, h2))",
            "select 1 from analytic view (using sales_av hierarchies ())", "select 1 from analytic view (using sales_av) x",
            "select 1 from analytic view (using sales_av filter fact (h1 to a = 1, h2 to b in (1, 2) and c = 3))",
            "select 1 from analytic view (using sales_av filter fact (h1 to a = 1) add measures (m as (1), n as (2)))",
            "select 1 from dual join analytic view (using sales_av) x on 1 = 1", "select 1 from dual, analytic view (using sales_av)",
            "select 1 from sales_av hierarchies (h1, h2) x", "select 1 from sales_av hierarchies ()",
            "select 1 from s.sales_av hierarchies (h1) as of scn 1", "select 1 from sales_av partition (p1) hierarchies (h1)",
            "select 1 from sales_av x, sales_av hierarchies (h1) y where x.a = y.a",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsMalformedQueryBodies() {
        listOf(
            "select 1 from analytic view (using sales_av) as x", "select 1 from analytic view ()",
            "select 1 from analytic view (sales_av)", "select 1 from analytic view using sales_av",
            "select 1 from analytic view (select 1 from dual)",
            "select 1 from analytic view (using sales_av add measures (m as (1)) filter fact (h1 to a = 1))",
            "select 1 from analytic view (using sales_av hierarchies (h1) hierarchies (h2))",
            "select 1 from analytic view (using sales_av add measures (m as (1)) add measures (n as (2)))",
            "select 1 from analytic view (using sales_av filter fact ())", "select 1 from analytic view (using sales_av filter fact (h1 a = 1))",
            "select 1 from analytic view (using sales_av add measures ())", "select 1 from analytic view (using sales_av add measures (m))",
            "select 1 from sales_av x hierarchies (h1)", "select 1 from sales_av hierarchies", "select 1 from dual hierarchies",
            "select 1 from sales_av hierarchies (h1) sample (10)",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsHierarchiesOutOfTableAliases() {
        val node = p.parse("select 1 from sales_av hierarchies (h1, h2) x")
        val table = node.getFirstDescendant(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
        assertThatAst(table.getFirstChild(DmlGrammar.TABLE_REFERENCE).texts()).containsExactly("sales_av")
        assertThatAst(table.getFirstChild(AnalyticViewGrammar.HIERARCHIES_CLAUSE).texts())
            .containsExactly("hierarchies", "(", "h1", ",", "h2", ")")
        assertThatAst(table.getFirstChild(DmlGrammar.ALIAS).tokenOriginalValue).isEqualTo("x")

        val ordinary = p.parse("select 1 from emp e, dept d where e.a = d.a")
            .getFirstDescendant(DmlGrammar.FROM_CLAUSE)
        assertThatAst(ordinary.getDescendants(AnalyticViewGrammar.HIERARCHIES_CLAUSE)).isEmpty()
        assertThatAst(ordinary.getDescendants(DmlGrammar.ALIAS).map { it.tokenOriginalValue }).containsExactly("e", "d")
    }

    @Test
    fun separatesAnalyticViewFactoringFromOrdinaryCtes() {
        val node = p.parse(
            "with c1 as (select 1 from dual), a analytic view as (using sales_av), c2 (x) as (select 2 from dual) select 1 from a, c1, c2")
        assertThatAst(node.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE).map { it.firstChild.tokenOriginalValue })
            .containsExactly("c1", "c2")
        val subav = node.getDescendants(AnalyticViewGrammar.SUBAV_FACTORING_CLAUSE)
        assertThatAst(subav.map { it.firstChild.tokenOriginalValue }).containsExactly("a")
        assertThatAst(subav[0].getFirstChild(AnalyticViewGrammar.ANALYTIC_VIEW_QUERY_CLAUSE).texts()).containsExactly("using", "sales_av")

        listOf(
            "with a analytic view (c1) as (using sales_av) select 1 from a", "with a analytic view as (select 1 from dual) select 1 from a",
            "with a as (analytic view (using sales_av)) select 1 from a", "with a analytic view as () select 1 from a",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        assertThat(p).matches("with function f return number is begin return 1; end; a analytic view as (using sales_av) select 1 from a")
    }

    @Test
    fun buildsInlineAnalyticViewNode() {
        val table = p.parse("select 1 from analytic view (using sales_av hierarchies (h) filter fact (h to a = 1)) av")
            .getFirstDescendant(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
        val inline = table.getFirstChild(AnalyticViewGrammar.INLINE_ANALYTIC_VIEW)
        val body = inline.getFirstChild(AnalyticViewGrammar.ANALYTIC_VIEW_QUERY_CLAUSE)
        assertThatAst(body.children.filter { it.type is AnalyticViewGrammar }.map { it.type }).containsExactly(
            AnalyticViewGrammar.HIERARCHIES_CLAUSE, AnalyticViewGrammar.FILTER_FACT_CLAUSE)
        assertThatAst(body.getFirstDescendant(AnalyticViewGrammar.FILTER_FACT_ITEM).texts()).containsExactly("h", "to", "a", "=", "1")
        assertThatAst(table.getFirstChild(DmlGrammar.ALIAS).tokenOriginalValue).isEqualTo("av")
    }

    @Test
    fun matchesFilterFactOnHierarchiesAndMeasures() {
        listOf(
            "filter fact (measures to sales between 100 and 200)", "filter fact (measures to sales > 1 and units < 5)",
            "filter fact (measures to sales > 1, measures to units < 5)", "filter fact (h1 to a = 1, measures to sales > 1)",
            "filter fact (measures to sales > 1, h1 to a = 1)", "filter fact (h1 to a = 1, h1 to b = 2)",
            "filter fact (d.measures to sales > 1)", "filter fact (measure to sales > 1)",
            "hierarchies (h1) filter fact (measures to sales > 1) add measures (m as (1))",
        ).forEach {
            val source = "select 1 from analytic view (using sales_av $it)"
            assertThat(p).describedAs(it).matches(source)
        }
        listOf(
            "filter fact (measures sales > 1)", "filter fact (measures to)", "filter fact (measures to sales > 1, units < 5)",
            "filter fact (measures to sales > 1) filter fact (h1 to a = 1)",
        ).forEach { assertThat(p).describedAs(it).notMatches("select 1 from analytic view (using sales_av $it)") }

        val items = p.parse("select 1 from analytic view (using sales_av filter fact (measures to sales between 100 and 200, h1 to a = 1))")
            .getDescendants(AnalyticViewGrammar.FILTER_FACT_ITEM)
        assertThatAst(items.map { it.texts() }).containsExactly(
            listOf("measures", "to", "sales", "between", "100", "and", "200"), listOf("h1", "to", "a", "=", "1"))
        assertThatAst(items.map { it.hasDirectChildren(AnalyticViewGrammar.HIERARCHY_REFERENCE) }).containsExactly(false, true)
    }

    @Test
    fun matchesQueryBaseMeasures() {
        listOf(
            "m fact sales aggregate by sum", "m fact (sales) aggregate by sum", "m fact sales + 1 aggregate by sum",
            "m fact (sales + 1) aggregate by avg", "m fact d.sales aggregate by count", "m fact 1 aggregate by max",
            "m fact sales aggregate by min", "m fact sales aggregate by stddev", "m fact sales aggregate by stddev_pop",
            "m fact sales aggregate by stddev_samp", "m fact sales aggregate by var_pop", "m fact sales aggregate by var_samp",
            "m fact sales aggregate by variance", "m fact sales aggregate by approx_count_distinct",
            "m fact sales aggregate by APPROX_COUNT_DISTINCT_AGG",
            "m fact sales aggregate by sum, n as (1)", "m as (1), n fact sales aggregate by sum",
            "m fact sales aggregate by sum, n fact units aggregate by avg",
        ).forEach { assertThat(p).describedAs(it).matches("select 1 from analytic view (using sales_av add measures ($it))") }
        listOf(
            "m fact sales", "m fact (sales)", "m fact aggregate by sum", "m aggregate by sum",
            "m fact sales aggregate sum", "m fact sales aggregate by median", "m fact sales aggregate by first_value",
            "m fact sales aggregate by bogus", "m fact sales aggregate by sum(sales)", "m fact sales aggregate by sum aggregate by avg",
            "m fact sales aggregate by sum as (1)", "m as (1) aggregate by sum", "m as (1) fact sales aggregate by sum",
            "m fact sales aggregate by sum classification c1 value 'v'", "m fact sales aggregate by sum distinct",
        ).forEach { assertThat(p).describedAs(it).notMatches("select 1 from analytic view (using sales_av add measures ($it))") }
    }

    @Test
    fun buildsBaseAndCalculatedMeasuresInOneList() {
        val measures = p.parse(
            "select 1 from analytic view (using sales_av add measures (m fact sales + 1 aggregate by avg, n as (m * 2)))")
            .getDescendants(AnalyticViewGrammar.ANALYTIC_VIEW_MEASURE)
        assertThatAst(measures.map { it.firstChild.tokenOriginalValue }).containsExactly("m", "n")
        val base = measures[0].getFirstChild(AnalyticViewGrammar.BASE_MEASURE_CLAUSE)
        assertThatAst(base.texts()).containsExactly("fact", "sales", "+", "1", "aggregate", "by", "avg")
        assertThatAst(base.getFirstChild(AnalyticViewGrammar.AGGREGATE_BY_CLAUSE).texts()).containsExactly("aggregate", "by", "avg")
        assertThatAst(measures[0].hasDirectChildren(AnalyticViewGrammar.CALC_MEASURE_CLAUSE)).isFalse()
        assertThatAst(measures[1].hasDirectChildren(AnalyticViewGrammar.CALC_MEASURE_CLAUSE)).isTrue()
        assertThatAst(measures[1].hasDirectChildren(AnalyticViewGrammar.BASE_MEASURE_CLAUSE)).isFalse()
    }

    @Test
    fun matchesHierarchyLeadLagMeasures() {
        listOf(
            "lag(sales) over (hierarchy time_hier offset 1)", "lead(sales) over (hierarchy time_hier offset 1)",
            "lag_diff(sales) over (hierarchy time_hier offset 1 across ancestor at level year)",
            "lag_diff_percent(sales) over (hierarchy time_hier offset 1 across ancestor at level quarter)",
            "lead_diff(sales) over (hierarchy time_hier offset 1)", "lead_diff_percent(sales) over (hierarchy time_hier offset 1)",
            "lag(sales) over (hierarchy time_hier offset 1 within level)", "lag(sales) over (hierarchy time_hier offset 1 within parent)",
            "lag(sales) over (hierarchy time_hier offset 1 across ancestor at level year position from beginning)",
            "lag(sales) over (hierarchy time_hier offset 1 across ancestor at level year position from end)",
            "lag(sales) over (hierarchy d.time_hier offset 1 + 1)", "lag(sales) over (hierarchy time_hier offset (select 1 from dual))",
            "sales - lag(sales) over (hierarchy time_hier offset 1)", "nvl(lag(sales) over (hierarchy time_hier offset 1), 0)",
            "lag(lag(sales) over (hierarchy time_hier offset 1)) over (hierarchy time_hier offset 1)",
            "case when sales > 1 then 1 else 0 end", "sales * 2",
        ).forEach { assertThat(p).describedAs(it).matches(measure(it)) }
    }

    @Test
    fun matchesHierarchyRankMeasures() {
        listOf(
            "rank() over (hierarchy time_hier order by sales)", "dense_rank() over (hierarchy time_hier order by units)",
            "average_rank() over (hierarchy time_hier order by units)", "row_number() over (hierarchy time_hier order by units)",
            "rank() over (hierarchy time_hier order by units desc nulls last within level)",
            "rank() over (hierarchy time_hier order by units within parent)",
            "rank() over (hierarchy time_hier order by units within ancestor at level year)",
            "rank() over (hierarchy time_hier order by units, sales desc)",
            "rank() over (hierarchy time_hier order by units within level include when null)",
            "rank() over (hierarchy time_hier order by units skip when null)",
            "rank() over (hierarchy time_hier order by sales) + 1",
            "rank() over (hierarchy time_hier order by lag(sales) over (hierarchy time_hier offset 1))",
        ).forEach { assertThat(p).describedAs(it).matches(measure(it)) }
    }

    @Test
    fun rejectsMalformedHierarchyOverClauses() {
        listOf(
            "lag(sales) over (hierarchy time_hier)", "lag(sales) over (hierarchy offset 1)", "lag(sales) over (hierarchy time_hier offset)",
            "lag(sales) over (hierarchy time_hier offset 1 within level within parent)",
            "lag(sales) over (hierarchy time_hier offset 1 within level across ancestor at level year)",
            "lag(sales) over (hierarchy time_hier offset 1 position from end)",
            "lag(sales) over (hierarchy time_hier offset 1 across ancestor level year)",
            "lag(sales) over (hierarchy time_hier offset 1 across level year)",
            "lag(sales) over (hierarchy time_hier order by sales)", "lag(sales) over (hierarchy d.t.time_hier offset 1)",
            "lag(sales) over (hierarchy time_hier as th offset 1)", "lag(sales) over (hierarchy time_hier default offset 1)",
            "rank() over (hierarchy time_hier offset 1)", "rank() over (hierarchy time_hier within level)", "rank(1) over (hierarchy time_hier order by units)",
            "rank() over (hierarchy time_hier order by units within ancestor level year)",
            "rank() over (hierarchy time_hier order by units include null)",
            "rank() over (hierarchy time_hier order by units skip when null within level)",
            "rank() over (hierarchy time_hier order by units within level within parent)",
            "sum(sales) over (hierarchy time_hier offset 1)", "sales over (hierarchy time_hier offset 1)",
        ).forEach { assertThat(p).describedAs(it).notMatches(measure(it)) }
        assertThat(p).describedAs("outside a calculated measure").notMatches("select lag(sales) over (hierarchy h offset 1) from t")
    }

    @Test
    fun buildsCalculatedMeasureStructure() {
        val measure = p.parse(measure("rank() over (hierarchy geography_hier order by units desc nulls last within level) + 1"))
            .getFirstDescendant(AnalyticViewGrammar.ANALYTIC_VIEW_MEASURE)
        assertThatAst(measure.firstChild.tokenOriginalValue).isEqualTo("m")
        val clause = measure.getFirstChild(AnalyticViewGrammar.CALC_MEASURE_CLAUSE)
        val rank = clause.getFirstDescendant(AnalyticViewGrammar.AV_RANK_EXPRESSION)
        assertThatAst(rank.texts()).containsExactly(
            "rank", "(", ")", "over", "(", "hierarchy", "geography_hier", "order", "by", "units", "desc", "nulls", "last",
            "within", "level", ")")
        val rankClause = rank.getFirstChild(AnalyticViewGrammar.AV_RANK_CLAUSE)
        assertThatAst(rankClause.getFirstChild(AnalyticViewGrammar.HIERARCHY_REFERENCE).tokenOriginalValue).isEqualTo("geography_hier")
        assertThatAst(rankClause.getFirstChild(DmlGrammar.ORDER_BY_ITEM).texts()).containsExactly("units", "desc", "nulls", "last")
        assertThatAst(clause.getDescendants(DmlGrammar.ANALYTIC_CLAUSE)).isEmpty()

        val lag = p.parse(measure("lag_diff(sales) over (hierarchy time_hier offset 1 across ancestor at level year position from end)"))
            .getFirstDescendant(AnalyticViewGrammar.AV_LEAD_LAG_EXPRESSION)
        assertThatAst(lag.getFirstChild(AnalyticViewGrammar.AV_LEAD_LAG_CLAUSE).texts()).containsExactly(
            "hierarchy", "time_hier", "offset", "1", "across", "ancestor", "at", "level", "year", "position", "from", "end")
    }

    @Test
    fun keepsOrdinaryAnalyticClausesUnchanged() {
        listOf(
            "select lag(sales) over (partition by region order by day) from t",
            "select lag(sales, 1, 0) over (order by day) from t", "select rank() over (order by sales desc) from t",
            "select count(*) over (hierarchy order by x) from t", "select sum(x) over w from t window w as (partition by y)",
        ).forEach { assertThat(p).describedAs(it).matches(it) }

        val ordinary = p.parse("select lag(sales) over (partition by region order by day) from t")
        assertThatAst(ordinary.getDescendants(DmlGrammar.ANALYTIC_CLAUSE)).hasSize(1)
        assertThatAst(ordinary.getDescendants(AnalyticViewGrammar.AV_LEAD_LAG_EXPRESSION)).isEmpty()

        val inMeasure = p.parse(measure("lag(sales) over (hierarchy h offset 1) + sum(x) over (partition by y)"))
        assertThatAst(inMeasure.getDescendants(AnalyticViewGrammar.AV_LEAD_LAG_EXPRESSION)).hasSize(1)
        assertThatAst(inMeasure.getDescendants(DmlGrammar.ANALYTIC_CLAUSE)).hasSize(1)
    }

    @Test
    fun admitsAnalyticViewAsUpdateAndDeleteTarget() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        listOf(
            "update analytic view (using sales_av) set a = 1;", "delete from analytic view (using sales_av);",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }
}
