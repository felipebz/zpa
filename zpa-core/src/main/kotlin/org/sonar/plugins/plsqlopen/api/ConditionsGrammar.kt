/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api

import com.felipebz.flr.grammar.GrammarRuleKey
import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.CONCATENATION_EXPRESSION
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.OBJECT_REFERENCE
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*

enum class ConditionsGrammar : GrammarRuleKey {

    // internal
    RELATIONAL_OPERATOR,
    IS_JSON_ARGS,
    JSON_MODIFIER_LIST,
    JSON_COLUMN_MODIFIER,
    JSON_SCALAR_MODIFIER,
    JSON_EQUAL_ON_ERROR_CLAUSE,
    JSON_EXISTS_ON_EMPTY_CLAUSE,
    JSON_EXISTS_ON_ERROR_CLAUSE,

    // conditions
    RELATIONAL_CONDITION,
    LIKE_CONDITION,
    BETWEEN_CONDITION,
    MULTISET_CONDITION,
    IS_A_SET_CONDITION,
    IS_EMPTY_CONDITION,
    MEMBER_CONDITION,
    SUBMULTISET_CONDITION,
    IS_OF_CONDITION,
    IS_JSON_CONDITION,
    JSON_EQUAL_CONDITION,
    JSON_EXISTS_CONDITION,
    JSON_TEXTCONTAINS_CONDITION,
    CONDITION;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(RELATIONAL_OPERATOR).define(
                b.firstOf(
                    PlSqlGrammar.EQUALS_OPERATOR,
                    PlSqlGrammar.NOTEQUALS_OPERATOR,
                    PlSqlGrammar.LESSTHANOREQUALS_OPERATOR,
                    PlSqlGrammar.LESSTHAN_OPERATOR,
                    PlSqlGrammar.GREATERTHANOREQUALS_OPERATOR,
                    PlSqlGrammar.GREATERTHAN_OPERATOR
                ),
                b.optional(b.firstOf(ANY, SOME, ALL))
            )

            b.rule(RELATIONAL_CONDITION).define(
                CONCATENATION_EXPRESSION, RELATIONAL_OPERATOR, CONCATENATION_EXPRESSION
            ).skip()

            b.rule(LIKE_CONDITION).define(
                CONCATENATION_EXPRESSION,
                b.optional(NOT), LIKE,
                CONCATENATION_EXPRESSION,
                b.optional(ESCAPE, CONCATENATION_EXPRESSION)
            )

            b.rule(BETWEEN_CONDITION).define(
                CONCATENATION_EXPRESSION,
                b.optional(NOT), BETWEEN,
                CONCATENATION_EXPRESSION, AND, CONCATENATION_EXPRESSION
            ).skip()

            b.rule(IS_A_SET_CONDITION).define(CONCATENATION_EXPRESSION, IS, b.optional(NOT), A, SET)

            b.rule(IS_EMPTY_CONDITION).define(CONCATENATION_EXPRESSION, IS, b.optional(NOT), EMPTY)

            b.rule(MEMBER_CONDITION)
                .define(CONCATENATION_EXPRESSION, b.optional(NOT), MEMBER, b.optional(OF), CONCATENATION_EXPRESSION)
                .skip()

            b.rule(SUBMULTISET_CONDITION).define(
                CONCATENATION_EXPRESSION,
                b.optional(NOT),
                SUBMULTISET,
                b.optional(OF),
                CONCATENATION_EXPRESSION
            )

            //https://docs.oracle.com/cloud/latest/db112/SQLRF/conditions006.htm#SQLRF52128
            b.rule(MULTISET_CONDITION).define(
                b.firstOf(
                    IS_A_SET_CONDITION,
                    IS_EMPTY_CONDITION,
                    MEMBER_CONDITION,
                    SUBMULTISET_CONDITION
                )
            )

            b.rule(IS_OF_CONDITION).define(
                CONCATENATION_EXPRESSION,
                IS,
                b.optional(NOT),
                OF,
                b.optional(TYPE),
                PlSqlPunctuator.LPARENTHESIS,
                b.optional(ONLY),
                OBJECT_REFERENCE,
                b.zeroOrMore(PlSqlPunctuator.COMMA, b.optional(ONLY), OBJECT_REFERENCE),
                PlSqlPunctuator.RPARENTHESIS
            )

            b.rule(IS_JSON_CONDITION).define(
                CONCATENATION_EXPRESSION,
                IS,
                b.optional(NOT),
                JSON,
                b.optional(JSON_MODIFIER_LIST),
                IS_JSON_ARGS
            )

            b.rule(IS_JSON_ARGS).define(
                b.firstOf(
                    b.sequence(
                        VALIDATE, b.optional(CAST), b.optional(USING), PlSqlTokenType.STRING_LITERAL
                    ),
                    b.sequence(
                        b.optional(FORMAT, JSON),
                        b.optional(b.firstOf(STRICT, LAX)),
                        b.optional(b.firstOf(ALLOW, DISALLOW), SCALARS),
                        b.optional(b.firstOf(WITH, WITHOUT), UNIQUE, KEYS)
                    )
                )
            ).skip()

            b.rule(JSON_MODIFIER_LIST).define(
                b.firstOf(
                    b.sequence(
                        PlSqlPunctuator.LPARENTHESIS,
                        JSON_COLUMN_MODIFIER,
                        b.zeroOrMore(PlSqlPunctuator.COMMA, JSON_COLUMN_MODIFIER),
                        PlSqlPunctuator.RPARENTHESIS
                    ),
                    JSON_COLUMN_MODIFIER
                )
            )

            b.rule(JSON_COLUMN_MODIFIER).define(
                b.firstOf(
                    VALUE,
                    ARRAY,
                    OBJECT,
                    b.sequence(SCALAR, b.optional(JSON_SCALAR_MODIFIER))
                )
            )

            b.rule(JSON_SCALAR_MODIFIER).define(
                b.firstOf(
                    NUMBER,
                    STRING,
                    BINARY_DOUBLE,
                    BINARY_FLOAT,
                    DATE,
                    b.sequence(TIMESTAMP, b.optional(WITH, TIME, ZONE)),
                    NULL,
                    BOOLEAN,
                    BINARY,
                    b.sequence(INTERVAL, b.firstOf(b.sequence(YEAR, TO, MONTH), b.sequence(DAY, TO, SECOND)))
                )
            )

            b.rule(JSON_EQUAL_CONDITION).define(
                JSON_EQUAL,
                PlSqlPunctuator.LPARENTHESIS,
                PlSqlGrammar.EXPRESSION,
                PlSqlPunctuator.COMMA,
                PlSqlGrammar.EXPRESSION,
                b.optional(JSON_EQUAL_ON_ERROR_CLAUSE),
                PlSqlPunctuator.RPARENTHESIS
            )

            b.rule(JSON_EQUAL_ON_ERROR_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    TRUE,
                    FALSE
                ), ON, ERROR
            )

            b.rule(JSON_EXISTS_CONDITION).define(
                JSON_EXISTS,
                PlSqlPunctuator.LPARENTHESIS,
                PlSqlGrammar.EXPRESSION,
                b.optional(FORMAT, JSON),
                PlSqlPunctuator.COMMA,
                SingleRowSqlFunctionsGrammar.JSON_BASIC_PATH_EXPRESSION,
                b.optional(SingleRowSqlFunctionsGrammar.JSON_PASSING_CLAUSE),
                b.optional(JSON_EXISTS_ON_ERROR_CLAUSE),
                b.optional(TYPE, b.firstOf(STRICT, LAX)),
                b.optional(JSON_EXISTS_ON_EMPTY_CLAUSE),
                PlSqlPunctuator.RPARENTHESIS
            )

            b.rule(JSON_EXISTS_ON_ERROR_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    TRUE,
                    FALSE
                ), ON, ERROR
            )

            b.rule(JSON_EXISTS_ON_EMPTY_CLAUSE).define(
                b.firstOf(
                    ERROR,
                    TRUE,
                    FALSE
                ), ON, EMPTY
            )

            b.rule(JSON_TEXTCONTAINS_CONDITION).define(
                JSON_TEXTCONTAINS,
                PlSqlPunctuator.LPARENTHESIS,
                PlSqlGrammar.EXPRESSION,
                PlSqlPunctuator.COMMA,
                SingleRowSqlFunctionsGrammar.JSON_BASIC_PATH_EXPRESSION,
                PlSqlPunctuator.COMMA,
                PlSqlGrammar.EXPRESSION,
                PlSqlPunctuator.RPARENTHESIS
            )

            b.rule(CONDITION).define(
                b.firstOf(
                    RELATIONAL_CONDITION,
                    LIKE_CONDITION,
                    BETWEEN_CONDITION,
                    MULTISET_CONDITION,
                    IS_JSON_CONDITION,
                    IS_OF_CONDITION,
                    JSON_EQUAL_CONDITION,
                    JSON_EXISTS_CONDITION,
                    JSON_TEXTCONTAINS_CONDITION
                )
            ).skip()
        }
    }

}
