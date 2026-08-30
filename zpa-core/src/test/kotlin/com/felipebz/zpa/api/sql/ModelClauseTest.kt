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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.MODEL_CLAUSE)
    }

    @Test
    fun matchesOracleModelExample() {
        assertThat(p).matches(
            """
            model
              partition by (country)
              dimension by (prod, year)
              measures (sale s)
              ignore nav
              unique dimension
              rules upsert sequential order
              (
                s[prod = 'Mouse Pad', year = 2001] =
                  s['Mouse Pad', 1999] + s['Mouse Pad', 2000],
                s['Standard Mouse', 2002] = s['Standard Mouse', 2001]
              )
            """
        )
    }

    @Test
    fun matchesReturnRowsAndReferenceModel() {
        assertThat(p).matches(
            """
            model
              keep nav
              return updated rows
              reference previous_values on (select year, value from history)
                dimension by (year)
                measures (value)
                unique single reference
              main current_values
                dimension by (year)
                measures (value)
                rules (value[2025] = previous_values[2024])
            """
        )
    }

    @Test
    fun matchesModelRulesOptionsAndIterate() {
        assertThat(p).matches(
            """
            model
              dimension by (year)
              measures (value)
              rules iterate (100) until (previous(value[1]) > value[1])
              (
                update value[1] order by year = value[1] / 2,
                upsert all value[2] = value[1]
              )
            """
        )
    }

    @Test
    fun matchesModelForLoopsAndPresentCondition() {
        assertThat(p).matches(
            """
            model
              dimension by (year, month)
              measures (value)
              rules
              (
                value[for year in (2024, 2025)] = 1,
                value[for year like '202%' from 2024 to 2026 increment 1] = 1,
                value[for year from 2026 to 2024 decrement 1] = 1,
                value[for (year, month) in ((2024, 1), (2024, 2))] =
                  case when value[2023, 12] is present then value[2023, 12] else 0 end
              )
            """
        )
    }

    @Test
    fun matchesOptionalRulesPartsAndForLoopSubqueries() {
        assertThat(p).matches(
            """
            model
              return all rows
              dimension by (year, month)
              measures (value)
              rules update automatic order
              (
                value[for year in (select year from years)] = 1,
                value[for (year, month) in (select year, month from year_months)] = 1
              )
            """
        )

        assertThat(p).matches(
            """
            model
              dimension by (year)
              measures (value)
              (
                value[1] = 1
              )
            """
        )
    }

    @Test
    fun matchesModelInSelectExpression() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select country, prod, year, s
              from sales_view_ref
              model
                partition by (country)
                dimension by (prod, year)
                measures (sale s)
                rules (s['Mouse Pad', 2001] = s['Mouse Pad', 2000])
              order by country, prod, year
            """
        )
    }

    @Test
    fun matchesModelWithAnalyticExpression() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select country, year, sale, csum
              from (
                select country, year, sum(sale) sale
                  from sales_view_ref
                 group by country, year
              )
              model dimension by (country, year)
                    measures (sale, 0 csum)
                    rules (
                      csum[any, any] =
                        sum(sale) over (
                          partition by country
                          order by year
                          rows unbounded preceding
                        )
                    )
            order by country, year
            """
        )
    }

    @Test
    fun matchesAggregateCellReference() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select country, prod, year, value
              from sales_view_ref
              model dimension by (prod, year)
                    measures (value)
                    rules (
                      value[2024] =
                        sum(value)['Mouse Pad', year between cv() - 2 and cv() - 1]
                    )
            """
        )
    }

    @Test
    fun matchesModelForLoopAndIterationFunctions() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select country, prod, year, s
              from sales_view_ref
              model
                partition by (country)
                dimension by (prod, year)
                measures (sale s)
                rules upsert sequential order iterate (2)
                (
                  s[for prod in ('Mouse Pad', 'Standard Mouse'), 2001] =
                    s[cv(), 1999] + s[cv(), 2000],
                  s['Mouse Pad', 2001 + iteration_number] =
                    s['Mouse Pad', 1998 + iteration_number]
                )
            order by country, prod, year
            """
        )
    }

    @Test
    fun matchesModelPresentValueFunctions() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            """
            select country, prod, year, s
              from sales_view_ref
              model
                partition by (country)
                dimension by (prod, year)
                measures (sale s)
                rules (
                  s['Mouse Pad', 2001] = presentv(s['Mouse Pad', 2000], s['Mouse Pad', 2000], 0),
                  s['Mouse Pad', 2002] = presentnnv(s['Mouse Pad', 2002], s['Mouse Pad', 2002], 10)
                )
            order by country, prod, year
            """
        )
    }

    @Test
    fun keepsModelOnlySyntaxOutOfOrdinaryExpressionsAndConditions() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).notMatches("select measure[2024] from values_table")
        assertThat(p).notMatches("select sum(value)[2024] from values_table")
        assertThat(p).notMatches("select value from values_table where measure[2024] = 1")
        assertThat(p).notMatches("select value from values_table where value is present")
        assertThat(p).notMatches("select country from values_table group by country having value is not present")

        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
        assertThat(p).notMatches("case when value is present then 1 else 0 end")
    }

    @Test
    fun doesNotLeakModelContextIntoNestedQueries() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).notMatches(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = (select value from history where value is present))"
        )

        val node = p.parse(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = (select t.data[0] from tab t) + value[2])"
        )
        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).hasSize(2)
    }

    @Test
    fun allowsAnIndependentModelInsideANestedQuery() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = (select value from history model dimension by (year) "
                + "measures (value) rules (value[1] = value[2])))"
        )
    }

    @Test
    fun isolatesModelContextFromInExistsAndForLoopSubqueries() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).notMatches(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = value[2] in "
                + "(select value from history where value is present))"
        )
        assertThat(p).notMatches(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = case when exists "
                + "(select value from history where value is present) "
                + "then value[2] else 0 end)"
        )
        assertThat(p).notMatches(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[for year in "
                + "(select year from years where value is present)] = 1)"
        )
    }

    @Test
    fun acceptsJsonArrayStepsInOrdinaryExpressions() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        assertThat(p).matches("select t.data.a[*].sum() from tab t")
        assertThat(p).matches("select t.data[0] from tab t")

        val node = p.parse("select t.data.a[*].sum() from tab t")
        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).isEmpty()
    }

    @Test
    fun acceptsUnambiguousJsonAccessInsideModelExpressions() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        val node = p.parse(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = t.data.a[*].sum())"
        )

        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).hasSize(1)
        assertThat(node.getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ACCESS_EXPRESSION)).hasSize(1)
    }

    @Test
    fun keepsAmbiguousTwoPartBracketAccessOnTheModelMemberPath() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        val node = p.parse(
            "select value from values_table model dimension by (year) measures (value) "
                + "rules (value[1] = t.data[0])"
        )

        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).hasSize(2)
    }

    @Test
    fun keepsModelFunctionNamesGenericAtSyntaxLevel() {
        setRootRule(PlSqlGrammar.EXPRESSION)

        assertThat(p).matches("cv()")
        assertThat(p).matches("presentv(value, fallback, 0)")
        assertThat(p).matches("presentnnv(value, fallback, 0)")
        assertThat(p).matches("previous(value)")
        assertThat(p).matches("iteration_number")
    }

    @Test
    fun exposesModelAstBoundaries() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        val node = p.parse(
            "select value from values_table "
                + "model dimension by (year) measures (value) "
                + "rules (value[1] = value[2])"
        )

        assertThat(node.getDescendants(DmlGrammar.MODEL_CLAUSE)).hasSize(1)
        assertThat(node.getDescendants(DmlGrammar.MODEL_RULE)).hasSize(1)
        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).hasSize(2)
    }

    @Test
    fun preservesReferenceModelCellReferenceAst() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)

        val node = p.parse(
            "select value from values_table "
                + "model reference ref on (select year, value from history) "
                + "dimension by (year) measures (value) "
                + "dimension by (year) measures (value) "
                + "rules (value[2025] = ref.value[2024])"
        )

        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE_SUFFIX)).hasSize(2)
        assertThat(node.getDescendants(DmlGrammar.MODEL_CELL_REFERENCE)).hasSize(1)
        assertThat(node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION)).isNotEmpty()
    }

    @Test
    fun rejectsMissingModelColumns() {
        assertThat(p).notMatches("model rules (value[1] = 1)")
    }

    @Test
    fun rejectsEmptyCellReference() {
        assertThat(p).notMatches("model dimension by (year) measures (value) rules (value[] = 1)")
    }

    @Test
    fun rejectsTrailingCommas() {
        assertThat(p).notMatches("model dimension by (year,) measures (value) rules (value[1] = 1)")
        assertThat(p).notMatches("model dimension by (year) measures (value,) rules (value[1] = 1)")
        assertThat(p).notMatches("model dimension by (year) measures (value) rules (value[1] = 1,)")
        assertThat(p).notMatches(
            "model dimension by (year, month) measures (value) "
                + "rules (value[for (year, month) in ((2024, 1),)] = 1)"
        )
    }

    @Test
    fun rejectsPartitionByInReferenceModel() {
        assertThat(p).notMatches(
            "model reference ref on (select year, value from history) "
                + "partition by (country) dimension by (year) measures (value) "
                + "dimension by (year) measures (value) rules (value[1] = 1)"
        )
    }

    @Test
    fun rejectsSiblingsInModelRuleOrderBy() {
        assertThat(p).notMatches(
            "model dimension by (year) measures (value) "
                + "rules (value[1] order siblings by year = 1)"
        )
    }

}
