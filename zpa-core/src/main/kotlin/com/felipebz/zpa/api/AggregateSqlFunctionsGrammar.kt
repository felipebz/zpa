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

import com.felipebz.flr.api.TokenType
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder
import com.felipebz.zpa.api.DmlGrammar.ORDER_BY_CLAUSE
import com.felipebz.zpa.api.DmlGrammar.PARTITION_BY_CLAUSE
import com.felipebz.zpa.api.DmlGrammar.ORDER_BY_ITEM
import com.felipebz.zpa.api.PlSqlGrammar.EXPRESSION
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*

enum class AggregateSqlFunctionsGrammar : GrammarRuleKey {

    LISTAGG_EXPRESSION,
    PERCENTILE_DISC_EXPRESSION,
    PERCENTILE_CONT_EXPRESSION,
    RANK_AGGREGATE_EXPRESSION,
    DENSE_RANK_AGGREGATE_EXPRESSION,
    CUME_DIST_AGGREGATE_EXPRESSION,
    PERCENT_RANK_AGGREGATE_EXPRESSION,
    APPROX_COUNT_EXPRESSION,
    APPROX_SUM_EXPRESSION,
    APPROX_MEDIAN_EXPRESSION,
    APPROX_PERCENTILE_EXPRESSION,
    APPROX_RANK_EXPRESSION,
    CLUSTER_DETAILS_EXPRESSION,
    CLUSTER_DISTANCE_EXPRESSION,
    CLUSTER_ID_EXPRESSION,
    CLUSTER_PROBABILITY_EXPRESSION,
    CLUSTER_SET_EXPRESSION,
    FEATURE_COMPARE_EXPRESSION,
    FEATURE_DETAILS_EXPRESSION,
    FEATURE_ID_EXPRESSION,
    FEATURE_SET_EXPRESSION,
    FEATURE_VALUE_EXPRESSION,
    ORA_DM_PARTITION_NAME_EXPRESSION,
    PREDICTION_EXPRESSION,
    PREDICTION_BOUNDS_EXPRESSION,
    PREDICTION_COST_EXPRESSION,
    PREDICTION_DETAILS_EXPRESSION,
    PREDICTION_PROBABILITY_EXPRESSION,
    PREDICTION_SET_EXPRESSION,
    FILTER_CLAUSE,
    XMLAGG_EXPRESSION,
    COLLECT_EXPRESSION,
    JSON_ARRAYAGG_EXPRESSION,
    JSON_OBJECTAGG_EXPRESSION,
    AGGREGATE_SQL_FUNCTION;

    companion object {
        internal val ALTERNATIVES: List<FunctionAlternative> = listOf(
            FunctionAlternative(LISTAGG_EXPRESSION, LISTAGG),
            FunctionAlternative(CLUSTER_DETAILS_EXPRESSION, CLUSTER_DETAILS),
            FunctionAlternative(CLUSTER_DISTANCE_EXPRESSION, CLUSTER_DISTANCE),
            FunctionAlternative(CLUSTER_ID_EXPRESSION, CLUSTER_ID),
            FunctionAlternative(CLUSTER_PROBABILITY_EXPRESSION, CLUSTER_PROBABILITY),
            FunctionAlternative(CLUSTER_SET_EXPRESSION, CLUSTER_SET),
            FunctionAlternative(FEATURE_COMPARE_EXPRESSION, FEATURE_COMPARE),
            FunctionAlternative(FEATURE_DETAILS_EXPRESSION, FEATURE_DETAILS),
            FunctionAlternative(FEATURE_ID_EXPRESSION, FEATURE_ID),
            FunctionAlternative(FEATURE_SET_EXPRESSION, FEATURE_SET),
            FunctionAlternative(FEATURE_VALUE_EXPRESSION, FEATURE_VALUE),
            FunctionAlternative(ORA_DM_PARTITION_NAME_EXPRESSION, ORA_DM_PARTITION_NAME),
            FunctionAlternative(PREDICTION_EXPRESSION, PREDICTION),
            FunctionAlternative(PREDICTION_BOUNDS_EXPRESSION, PREDICTION_BOUNDS),
            FunctionAlternative(PREDICTION_COST_EXPRESSION, PREDICTION_COST),
            FunctionAlternative(PREDICTION_DETAILS_EXPRESSION, PREDICTION_DETAILS),
            FunctionAlternative(PREDICTION_PROBABILITY_EXPRESSION, PREDICTION_PROBABILITY),
            FunctionAlternative(PREDICTION_SET_EXPRESSION, PREDICTION_SET),
            FunctionAlternative(PERCENTILE_DISC_EXPRESSION, PERCENTILE_DISC),
            FunctionAlternative(PERCENTILE_CONT_EXPRESSION, PERCENTILE_CONT),
            FunctionAlternative(RANK_AGGREGATE_EXPRESSION, RANK),
            FunctionAlternative(DENSE_RANK_AGGREGATE_EXPRESSION, DENSE_RANK),
            FunctionAlternative(CUME_DIST_AGGREGATE_EXPRESSION, CUME_DIST),
            FunctionAlternative(PERCENT_RANK_AGGREGATE_EXPRESSION, PERCENT_RANK),
            FunctionAlternative(APPROX_COUNT_EXPRESSION, APPROX_COUNT),
            FunctionAlternative(APPROX_SUM_EXPRESSION, APPROX_SUM),
            FunctionAlternative(APPROX_MEDIAN_EXPRESSION, APPROX_MEDIAN),
            FunctionAlternative(APPROX_PERCENTILE_EXPRESSION, APPROX_PERCENTILE),
            FunctionAlternative(APPROX_RANK_EXPRESSION, APPROX_RANK),
            FunctionAlternative(XMLAGG_EXPRESSION, XMLAGG),
            FunctionAlternative(COLLECT_EXPRESSION, COLLECT),
            FunctionAlternative(JSON_ARRAYAGG_EXPRESSION, JSON_ARRAYAGG),
            FunctionAlternative(JSON_OBJECTAGG_EXPRESSION, JSON_OBJECTAGG),
        )

        /**
         * Data mining functions carry their own OVER clause, so the generic
         * analytic clause must not be applied after them.
         */
        val miningFunctions: Array<GrammarRuleKey> = arrayOf(
            CLUSTER_DETAILS_EXPRESSION, CLUSTER_DISTANCE_EXPRESSION, CLUSTER_ID_EXPRESSION,
            CLUSTER_PROBABILITY_EXPRESSION, CLUSTER_SET_EXPRESSION, FEATURE_COMPARE_EXPRESSION,
            FEATURE_DETAILS_EXPRESSION, FEATURE_ID_EXPRESSION, FEATURE_SET_EXPRESSION,
            FEATURE_VALUE_EXPRESSION, ORA_DM_PARTITION_NAME_EXPRESSION, PREDICTION_EXPRESSION,
            PREDICTION_BOUNDS_EXPRESSION, PREDICTION_COST_EXPRESSION, PREDICTION_DETAILS_EXPRESSION,
            PREDICTION_PROBABILITY_EXPRESSION, PREDICTION_SET_EXPRESSION
        )

        /**
         * Functions whose own syntax ends the call: Oracle rejects an OVER or KEEP suffix after
         * them (ORA-00923), so the generic analytic suffix must not be applied either. Their
         * optional FILTER clause is part of the function rule.
         */
        val functionsWithoutAnalyticSuffix: Array<GrammarRuleKey> = miningFunctions + arrayOf(
            RANK_AGGREGATE_EXPRESSION, DENSE_RANK_AGGREGATE_EXPRESSION,
            CUME_DIST_AGGREGATE_EXPRESSION, PERCENT_RANK_AGGREGATE_EXPRESSION,
            APPROX_COUNT_EXPRESSION, APPROX_SUM_EXPRESSION, APPROX_MEDIAN_EXPRESSION,
            APPROX_PERCENTILE_EXPRESSION, APPROX_RANK_EXPRESSION
        )

        val admissionTokens: Array<TokenType> =
            ALTERNATIVES.flatMap { it.admissionTokens }.distinct().toTypedArray()

        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(FILTER_CLAUSE).define(
                FILTER, LPARENTHESIS, WHERE, ConditionsGrammar.CONDITION, RPARENTHESIS)

            b.rule(LISTAGG_EXPRESSION).define(
                    LISTAGG,
                    LPARENTHESIS, b.optional(b.firstOf(ALL, DISTINCT, UNIQUE)), EXPRESSION, b.optional(COMMA, EXPRESSION),
                    b.optional(ON, OVERFLOW, b.firstOf(
                            ERROR,
                            b.sequence(
                                TRUNCATE,
                                b.optional(b.nextNot(b.firstOf(WITH, WITHOUT)), EXPRESSION),
                                b.optional(b.firstOf(WITH, WITHOUT), COUNT)))),
                    RPARENTHESIS,
                    b.optional(WITHIN, GROUP, LPARENTHESIS,
                        b.nextNot(b.sequence(ORDER, SIBLINGS)), ORDER_BY_CLAUSE, RPARENTHESIS))

            fun percentileSyntax(function: TokenType) = b.sequence(
                function, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                WITHIN, GROUP, LPARENTHESIS, ORDER, BY, ORDER_BY_ITEM, RPARENTHESIS
            )
            b.rule(PERCENTILE_DISC_EXPRESSION).define(percentileSyntax(PERCENTILE_DISC))
            b.rule(PERCENTILE_CONT_EXPRESSION).define(percentileSyntax(PERCENTILE_CONT))

            buildHypotheticalSetFunctions(b)
            buildApproximateFunctions(b)

            buildMiningFunctions(b)

            b.rule(XMLAGG_EXPRESSION).define(
                XMLAGG, LPARENTHESIS,
                EXPRESSION, b.optional(ORDER_BY_CLAUSE),
                RPARENTHESIS
            )

            b.rule(COLLECT_EXPRESSION).define(
                COLLECT, LPARENTHESIS,
                b.optional(b.firstOf(ALL, DISTINCT, UNIQUE)),
                EXPRESSION,
                // COLLECT uses an ORDER BY list but does not allow ORDER SIBLINGS BY.
                b.optional(ORDER, BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM)),
                RPARENTHESIS,
                b.optional(FILTER_CLAUSE)
            )

            b.rule(JSON_ARRAYAGG_EXPRESSION).define(
                JSON_ARRAYAGG, LPARENTHESIS,
                EXPRESSION, b.optional(FORMAT, JSON),
                b.optional(ORDER_BY_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_ON_NULL_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_RETURNING_CLAUSE),
                b.optional(PRETTY),
                b.optional(ASCII),
                b.optional(STRICT),
                RPARENTHESIS
            )

            b.rule(JSON_OBJECTAGG_EXPRESSION).define(
                JSON_OBJECTAGG, LPARENTHESIS,
                b.optional(KEY), EXPRESSION, b.firstOf(VALUE, IS), EXPRESSION,
                b.optional(FORMAT, JSON),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_ON_NULL_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_RETURNING_CLAUSE),
                b.optional(PRETTY),
                b.optional(ASCII),
                b.optional(STRICT),
                b.optional(WITH, UNIQUE, KEYS),
                RPARENTHESIS
            )

            b.rule(AGGREGATE_SQL_FUNCTION).define(
                b.firstOf(ALTERNATIVES.map { it.ruleKey })
            )
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/RANK.html (and DENSE_RANK,
        // CUME_DIST, PERCENT_RANK). The analytic forms, such as RANK() OVER (...), stay generic calls: a
        // hypothetical-set call needs at least one argument and WITHIN GROUP. Oracle compares argument and
        // ORDER BY item counts only after parsing (ORA-00909). FILTER is accepted after RANK and DENSE_RANK
        // only (ORA-00923 for the others).
        private fun buildHypotheticalSetFunctions(b: PlSqlGrammarBuilder) {
            fun hypotheticalSet(function: TokenType, vararg suffix: Any) = b.sequence(
                function, LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS,
                withinGroupOrderBy(b), *suffix
            )
            b.rule(RANK_AGGREGATE_EXPRESSION).define(hypotheticalSet(RANK, b.optional(FILTER_CLAUSE)))
            b.rule(DENSE_RANK_AGGREGATE_EXPRESSION).define(hypotheticalSet(DENSE_RANK, b.optional(FILTER_CLAUSE)))
            b.rule(CUME_DIST_AGGREGATE_EXPRESSION).define(hypotheticalSet(CUME_DIST))
            b.rule(PERCENT_RANK_AGGREGATE_EXPRESSION).define(hypotheticalSet(PERCENT_RANK))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Aggregate-Functions.html
        // The optional second argument ('MAX_ERROR', 'ERROR_RATE', 'CONFIDENCE') is an expression:
        // Oracle validates its value only after parsing (ORA-01760).
        private fun buildApproximateFunctions(b: PlSqlGrammarBuilder) {
            val optionalSecondArgument = b.optional(COMMA, EXPRESSION)

            b.rule(APPROX_COUNT_EXPRESSION).define(
                APPROX_COUNT, LPARENTHESIS, b.firstOf(MULTIPLICATION, EXPRESSION), optionalSecondArgument, RPARENTHESIS,
                b.optional(FILTER_CLAUSE)
            )

            // Unlike APPROX_COUNT, APPROX_SUM(*) is a syntax error (ORA-00936) despite the diagram.
            b.rule(APPROX_SUM_EXPRESSION).define(
                APPROX_SUM, LPARENTHESIS, b.nextNot(MULTIPLICATION), EXPRESSION, optionalSecondArgument, RPARENTHESIS,
                b.optional(FILTER_CLAUSE)
            )

            val deterministicArguments = b.sequence(
                LPARENTHESIS, EXPRESSION, b.optional(DETERMINISTIC), optionalSecondArgument, RPARENTHESIS
            )

            b.rule(APPROX_MEDIAN_EXPRESSION).define(
                APPROX_MEDIAN, deterministicArguments, b.optional(FILTER_CLAUSE)
            )

            // A single ORDER BY item: a second one is a syntax error (ORA-00909).
            b.rule(APPROX_PERCENTILE_EXPRESSION).define(
                APPROX_PERCENTILE, deterministicArguments,
                WITHIN, GROUP, LPARENTHESIS, ORDER, BY, ORDER_BY_ITEM, RPARENTHESIS,
                b.optional(FILTER_CLAUSE)
            )

            // Oracle 26 rejects the documented leading expr (ORA-00907) and requires ORDER BY after an
            // optional PARTITION BY. A single DESC item is checked after parsing (ORA-62231/ORA-62233).
            b.rule(APPROX_RANK_EXPRESSION).define(
                APPROX_RANK, LPARENTHESIS,
                b.optional(PARTITION_BY_CLAUSE),
                b.nextNot(ORDER, SIBLINGS), ORDER_BY_CLAUSE,
                RPARENTHESIS,
                b.optional(FILTER_CLAUSE)
            )
        }

        private fun withinGroupOrderBy(b: PlSqlGrammarBuilder) = b.sequence(
            WITHIN, GROUP, LPARENTHESIS, b.nextNot(ORDER, SIBLINGS), ORDER_BY_CLAUSE, RPARENTHESIS
        )

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Data-Mining-Functions.html
        // Keep the shared mining helpers anonymous so each function rule adds only its own AST boundary.
        private fun buildMiningFunctions(b: PlSqlGrammarBuilder) {
            val modelName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val miningAttribute = b.firstOf(
                b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME), DOT, MULTIPLICATION),
                b.sequence(EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME))
            )
            val miningAttributeClause = b.sequence(
                USING,
                b.firstOf(MULTIPLICATION, b.sequence(miningAttribute, b.zeroOrMore(COMMA, miningAttribute)))
            )
            val oneOptionalArgument = b.optional(COMMA, EXPRESSION)
            val twoOptionalArguments = b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION))
            val detailsOrder = b.optional(b.firstOf(DESC, ASC, ABS))

            // Oracle 26 requires at least two classes, two rows and two costs per row (ORA-02000).
            fun atLeastTwo(item: Any) = b.sequence(item, b.oneOrMore(COMMA, item))
            val costRow = b.sequence(LPARENTHESIS, atLeastTwo(EXPRESSION), RPARENTHESIS)
            val costMatrixClause = b.sequence(
                COST,
                b.firstOf(
                    b.sequence(MODEL, b.optional(AUTO)),
                    b.sequence(
                        LPARENTHESIS, atLeastTwo(EXPRESSION), RPARENTHESIS,
                        VALUES, LPARENTHESIS, atLeastTwo(costRow), RPARENTHESIS)))

            // Model forms accept only OVER (ORDER BY ...): Oracle 26 rejects OVER (), PARTITION BY, window
            // names and frames there (ORA-02000). The SQLRF diagrams show this only for PREDICTION*, but
            // Oracle 26 parses it for every model form except ORA_DM_PARTITION_NAME.
            val miningOrderByClause = b.sequence(
                b.nextNot(b.sequence(ORDER, SIBLINGS)),
                ORDER, BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM)
            )
            val miningOrderedClause = b.sequence(OVER, LPARENTHESIS, miningOrderByClause, RPARENTHESIS)
            val miningAnalyticClause = b.sequence(
                OVER,
                b.firstOf(
                    IDENTIFIER_NAME,
                    b.sequence(
                        LPARENTHESIS,
                        b.optional(b.nextNot(b.firstOf(PARTITION, ORDER)), IDENTIFIER_NAME),
                        b.optional(DmlGrammar.PARTITION_BY_CLAUSE),
                        b.optional(miningOrderByClause),
                        RPARENTHESIS
                    )
                )
            )

            fun modelForm(function: TokenType, vararg arguments: Any) = b.sequence(
                function, LPARENTHESIS, modelName, *arguments, RPARENTHESIS, b.optional(miningOrderedClause))

            // The analytic forms build a transient model and always require OVER (ORA-30484).
            fun analyticForm(function: TokenType, head: Any, vararg arguments: Any) = b.sequence(
                function, LPARENTHESIS, head, *arguments, RPARENTHESIS, miningAnalyticClause)

            val into = b.sequence(INTO, EXPRESSION)
            val predictionTarget = b.firstOf(b.sequence(OF, ANOMALY), b.sequence(FOR, EXPRESSION))

            fun clusterOrFeature(rule: AggregateSqlFunctionsGrammar, function: TokenType, vararg arguments: Any) =
                b.rule(rule).define(b.firstOf(
                    modelForm(function, *arguments),
                    analyticForm(function, into, *arguments)))

            fun prediction(rule: AggregateSqlFunctionsGrammar, function: TokenType, vararg arguments: Any) =
                b.rule(rule).define(b.firstOf(
                    modelForm(function, *arguments),
                    analyticForm(function, predictionTarget, *arguments)))

            clusterOrFeature(CLUSTER_DETAILS_EXPRESSION, CLUSTER_DETAILS,
                twoOptionalArguments, detailsOrder, miningAttributeClause)
            clusterOrFeature(CLUSTER_DISTANCE_EXPRESSION, CLUSTER_DISTANCE, oneOptionalArgument, miningAttributeClause)
            clusterOrFeature(CLUSTER_ID_EXPRESSION, CLUSTER_ID, miningAttributeClause)
            clusterOrFeature(CLUSTER_PROBABILITY_EXPRESSION, CLUSTER_PROBABILITY, oneOptionalArgument, miningAttributeClause)
            clusterOrFeature(CLUSTER_SET_EXPRESSION, CLUSTER_SET, twoOptionalArguments, miningAttributeClause)
            clusterOrFeature(FEATURE_DETAILS_EXPRESSION, FEATURE_DETAILS,
                twoOptionalArguments, detailsOrder, miningAttributeClause)
            clusterOrFeature(FEATURE_ID_EXPRESSION, FEATURE_ID, miningAttributeClause)
            clusterOrFeature(FEATURE_SET_EXPRESSION, FEATURE_SET, twoOptionalArguments, miningAttributeClause)
            clusterOrFeature(FEATURE_VALUE_EXPRESSION, FEATURE_VALUE, oneOptionalArgument, miningAttributeClause)

            // Oracle 26 rejects an analytic FEATURE_COMPARE (ORA-30483) and any OVER after
            // ORA_DM_PARTITION_NAME (ORA-00923).
            b.rule(FEATURE_COMPARE_EXPRESSION).define(
                modelForm(FEATURE_COMPARE, miningAttributeClause, AND, miningAttributeClause))
            b.rule(ORA_DM_PARTITION_NAME_EXPRESSION).define(
                ORA_DM_PARTITION_NAME, LPARENTHESIS, modelName, miningAttributeClause, RPARENTHESIS)

            prediction(PREDICTION_EXPRESSION, PREDICTION, b.optional(costMatrixClause), miningAttributeClause)
            // The analytic PREDICTION_BOUNDS form is undocumented, but Oracle 26 parses it and requires OVER.
            prediction(PREDICTION_BOUNDS_EXPRESSION, PREDICTION_BOUNDS, twoOptionalArguments, miningAttributeClause)
            // PREDICTION_COST without a cost matrix fails with ORA-40283.
            prediction(PREDICTION_COST_EXPRESSION, PREDICTION_COST,
                oneOptionalArgument, costMatrixClause, miningAttributeClause)
            prediction(PREDICTION_DETAILS_EXPRESSION, PREDICTION_DETAILS,
                twoOptionalArguments, detailsOrder, miningAttributeClause)
            prediction(PREDICTION_PROBABILITY_EXPRESSION, PREDICTION_PROBABILITY,
                oneOptionalArgument, miningAttributeClause)
            prediction(PREDICTION_SET_EXPRESSION, PREDICTION_SET,
                twoOptionalArguments, b.optional(costMatrixClause), miningAttributeClause)
        }
    }

}
