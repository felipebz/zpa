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
import com.felipebz.zpa.api.DmlGrammar.ORDER_BY_ITEM
import com.felipebz.zpa.api.PlSqlGrammar.EXPRESSION
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*

enum class AggregateSqlFunctionsGrammar : GrammarRuleKey {

    LISTAGG_EXPRESSION,
    PERCENTILE_DISC_EXPRESSION,
    PERCENTILE_CONT_EXPRESSION,
    CLUSTER_SET_EXPRESSION,
    FILTER_CLAUSE,
    XMLAGG_EXPRESSION,
    COLLECT_EXPRESSION,
    JSON_ARRAYAGG_EXPRESSION,
    JSON_OBJECTAGG_EXPRESSION,
    AGGREGATE_SQL_FUNCTION;

    companion object {
        internal val ALTERNATIVES: List<FunctionAlternative> = listOf(
            FunctionAlternative(LISTAGG_EXPRESSION, LISTAGG),
            FunctionAlternative(CLUSTER_SET_EXPRESSION, CLUSTER_SET),
            FunctionAlternative(PERCENTILE_DISC_EXPRESSION, PERCENTILE_DISC),
            FunctionAlternative(PERCENTILE_CONT_EXPRESSION, PERCENTILE_CONT),
            FunctionAlternative(XMLAGG_EXPRESSION, XMLAGG),
            FunctionAlternative(COLLECT_EXPRESSION, COLLECT),
            FunctionAlternative(JSON_ARRAYAGG_EXPRESSION, JSON_ARRAYAGG),
            FunctionAlternative(JSON_OBJECTAGG_EXPRESSION, JSON_OBJECTAGG),
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

            // The USING clause is shared mining syntax. Keep its helpers
            // anonymous so CLUSTER_SET is the only new AST boundary.
            val miningAttribute = b.firstOf(
                b.sequence(
                    IDENTIFIER_NAME,
                    b.optional(DOT, IDENTIFIER_NAME),
                    DOT,
                    MULTIPLICATION
                ),
                b.sequence(
                    EXPRESSION,
                    b.optional(b.optional(AS), IDENTIFIER_NAME)
                )
            )
            val miningAttributeClause = b.sequence(
                USING,
                b.firstOf(
                    MULTIPLICATION,
                    b.sequence(miningAttribute, b.zeroOrMore(COMMA, miningAttribute))
                )
            )
            val miningArguments = b.sequence(
                b.optional(COMMA, EXPRESSION, b.optional(COMMA, EXPRESSION)),
                miningAttributeClause
            )
            val miningOrderByClause = b.sequence(
                b.nextNot(b.sequence(ORDER, SIBLINGS)),
                ORDER,
                BY,
                ORDER_BY_ITEM,
                b.zeroOrMore(COMMA, ORDER_BY_ITEM)
            )
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
            b.rule(CLUSTER_SET_EXPRESSION).define(
                b.firstOf(
                    b.sequence(
                        CLUSTER_SET,
                        LPARENTHESIS,
                        IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                        miningArguments,
                        RPARENTHESIS
                    ),
                    b.sequence(
                        CLUSTER_SET,
                        LPARENTHESIS,
                        INTO, EXPRESSION,
                        miningArguments,
                        RPARENTHESIS,
                        miningAnalyticClause
                    )
                )
            )

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
    }

}
