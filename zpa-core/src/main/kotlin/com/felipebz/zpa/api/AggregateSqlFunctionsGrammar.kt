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
import com.felipebz.zpa.api.PlSqlGrammar.EXPRESSION
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*

enum class AggregateSqlFunctionsGrammar : GrammarRuleKey {

    LISTAGG_EXPRESSION,
    FILTER_CLAUSE,
    XMLAGG_EXPRESSION,
    COLLECT_EXPRESSION,
    JSON_ARRAYAGG_EXPRESSION,
    JSON_OBJECTAGG_EXPRESSION,
    AGGREGATE_SQL_FUNCTION;

    companion object {
        internal val ALTERNATIVES: List<FunctionAlternative> = listOf(
            FunctionAlternative(LISTAGG_EXPRESSION, LISTAGG),
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
                    LPARENTHESIS, b.optional(b.firstOf(ALL, DISTINCT)), EXPRESSION, b.optional(COMMA, EXPRESSION),
                    b.optional(ON, OVERFLOW, b.firstOf(
                            ERROR,
                            b.sequence(
                                TRUNCATE,
                                b.optional(b.nextNot(b.firstOf(WITH, WITHOUT)), EXPRESSION),
                                b.optional(b.firstOf(WITH, WITHOUT), COUNT)))),
                    RPARENTHESIS,
                    b.optional(WITHIN, GROUP, LPARENTHESIS, DmlGrammar.ORDER_BY_CLAUSE, RPARENTHESIS))

            b.rule(XMLAGG_EXPRESSION).define(
                XMLAGG, LPARENTHESIS,
                EXPRESSION, b.optional(ORDER_BY_CLAUSE),
                RPARENTHESIS
            )

            b.rule(COLLECT_EXPRESSION).define(
                COLLECT, LPARENTHESIS,
                b.optional(b.firstOf(DISTINCT, UNIQUE)),
                EXPRESSION,
                b.optional(ORDER, BY, EXPRESSION),
                RPARENTHESIS
            )

            b.rule(JSON_ARRAYAGG_EXPRESSION).define(
                JSON_ARRAYAGG, LPARENTHESIS,
                EXPRESSION, b.optional(FORMAT, JSON),
                b.optional(ORDER_BY_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_ON_NULL_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_RETURNING_CLAUSE),
                b.optional(STRICT),
                RPARENTHESIS
            )

            b.rule(JSON_OBJECTAGG_EXPRESSION).define(
                JSON_OBJECTAGG, LPARENTHESIS,
                b.optional(KEY), EXPRESSION, VALUE, EXPRESSION,
                b.optional(FORMAT, JSON),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_ON_NULL_CLAUSE),
                b.optional(SingleRowSqlFunctionsGrammar.JSON_RETURNING_CLAUSE),
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
