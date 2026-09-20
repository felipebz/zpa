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
import com.felipebz.zpa.api.PlSqlGrammar.EXPRESSION
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*
import com.felipebz.zpa.api.PlSqlTokenType.INTEGER_LITERAL
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

internal enum class RowPatternExpressionMode {
    MEASURES,
    DEFINE
}

internal val ROW_PATTERN_EXPRESSION_CONTEXT: ContextKey<RowPatternExpressionMode> = ContextKey()

/** Grammar rules for Oracle row pattern matching. */
enum class RowPatternGrammar : GrammarRuleKey {
    ROW_PATTERN_CLAUSE,
    ROW_PATTERN_PARTITION_BY_CLAUSE,
    ROW_PATTERN_ORDER_BY_CLAUSE,
    ROW_PATTERN_ORDER_BY_ITEM,
    ROW_PATTERN_COLUMN_REFERENCE,
    ROW_PATTERN_OFFSET,
    ROW_PATTERN_MEASURES_CLAUSE,
    ROW_PATTERN_MEASURE_COLUMN,
    ROW_PATTERN_MEASURE_EXPRESSION,
    ROW_PATTERN_ROWS_PER_MATCH_CLAUSE,
    ROW_PATTERN_SKIP_TO_CLAUSE,
    ROW_PATTERN_PATTERN_CLAUSE,
    ROW_PATTERN,
    ROW_PATTERN_ALTERNATION,
    ROW_PATTERN_CONCATENATION,
    ROW_PATTERN_FACTOR,
    ROW_PATTERN_PRIMARY,
    ROW_PATTERN_QUANTIFIER,
    ROW_PATTERN_PERMUTE,
    ROW_PATTERN_RECOGNITION_FUNCTION,
    ROW_PATTERN_NAVIGATION_FUNCTION,
    ROW_PATTERN_COMPOUND_NAVIGATION_FUNCTION,
    ROW_PATTERN_LOGICAL_NAVIGATION_FUNCTION,
    ROW_PATTERN_PHYSICAL_NAVIGATION_FUNCTION,
    ROW_PATTERN_AGGREGATE_FUNCTION,
    ROW_PATTERN_INVALID_AGGREGATE_FUNCTION,
    ROW_PATTERN_CLASSIFIER_FUNCTION,
    ROW_PATTERN_MATCH_NUMBER_FUNCTION,
    ROW_PATTERN_SUBSET_CLAUSE,
    ROW_PATTERN_SUBSET_ITEM,
    ROW_PATTERN_DEFINE_CLAUSE,
    ROW_PATTERN_DEFINITION,
    ROW_PATTERN_DEFINE_EXPRESSION;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(ROW_PATTERN_CLAUSE).define(
                MATCH_RECOGNIZE,
                LPARENTHESIS,
                b.optional(ROW_PATTERN_PARTITION_BY_CLAUSE),
                b.optional(ROW_PATTERN_ORDER_BY_CLAUSE),
                b.optional(ROW_PATTERN_MEASURES_CLAUSE),
                b.optional(ROW_PATTERN_ROWS_PER_MATCH_CLAUSE),
                b.optional(ROW_PATTERN_SKIP_TO_CLAUSE),
                ROW_PATTERN_PATTERN_CLAUSE,
                b.optional(ROW_PATTERN_SUBSET_CLAUSE),
                ROW_PATTERN_DEFINE_CLAUSE,
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_PARTITION_BY_CLAUSE).define(
                PARTITION,
                BY,
                ROW_PATTERN_COLUMN_REFERENCE,
                b.zeroOrMore(COMMA, ROW_PATTERN_COLUMN_REFERENCE)
            )

            b.rule(ROW_PATTERN_ORDER_BY_CLAUSE).define(
                ORDER,
                BY,
                ROW_PATTERN_ORDER_BY_ITEM,
                b.zeroOrMore(COMMA, ROW_PATTERN_ORDER_BY_ITEM)
            )

            b.rule(ROW_PATTERN_ORDER_BY_ITEM).define(
                ROW_PATTERN_COLUMN_REFERENCE,
                b.optional(b.firstOf(ASC, DESC)),
                b.optional(NULLS, b.firstOf(FIRST, LAST))
            )

            b.rule(ROW_PATTERN_COLUMN_REFERENCE).define(IDENTIFIER_NAME)

            b.rule(ROW_PATTERN_OFFSET).define(EXPRESSION)

            b.rule(ROW_PATTERN_MEASURES_CLAUSE).define(
                MEASURES,
                ROW_PATTERN_MEASURE_COLUMN,
                b.zeroOrMore(COMMA, ROW_PATTERN_MEASURE_COLUMN)
            )

            b.rule(ROW_PATTERN_MEASURE_COLUMN).define(
                ROW_PATTERN_MEASURE_EXPRESSION,
                b.optional(AS),
                IDENTIFIER_NAME
            )

            b.rule(ROW_PATTERN_MEASURE_EXPRESSION).define(
                b.withContext(
                    ROW_PATTERN_EXPRESSION_CONTEXT,
                    RowPatternExpressionMode.MEASURES,
                    EXPRESSION
                )
            )

            b.rule(ROW_PATTERN_ROWS_PER_MATCH_CLAUSE).define(
                b.firstOf(
                    b.sequence(ONE, ROW, PER, MATCH),
                    b.sequence(
                        ALL,
                        ROWS,
                        PER,
                        MATCH,
                        b.optional(
                            b.firstOf(
                                b.sequence(SHOW, EMPTY, MATCHES),
                                b.sequence(OMIT, EMPTY, MATCHES),
                                b.sequence(WITH, UNMATCHED, ROWS)
                            )
                        )
                    )
                )
            )

            b.rule(ROW_PATTERN_RECOGNITION_FUNCTION).define(
                b.firstOf(
                    ROW_PATTERN_NAVIGATION_FUNCTION,
                    ROW_PATTERN_AGGREGATE_FUNCTION,
                    ROW_PATTERN_CLASSIFIER_FUNCTION,
                    ROW_PATTERN_MATCH_NUMBER_FUNCTION
                )
            )

            b.rule(ROW_PATTERN_NAVIGATION_FUNCTION).define(
                b.firstOf(
                    ROW_PATTERN_COMPOUND_NAVIGATION_FUNCTION,
                    ROW_PATTERN_LOGICAL_NAVIGATION_FUNCTION,
                    ROW_PATTERN_PHYSICAL_NAVIGATION_FUNCTION
                )
            )

            b.rule(ROW_PATTERN_COMPOUND_NAVIGATION_FUNCTION).define(
                b.firstOf(PREV, NEXT),
                LPARENTHESIS,
                ROW_PATTERN_LOGICAL_NAVIGATION_FUNCTION,
                b.optional(COMMA, ROW_PATTERN_OFFSET),
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_LOGICAL_NAVIGATION_FUNCTION).define(
                b.sequence(
                    b.firstOf(
                        b.sequence(
                            b.requireContext(
                                ROW_PATTERN_EXPRESSION_CONTEXT,
                                RowPatternExpressionMode.MEASURES
                            ),
                            b.firstOf(RUNNING, FINAL)
                        ),
                        b.optional(RUNNING)
                    ),
                    b.firstOf(FIRST, LAST),
                    LPARENTHESIS,
                    EXPRESSION,
                    b.optional(COMMA, ROW_PATTERN_OFFSET),
                    RPARENTHESIS
                )
            )

            b.rule(ROW_PATTERN_PHYSICAL_NAVIGATION_FUNCTION).define(
                b.firstOf(PREV, NEXT),
                LPARENTHESIS,
                EXPRESSION,
                b.optional(COMMA, ROW_PATTERN_OFFSET),
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_AGGREGATE_FUNCTION).define(
                b.firstOf(
                    b.sequence(
                        b.requireContext(
                            ROW_PATTERN_EXPRESSION_CONTEXT,
                            RowPatternExpressionMode.MEASURES
                        ),
                        b.firstOf(RUNNING, FINAL)
                    ),
                    b.optional(RUNNING)
                ),
                b.firstOf(
                    b.sequence(
                        COUNT,
                        LPARENTHESIS,
                        b.firstOf(
                            MULTIPLICATION,
                            b.sequence(IDENTIFIER_NAME, DOT, MULTIPLICATION),
                            b.sequence(
                                b.optional(b.firstOf(ALL, DISTINCT)),
                                b.nextNot(MULTIPLICATION),
                                EXPRESSION
                            )
                        ),
                        RPARENTHESIS
                    ),
                    b.sequence(
                        b.firstOf(AVG, MAX, MIN, SUM),
                        LPARENTHESIS,
                        b.optional(b.firstOf(ALL, DISTINCT)),
                        b.nextNot(MULTIPLICATION),
                        EXPRESSION,
                        RPARENTHESIS
                    )
                )
            )

            b.rule(ROW_PATTERN_INVALID_AGGREGATE_FUNCTION).define(
                b.requireContext(ROW_PATTERN_EXPRESSION_CONTEXT),
                b.firstOf(
                    b.sequence(AVG, LPARENTHESIS, MULTIPLICATION),
                    b.sequence(MAX, LPARENTHESIS, MULTIPLICATION),
                    b.sequence(MIN, LPARENTHESIS, MULTIPLICATION),
                    b.sequence(SUM, LPARENTHESIS, MULTIPLICATION)
                )
            )

            b.rule(ROW_PATTERN_CLASSIFIER_FUNCTION).define(
                CLASSIFIER,
                LPARENTHESIS,
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_MATCH_NUMBER_FUNCTION).define(
                MATCH_NUMBER,
                LPARENTHESIS,
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_SKIP_TO_CLAUSE).define(
                AFTER,
                MATCH,
                SKIP,
                b.firstOf(
                    b.sequence(TO, NEXT, ROW),
                    b.sequence(PAST, LAST, ROW),
                    b.sequence(TO, FIRST, IDENTIFIER_NAME),
                    b.sequence(TO, LAST, IDENTIFIER_NAME),
                    b.sequence(TO, IDENTIFIER_NAME)
                )
            )

            b.rule(ROW_PATTERN_PATTERN_CLAUSE).define(
                PATTERN,
                LPARENTHESIS,
                ROW_PATTERN,
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN).define(ROW_PATTERN_ALTERNATION)

            b.rule(ROW_PATTERN_ALTERNATION).define(
                ROW_PATTERN_CONCATENATION,
                b.zeroOrMore(SINGLE_PIPE, ROW_PATTERN_CONCATENATION)
            )

            b.rule(ROW_PATTERN_CONCATENATION).define(
                b.oneOrMore(ROW_PATTERN_FACTOR)
            )

            b.rule(ROW_PATTERN_FACTOR).define(
                ROW_PATTERN_PRIMARY,
                b.optional(ROW_PATTERN_QUANTIFIER)
            )

            b.rule(ROW_PATTERN_PRIMARY).define(
                b.firstOf(
                    ROW_PATTERN_PERMUTE,
                    b.sequence(LPARENTHESIS, b.optional(ROW_PATTERN), RPARENTHESIS),
                    b.sequence(LBRACE, MINUS, ROW_PATTERN, MINUS, RBRACE),
                    CARET,
                    DOLLAR,
                    IDENTIFIER_NAME
                )
            )

            b.rule(ROW_PATTERN_QUANTIFIER).define(
                b.firstOf(
                    b.sequence(MULTIPLICATION, b.optional(QUESTION_MARK)),
                    b.sequence(PLUS, b.optional(QUESTION_MARK)),
                    b.sequence(QUESTION_MARK, b.optional(QUESTION_MARK)),
                    b.sequence(
                        LBRACE,
                        b.firstOf(
                            b.sequence(
                                b.optional(INTEGER_LITERAL),
                                COMMA,
                                b.optional(INTEGER_LITERAL)
                            ),
                            INTEGER_LITERAL
                        ),
                        RBRACE,
                        b.optional(QUESTION_MARK)
                    )
                )
            )

            b.rule(ROW_PATTERN_PERMUTE).define(
                PERMUTE,
                LPARENTHESIS,
                ROW_PATTERN,
                b.zeroOrMore(COMMA, ROW_PATTERN),
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_SUBSET_CLAUSE).define(
                SUBSET,
                ROW_PATTERN_SUBSET_ITEM,
                b.zeroOrMore(COMMA, ROW_PATTERN_SUBSET_ITEM)
            )

            b.rule(ROW_PATTERN_SUBSET_ITEM).define(
                IDENTIFIER_NAME,
                EQUALS,
                LPARENTHESIS,
                IDENTIFIER_NAME,
                b.zeroOrMore(COMMA, IDENTIFIER_NAME),
                RPARENTHESIS
            )

            b.rule(ROW_PATTERN_DEFINE_CLAUSE).define(
                DEFINE,
                ROW_PATTERN_DEFINITION,
                b.zeroOrMore(COMMA, ROW_PATTERN_DEFINITION)
            )

            b.rule(ROW_PATTERN_DEFINITION).define(
                IDENTIFIER_NAME,
                AS,
                ROW_PATTERN_DEFINE_EXPRESSION
            )

            b.rule(ROW_PATTERN_DEFINE_EXPRESSION).define(
                b.withContext(
                    ROW_PATTERN_EXPRESSION_CONTEXT,
                    RowPatternExpressionMode.DEFINE,
                    EXPRESSION
                )
            )
        }
    }
}
