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

import com.felipebz.flr.grammar.ContextKey
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.api.DmlGrammar.ORDER_BY_ITEM
import com.felipebz.zpa.api.PlSqlGrammar.EXPRESSION
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

internal val ANALYTIC_VIEW_MEASURE_CONTEXT: ContextKey<Boolean> = ContextKey()

enum class AnalyticViewGrammar : GrammarRuleKey {
    HIERARCHY_REFERENCE,
    HIERARCHIES_CLAUSE,
    FILTER_FACT_CLAUSE,
    FILTER_FACT_ITEM,
    CALC_MEASURE_CLAUSE,
    AGGREGATE_BY_CLAUSE,
    BASE_MEASURE_CLAUSE,
    ANALYTIC_VIEW_MEASURE,
    ADD_MEASURES_CLAUSE,
    ANALYTIC_VIEW_QUERY_CLAUSE,
    INLINE_ANALYTIC_VIEW,
    SUBAV_FACTORING_CLAUSE,
    AV_LEAD_LAG_EXPRESSION,
    AV_LEAD_LAG_CLAUSE,
    AV_RANK_EXPRESSION,
    AV_RANK_CLAUSE;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            val qualifiedName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))

            b.rule(HIERARCHY_REFERENCE).define(qualifiedName)

            b.rule(HIERARCHIES_CLAUSE).define(
                HIERARCHIES, LPARENTHESIS,
                b.optional(HIERARCHY_REFERENCE, b.zeroOrMore(COMMA, HIERARCHY_REFERENCE)),
                RPARENTHESIS)

            b.rule(FILTER_FACT_ITEM).define(
                b.firstOf(
                    b.sequence(MEASURES, TO, EXPRESSION),
                    b.sequence(HIERARCHY_REFERENCE, TO, EXPRESSION)))

            b.rule(FILTER_FACT_CLAUSE).define(
                FILTER, FACT, LPARENTHESIS, FILTER_FACT_ITEM, b.zeroOrMore(COMMA, FILTER_FACT_ITEM), RPARENTHESIS)

            b.rule(CALC_MEASURE_CLAUSE).define(
                AS, LPARENTHESIS, b.withContext(ANALYTIC_VIEW_MEASURE_CONTEXT, true, EXPRESSION), RPARENTHESIS)

            b.rule(AGGREGATE_BY_CLAUSE).define(
                AGGREGATE, BY,
                b.firstOf(
                    "APPROX_COUNT_DISTINCT", "APPROX_COUNT_DISTINCT_AGG", "AVG", "COUNT", "MAX", "MIN", "STDDEV",
                    "STDDEV_POP", "STDDEV_SAMP", "SUM", "VAR_POP", "VAR_SAMP", "VARIANCE"))

            b.rule(BASE_MEASURE_CLAUSE).define(FACT, EXPRESSION, AGGREGATE_BY_CLAUSE)

            b.rule(ANALYTIC_VIEW_MEASURE).define(IDENTIFIER_NAME, b.firstOf(BASE_MEASURE_CLAUSE, CALC_MEASURE_CLAUSE))

            b.rule(ADD_MEASURES_CLAUSE).define(
                ADD, MEASURES, LPARENTHESIS, ANALYTIC_VIEW_MEASURE, b.zeroOrMore(COMMA, ANALYTIC_VIEW_MEASURE), RPARENTHESIS)

            b.rule(ANALYTIC_VIEW_QUERY_CLAUSE).define(
                USING, qualifiedName,
                b.optional(HIERARCHIES_CLAUSE),
                b.optional(FILTER_FACT_CLAUSE),
                b.optional(ADD_MEASURES_CLAUSE))

            b.rule(INLINE_ANALYTIC_VIEW).define(ANALYTIC, VIEW, LPARENTHESIS, ANALYTIC_VIEW_QUERY_CLAUSE, RPARENTHESIS)

            b.rule(SUBAV_FACTORING_CLAUSE).define(
                IDENTIFIER_NAME, ANALYTIC, VIEW, AS, LPARENTHESIS, ANALYTIC_VIEW_QUERY_CLAUSE, RPARENTHESIS)

            val levelReference = qualifiedName

            b.rule(AV_LEAD_LAG_CLAUSE).define(
                HIERARCHY, HIERARCHY_REFERENCE, OFFSET, EXPRESSION,
                b.optional(b.firstOf(
                    b.sequence(WITHIN, b.firstOf(LEVEL, PARENT)),
                    b.sequence(
                        ACROSS, ANCESTOR, AT, LEVEL, levelReference,
                        b.optional(POSITION, FROM, b.firstOf(BEGINNING, END))))))

            b.rule(AV_LEAD_LAG_EXPRESSION).define(
                b.firstOf(LAG, LAG_DIFF, LAG_DIFF_PERCENT, LEAD, LEAD_DIFF, LEAD_DIFF_PERCENT),
                LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                OVER, LPARENTHESIS, AV_LEAD_LAG_CLAUSE, RPARENTHESIS)

            b.rule(AV_RANK_CLAUSE).define(
                HIERARCHY, HIERARCHY_REFERENCE,
                ORDER, BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM),
                b.optional(WITHIN, b.firstOf(LEVEL, PARENT, b.sequence(ANCESTOR, AT, LEVEL, levelReference))),
                b.optional(b.firstOf(INCLUDE, SKIP), WHEN, NULL))

            b.rule(AV_RANK_EXPRESSION).define(
                b.firstOf(RANK, DENSE_RANK, AVERAGE_RANK, ROW_NUMBER),
                LPARENTHESIS, RPARENTHESIS,
                OVER, LPARENTHESIS, AV_RANK_CLAUSE, RPARENTHESIS)
        }
    }
}
