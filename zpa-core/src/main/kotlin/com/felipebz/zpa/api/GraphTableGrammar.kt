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

/** Set while parsing the graph pattern and shape of a GRAPH_TABLE operator. */
internal val GRAPH_TABLE_EXPRESSION_CONTEXT: ContextKey<Boolean> = ContextKey()

/** Grammar rules for the Oracle SQL property graph GRAPH_TABLE operator. */
enum class GraphTableGrammar : GrammarRuleKey {
    GRAPH_TABLE,
    GRAPH_REFERENCE,
    GRAPH_PATTERN,
    GRAPH_PATH_PATTERN,
    GRAPH_VERTEX_PATTERN,
    GRAPH_EDGE_PATTERN,
    GRAPH_PARENTHESIZED_PATH_PATTERN,
    GRAPH_LABEL_EXPRESSION,
    GRAPH_PATTERN_QUANTIFIER,
    GRAPH_TABLE_SHAPE,
    GRAPH_TABLE_ROWS_CLAUSE,
    GRAPH_TABLE_COLUMNS_CLAUSE,
    GRAPH_TABLE_COLUMN_DEFINITION,
    GRAPH_ELEMENT_PREDICATE;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(GRAPH_TABLE).define(
                PlSqlKeyword.GRAPH_TABLE, LPARENTHESIS,
                GRAPH_REFERENCE,
                b.withContext(GRAPH_TABLE_EXPRESSION_CONTEXT, true, GRAPH_PATTERN, GRAPH_TABLE_SHAPE),
                RPARENTHESIS
            )

            b.rule(GRAPH_REFERENCE).define(
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                b.optional(
                    AS, OF,
                    b.firstOf(
                        b.sequence(b.firstOf(SCN, TIMESTAMP), EXPRESSION),
                        b.sequence(PERIOD, FOR, IDENTIFIER_NAME, EXPRESSION)
                    )
                )
            )

            createGraphPattern(b)
            createGraphTableShape(b)

            // Oracle rejects these predicates outside GRAPH_TABLE (ORA-00919), so the shared
            // comparison precedence only reaches them inside a graph pattern or shape.
            b.rule(GRAPH_ELEMENT_PREDICATE).define(
                b.requireContext(GRAPH_TABLE_EXPRESSION_CONTEXT, true),
                IDENTIFIER_NAME, IS, b.optional(NOT),
                b.firstOf(
                    b.sequence(b.firstOf(SOURCE, DESTINATION), OF, IDENTIFIER_NAME),
                    b.sequence(LABELED, IDENTIFIER_NAME)
                )
            )
        }

        private fun createGraphPattern(b: PlSqlGrammarBuilder) {
            b.rule(GRAPH_PATTERN).define(
                MATCH,
                GRAPH_PATH_PATTERN, b.zeroOrMore(COMMA, GRAPH_PATH_PATTERN),
                b.optional(WHERE, EXPRESSION)
            )

            // path_term: consecutive vertices and edges; Oracle supplies implicit vertices
            // between adjacent edges. Only edges and parenthesized paths take quantifiers.
            val pathTerm = b.oneOrMore(
                b.firstOf(
                    GRAPH_VERTEX_PATTERN,
                    GRAPH_PARENTHESIZED_PATH_PATTERN,
                    b.sequence(GRAPH_EDGE_PATTERN, b.optional(GRAPH_PATTERN_QUANTIFIER))
                )
            )

            b.rule(GRAPH_PATH_PATTERN).define(
                b.optional(IDENTIFIER_NAME, EQUALS),
                pathTerm
            )

            val elementPatternFiller = b.sequence(
                b.optional(IDENTIFIER_NAME),
                b.optional(IS, GRAPH_LABEL_EXPRESSION),
                b.optional(WHERE, EXPRESSION)
            )

            b.rule(GRAPH_VERTEX_PATTERN).define(LPARENTHESIS, elementPatternFiller, RPARENTHESIS)

            // A parenthesized path must be quantified; Oracle 26 rejects `((a))` and `(-[e]-)`.
            b.rule(GRAPH_PARENTHESIZED_PATH_PATTERN).define(
                LPARENTHESIS, pathTerm, b.optional(WHERE, EXPRESSION), RPARENTHESIS,
                GRAPH_PATTERN_QUANTIFIER
            )

            // Oracle 26 accepts whitespace between the arrow tokens, so they are not adjacency-checked.
            b.rule(GRAPH_EDGE_PATTERN).define(
                b.firstOf(
                    b.sequence(
                        b.optional(LESSTHAN), MINUS,
                        LBRACKET, elementPatternFiller, RBRACKET,
                        MINUS, b.optional(GREATERTHAN)
                    ),
                    b.sequence(LESSTHAN, MINUS, b.optional(GREATERTHAN)),
                    b.sequence(MINUS, b.optional(GREATERTHAN))
                )
            )

            b.rule(GRAPH_LABEL_EXPRESSION).define(
                IDENTIFIER_NAME, b.zeroOrMore(SINGLE_PIPE, IDENTIFIER_NAME)
            )

            b.rule(GRAPH_PATTERN_QUANTIFIER).define(
                LBRACE,
                b.firstOf(
                    b.sequence(b.optional(INTEGER_LITERAL), COMMA, INTEGER_LITERAL),
                    INTEGER_LITERAL
                ),
                RBRACE
            )
        }

        private fun createGraphTableShape(b: PlSqlGrammarBuilder) {
            b.rule(GRAPH_TABLE_SHAPE).define(
                b.optional(GRAPH_TABLE_ROWS_CLAUSE),
                GRAPH_TABLE_COLUMNS_CLAUSE
            )

            val inPathsClause = b.sequence(
                IN, LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS
            )

            b.rule(GRAPH_TABLE_ROWS_CLAUSE).define(
                ONE, ROW, PER,
                b.firstOf(
                    MATCH,
                    b.sequence(VERTEX, LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS, b.optional(inPathsClause)),
                    b.sequence(
                        STEP, LPARENTHESIS,
                        IDENTIFIER_NAME, COMMA, IDENTIFIER_NAME, COMMA, IDENTIFIER_NAME,
                        RPARENTHESIS, b.optional(inPathsClause)
                    )
                )
            )

            b.rule(GRAPH_TABLE_COLUMNS_CLAUSE).define(
                COLUMNS, LPARENTHESIS,
                GRAPH_TABLE_COLUMN_DEFINITION, b.zeroOrMore(COMMA, GRAPH_TABLE_COLUMN_DEFINITION),
                RPARENTHESIS
            )

            // Oracle 26 requires AS before a column name here (ORA-00907 without it).
            b.rule(GRAPH_TABLE_COLUMN_DEFINITION).define(
                b.firstOf(
                    b.sequence(IDENTIFIER_NAME, DOT, MULTIPLICATION),
                    b.sequence(EXPRESSION, b.optional(AS, IDENTIFIER_NAME))
                )
            )
        }
    }
}
