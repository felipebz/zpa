/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.INTEGER_LITERAL
import org.sonar.plugins.plsqlopen.api.SingleRowSqlFunctionsGrammar.*

enum class DmlGrammar : GrammarRuleKey {

    TABLE_REFERENCE,
    DML_TABLE_EXPRESSION_CLAUSE,
    ALIAS,
    VALUES_EXPRESSION_CLAUSE,
    PARTITION_BY_CLAUSE,
    WINDOWING_LIMIT,
    WINDOWING_CLAUSE,
    KEEP_CLAUSE,
    ANALYTIC_CLAUSE,
    ON_OR_USING_EXPRESSION,
    INNER_CROSS_JOIN_CLAUSE,
    OUTER_JOIN_TYPE,
    QUERY_PARTITION_CLAUSE,
    OUTER_JOIN_CLAUSE,
    NESTED_CLAUSE,
    JOIN_CLAUSE,
    SELECT_COLUMN,
    FROM_CLAUSE,
    WHERE_CLAUSE,
    INTO_CLAUSE,
    GROUP_BY_CLAUSE,
    HAVING_CLAUSE,
    ORDER_BY_ITEM,
    ORDER_BY_CLAUSE,
    OFFSET_CLAUSE,
    FETCH_ROW_CLAUSE,
    ROW_LIMITING_CLAUSE,
    FOR_UPDATE_CLAUSE,
    CONNECT_BY_CLAUSE,
    START_WITH_CLAUSE,
    HIERARCHICAL_QUERY_CLAUSE,
    WITH_CLAUSE,
    SUBQUERY_FACTORING_CLAUSE,
    SEARCH_CLAUSE,
    CYCLE_CLAUSE,
    RETURNING_INTO_CLAUSE,
    QUERY_BLOCK,
    SELECT_EXPRESSION,
    DELETE_EXPRESSION,
    UPDATE_COLUMN,
    UPDATE_EXPRESSION,
    INSERT_COLUMNS,
    INSERT_EXPRESSION,
    SINGLE_TABLE_INSERT,
    INSERT_INTO_CLAUSE,
    VALUES_CLAUSE,
    MULTI_TABLE_INSERT,
    CONDITIONAL_INSERT_CLAUSE,
    MERGE_EXPRESSION,
    MERGE_UPDATE_CLAUSE,
    MERGE_INSERT_CLAUSE,
    ERROR_LOGGING_CLAUSE,
    DML_COMMAND,
    GROUPING_EXPRESSION_LIST,
    ROLLUP_CUBE_CLAUSE,
    GROUPING_SETS_CLAUSE,

    BACKED_LIST,
    PIVOT_CLAUSE,
    UNPIVOT_CLAUSE,
    PIVOT_FOR_CLAUSE,
    PIVOT_IN_CLAUSE,
    UNPIVOT_IN_CLAUSE;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createSelectExpression(b)
            createDeleteExpression(b)
            createUpdateExpression(b)
            createInsertExpression(b)
            createMergeExpression(b)

            b.rule(DML_COMMAND).define(
                    b.firstOf(
                            SELECT_EXPRESSION,
                            DELETE_EXPRESSION,
                            UPDATE_EXPRESSION,
                            INSERT_EXPRESSION,
                            MERGE_EXPRESSION),
                    b.optional(SEMICOLON))
        }

        private fun createSelectExpression(b: PlSqlGrammarBuilder) {

            b.rule(TABLE_REFERENCE).define(
                    b.optional(IDENTIFIER_NAME, DOT),
                    IDENTIFIER_NAME,
                    b.optional(REMOTE, IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME)))

            b.rule(ALIAS).define(IDENTIFIER_NAME)

            b.rule(PARTITION_BY_CLAUSE).define(PARTITION, BY, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION))

            b.rule(WINDOWING_LIMIT).define(b.firstOf(
                    b.sequence(UNBOUNDED, b.firstOf(PRECEDING, FOLLOWING)),
                    b.sequence(CURRENT, ROW),
                    b.sequence(EXPRESSION, b.firstOf(PRECEDING, FOLLOWING))))

            b.rule(WINDOWING_CLAUSE).define(
                    b.firstOf(ROWS, RANGE_KEYWORD),
                    b.firstOf(
                            b.sequence(BETWEEN, WINDOWING_LIMIT, AND, WINDOWING_LIMIT),
                            WINDOWING_LIMIT))

            b.rule(KEEP_CLAUSE).define(
                    KEEP, LPARENTHESIS,
                    DENSE_RANK, b.firstOf(FIRST, LAST), ORDER_BY_CLAUSE,
                    RPARENTHESIS)

            b.rule(ANALYTIC_CLAUSE).define(
                    OVER, LPARENTHESIS,
                    b.optional(PARTITION_BY_CLAUSE), b.optional(ORDER_BY_CLAUSE, b.optional(WINDOWING_CLAUSE)),
                    RPARENTHESIS)

            b.rule(ON_OR_USING_EXPRESSION).define(
                    b.firstOf(
                            b.sequence(ON, EXPRESSION),
                            b.sequence(USING, LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)))

            b.rule(OUTER_JOIN_TYPE).define(b.firstOf(FULL, LEFT, RIGHT), b.optional(OUTER))

            b.rule(QUERY_PARTITION_CLAUSE).define(
                    PARTITION, BY,
                    b.firstOf(
                            b.sequence(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION)),
                            b.sequence(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS)))

            b.rule(INNER_CROSS_JOIN_CLAUSE).define(b.firstOf(
                    b.sequence(b.optional(INNER), JOIN, DML_TABLE_EXPRESSION_CLAUSE, ON_OR_USING_EXPRESSION),
                    b.sequence(
                            b.firstOf(
                                    CROSS,
                                    b.sequence(NATURAL, b.optional(INNER))),
                            JOIN, DML_TABLE_EXPRESSION_CLAUSE)
            ))

            b.rule(OUTER_JOIN_CLAUSE).define(
                    b.optional(QUERY_PARTITION_CLAUSE),
                    b.firstOf(
                            b.sequence(OUTER_JOIN_TYPE, JOIN),
                            b.sequence(NATURAL, b.optional(OUTER_JOIN_TYPE), JOIN)),
                    b.sequence(DML_TABLE_EXPRESSION_CLAUSE, b.optional(QUERY_PARTITION_CLAUSE),
                            b.optional(ON_OR_USING_EXPRESSION)))

            b.rule(NESTED_CLAUSE).define(
                NESTED, b.optional(PATH), IDENTIFIER_NAME,
                b.optional(b.firstOf(
                    b.sequence(DOT, JSON_RELATIVE_OBJECT_ACCESS),
                    b.sequence(COMMA, JSON_BASIC_PATH_EXPRESSION)
                )),
                b.optional(JSON_TABLE_ON_ERROR_CLAUSE),
                b.optional(JSON_TABLE_ON_EMPTY_CLAUSE),
                JSON_COLUMNS_CLAUSE
            )

            b.rule(JOIN_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(LPARENTHESIS, JOIN_CLAUSE, RPARENTHESIS),
                            DML_TABLE_EXPRESSION_CLAUSE),
                        b.oneOrMore(b.firstOf(INNER_CROSS_JOIN_CLAUSE, OUTER_JOIN_CLAUSE))),
                    b.sequence(LPARENTHESIS, JOIN_CLAUSE, RPARENTHESIS)
                ))

            b.rule(SELECT_COLUMN).define(EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME, b.nextNot(COLLECT)))

            b.rule(DML_TABLE_EXPRESSION_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS),
                            b.sequence(TABLE_REFERENCE, b.nextNot(LPARENTHESIS)),
                            OBJECT_REFERENCE
                        ),
                        b.optional(NESTED_CLAUSE),
                        b.optional(
                            b.nextNot(
                                b.firstOf(
                                    PARTITION,
                                    CROSS,
                                    USING,
                                    FULL,
                                    NATURAL,
                                    INNER,
                                    LEFT,
                                    RIGHT,
                                    OUTER,
                                    JOIN,
                                    RETURN,
                                    RETURNING,
                                    LOG,
                                    EXCEPT,
                                    SET
                                )
                            ), b.optional(
                                b.oneOrMore(
                                    b.firstOf(
                                        PIVOT_CLAUSE,
                                        UNPIVOT_CLAUSE
                                    )
                                )
                            ),
                            b.optional(AS),
                            ALIAS
                        )
                    ),
                    VALUES_EXPRESSION_CLAUSE
                ), b.optional(
                    b.oneOrMore(
                        b.firstOf(
                            PIVOT_CLAUSE,
                            UNPIVOT_CLAUSE
                        )
                    )
                )
            )

            b.rule(VALUES_EXPRESSION_CLAUSE).define(
                LPARENTHESIS,
                VALUES,
                b.oneOrMore(LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS, b.optional(COMMA)),
                RPARENTHESIS,
                b.optional(AS),
                IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)
            )

            b.rule(FROM_CLAUSE).define(
                    FROM,
                    b.firstOf(JOIN_CLAUSE, DML_TABLE_EXPRESSION_CLAUSE),
                    b.zeroOrMore(COMMA, b.firstOf(JOIN_CLAUSE, DML_TABLE_EXPRESSION_CLAUSE)))

            b.rule(WHERE_CLAUSE).define(WHERE, EXPRESSION)

            b.rule(INTO_CLAUSE).define(
                    b.optional(BULK, COLLECT), INTO,
                    OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE))

            b.rule(GROUPING_EXPRESSION_LIST).define(
                b.firstOf(
                    b.sequence(LPARENTHESIS, b.optional(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION)), RPARENTHESIS),
                    EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION))
                )

            b.rule(ROLLUP_CUBE_CLAUSE).define(
                b.firstOf(ROLLUP, CUBE), LPARENTHESIS, GROUPING_EXPRESSION_LIST, RPARENTHESIS)

            b.rule(GROUPING_SETS_CLAUSE).define(
                GROUPING, SETS, LPARENTHESIS,
                b.firstOf(ROLLUP_CUBE_CLAUSE, GROUPING_EXPRESSION_LIST),
                b.zeroOrMore(COMMA, b.firstOf(ROLLUP_CUBE_CLAUSE, GROUPING_EXPRESSION_LIST)),
                RPARENTHESIS)

            b.rule(GROUP_BY_CLAUSE).define(
                GROUP, BY,
                b.firstOf(ROLLUP_CUBE_CLAUSE, GROUPING_SETS_CLAUSE, GROUPING_EXPRESSION_LIST),
                b.zeroOrMore(COMMA, b.firstOf(ROLLUP_CUBE_CLAUSE, GROUPING_SETS_CLAUSE, GROUPING_EXPRESSION_LIST)))

            b.rule(HAVING_CLAUSE).define(HAVING, EXPRESSION)


            b.rule(BACKED_LIST).define(
                b.firstOf(
                    b.oneOrMore(EXPRESSION, b.optional(b.optional(AS), b.firstOf(ALIAS,LITERAL)), b.optional(COMMA)),
                    b.oneOrMore(
                        LPARENTHESIS,
                        b.oneOrMore(b.firstOf(LITERAL, IDENTIFIER_NAME), b.optional(COMMA)),
                        RPARENTHESIS,
                        b.optional(AS, b.oneOrMore(b.firstOf(LITERAL, IDENTIFIER_NAME), b.optional(COMMA))),
                        b.optional(COMMA)
                    ))
            )

            b.rule(PIVOT_FOR_CLAUSE).define(
                FOR,
                b.firstOf(
                    b.sequence(LPARENTHESIS, b.oneOrMore(VARIABLE_NAME, b.optional(COMMA)), RPARENTHESIS),
                    VARIABLE_NAME
                )
            )
            b.rule(PIVOT_IN_CLAUSE).define(
                IN,
                LPARENTHESIS,
                b.firstOf(
                    b.oneOrMore(
                        b.firstOf(
                            EXPRESSION,
                            b.sequence(LPARENTHESIS, b.oneOrMore(EXPRESSION, b.optional(COMMA)), RPARENTHESIS)
                        ),
                        b.optional(b.optional(AS), b.firstOf(ALIAS,LITERAL)),
                        b.optional(COMMA)),
                    SELECT_EXPRESSION,
                    b.oneOrMore(ANY, b.optional(COMMA))),
                RPARENTHESIS
            )

            b.rule(UNPIVOT_IN_CLAUSE).define(
                IN,
                LPARENTHESIS,
                b.oneOrMore(
                    b.firstOf(
                        b.sequence(LPARENTHESIS, b.oneOrMore(VARIABLE_NAME, b.optional(COMMA)), RPARENTHESIS),
                        VARIABLE_NAME
                    ),
                    b.optional(
                        b.optional(AS),
                        b.firstOf(
                            b.sequence(LPARENTHESIS, b.oneOrMore(b.firstOf(ALIAS, LITERAL), b.optional(COMMA)), RPARENTHESIS),
                            b.firstOf(ALIAS, LITERAL)
                        )
                    ),
                    b.optional(COMMA)
                ),
                RPARENTHESIS
            )

            b.rule(PIVOT_CLAUSE).define(
                b.sequence(
                    PIVOT,
                    LPARENTHESIS,
                    b.oneOrMore(
                        b.firstOf(
                            b.sequence(LPARENTHESIS, b.optional(EXPRESSION), RPARENTHESIS),
                            EXPRESSION
                        ),
                        b.optional(b.optional(AS), b.firstOf(ALIAS,LITERAL)),
                        b.optional(COMMA)
                    ),
                    PIVOT_FOR_CLAUSE,
                    PIVOT_IN_CLAUSE,
                    RPARENTHESIS
                )
            )

            b.rule(UNPIVOT_CLAUSE).define(
                b.sequence(
                    UNPIVOT,
                    b.optional(b.firstOf(INCLUDE, EXCLUDE), NULLS),
                    LPARENTHESIS,
                    b.firstOf(
                        b.sequence(LPARENTHESIS, b.oneOrMore(VARIABLE_NAME, b.optional(COMMA)), RPARENTHESIS),
                        VARIABLE_NAME
                    ),
                    PIVOT_FOR_CLAUSE,
                    UNPIVOT_IN_CLAUSE,
                    RPARENTHESIS
                )
            )

            b.rule(ORDER_BY_ITEM).define(EXPRESSION, b.optional(b.firstOf(ASC, DESC)), b.optional(NULLS, b.firstOf(FIRST, LAST)))

            b.rule(ORDER_BY_CLAUSE).define(
                    ORDER, b.optional(SIBLINGS), BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM))

            b.rule(OFFSET_CLAUSE).define(OFFSET, EXPRESSION, b.firstOf(ROW, ROWS))

            b.rule(FETCH_ROW_CLAUSE).define(FETCH, b.firstOf(FIRST, NEXT), b.optional(EXPRESSION, b.optional(PERCENT)), b.firstOf(ROW, ROWS), b.firstOf(ONLY, b.sequence(WITH, TIES)))

            b.rule(ROW_LIMITING_CLAUSE).define(b.firstOf(
                    b.sequence(OFFSET_CLAUSE, b.optional(FETCH_ROW_CLAUSE)),
                    FETCH_ROW_CLAUSE))

            b.rule(FOR_UPDATE_CLAUSE).define(
                    FOR, UPDATE,
                    b.optional(OF, OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE)),
                    b.optional(b.firstOf(NOWAIT, b.sequence(WAIT, INTEGER_LITERAL), b.sequence(SKIP, LOCKED))))

            b.rule(CONNECT_BY_CLAUSE).define(CONNECT, BY, b.optional(NOCYCLE), EXPRESSION)

            b.rule(START_WITH_CLAUSE).define(START, WITH, EXPRESSION)

            b.rule(HIERARCHICAL_QUERY_CLAUSE).define(b.firstOf(
                    b.sequence(CONNECT_BY_CLAUSE, b.optional(START_WITH_CLAUSE)),
                    b.sequence(START_WITH_CLAUSE, CONNECT_BY_CLAUSE)))

            b.rule(WITH_CLAUSE).define(
                WITH,
                b.firstOf(
                    b.sequence(
                        b.oneOrMore(b.firstOf(FUNCTION_DECLARATION, PROCEDURE_DECLARATION)),
                        b.zeroOrMore(SUBQUERY_FACTORING_CLAUSE, b.zeroOrMore(COMMA, SUBQUERY_FACTORING_CLAUSE))
                    ),
                    b.oneOrMore(SUBQUERY_FACTORING_CLAUSE, b.zeroOrMore(COMMA, SUBQUERY_FACTORING_CLAUSE))
                )
            )

            b.rule(SUBQUERY_FACTORING_CLAUSE).define(
                IDENTIFIER_NAME,
                b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                AS,
                b.firstOf(
                    b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS),
                    VALUES_EXPRESSION_CLAUSE),
                b.optional(SEARCH_CLAUSE),
                b.optional(CYCLE_CLAUSE)
            )

            b.rule(SEARCH_CLAUSE).define(
                SEARCH, b.firstOf(BREADTH, DEPTH),
                FIRST, BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM),
                SET, IDENTIFIER_NAME
            )

            b.rule(CYCLE_CLAUSE).define(
                CYCLE, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME),
                SET, IDENTIFIER_NAME, TO, EXPRESSION, DEFAULT, EXPRESSION
            )

            b.rule(QUERY_BLOCK).define(
                b.firstOf(
                    b.sequence(
                        SELECT, b.optional(b.firstOf(ALL, DISTINCT, UNIQUE)), SELECT_COLUMN, b.zeroOrMore(COMMA, SELECT_COLUMN),
                        b.optional(INTO_CLAUSE),
                        b.optional(FROM_CLAUSE),
                        b.optional(WHERE_CLAUSE),
                        b.optional(b.firstOf(
                            b.sequence(GROUP_BY_CLAUSE, b.optional(HAVING_CLAUSE)),
                            b.sequence(HAVING_CLAUSE, b.optional(GROUP_BY_CLAUSE)))),
                        b.optional(HAVING_CLAUSE),
                        b.optional(HIERARCHICAL_QUERY_CLAUSE)),
                    b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS)))

            b.rule(SELECT_EXPRESSION).define(
                b.optional(WITH_CLAUSE),
                QUERY_BLOCK,
                b.zeroOrMore(b.firstOf(MINUS_KEYWORD, INTERSECT, UNION, EXCEPT), b.optional(ALL), QUERY_BLOCK),
                b.optional(b.firstOf(
                    b.sequence(ORDER_BY_CLAUSE, b.optional(b.firstOf(FOR_UPDATE_CLAUSE, ROW_LIMITING_CLAUSE))),
                    ROW_LIMITING_CLAUSE,
                    b.sequence(FOR_UPDATE_CLAUSE, b.optional(ORDER_BY_CLAUSE)))))
        }

        private fun createDeleteExpression(b: PlSqlGrammarBuilder) {
            b.rule(RETURNING_INTO_CLAUSE).define(
                    b.firstOf(RETURNING, RETURN),
                    b.optional(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION)),
                    INTO_CLAUSE)

            b.rule(DELETE_EXPRESSION).define(
                    DELETE, b.optional(FROM),
                    DML_TABLE_EXPRESSION_CLAUSE,
                    b.optional(b.firstOf(
                            b.sequence(WHERE, CURRENT, OF, IDENTIFIER_NAME),
                            WHERE_CLAUSE)),
                    b.optional(RETURNING_INTO_CLAUSE))
        }

        private fun createUpdateExpression(b: PlSqlGrammarBuilder) {
            b.rule(UPDATE_COLUMN).define(OBJECT_REFERENCE, EQUALS, b.firstOf(EXPRESSION, DEFAULT))

            b.rule(UPDATE_EXPRESSION).define(
                    UPDATE, DML_TABLE_EXPRESSION_CLAUSE, SET, UPDATE_COLUMN, b.zeroOrMore(COMMA, UPDATE_COLUMN),
                    b.optional(b.firstOf(
                            b.sequence(WHERE, CURRENT, OF, IDENTIFIER_NAME),
                            WHERE_CLAUSE)),
                    b.optional(RETURNING_INTO_CLAUSE))
        }

        private fun createInsertExpression(b: PlSqlGrammarBuilder) {
            b.rule(INSERT_COLUMNS).define(LPARENTHESIS, MEMBER_EXPRESSION, b.zeroOrMore(COMMA, MEMBER_EXPRESSION), RPARENTHESIS)

            b.rule(INSERT_EXPRESSION).define(INSERT, b.firstOf(SINGLE_TABLE_INSERT, MULTI_TABLE_INSERT))

            b.rule(SINGLE_TABLE_INSERT).define(
                INSERT_INTO_CLAUSE,
                b.firstOf(
                    b.sequence(VALUES_CLAUSE, b.optional(RETURNING_INTO_CLAUSE)),
                    SELECT_EXPRESSION),
                b.optional(ERROR_LOGGING_CLAUSE))

            b.rule(INSERT_INTO_CLAUSE).define(INTO,
                b.firstOf(b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS), b.firstOf(
                    TABLE_EXPRESSION, THE_EXPRESSION, TABLE_REFERENCE)),
                b.optional(IDENTIFIER_NAME), b.optional(INSERT_COLUMNS))

            b.rule(VALUES_CLAUSE).define(
                VALUES,
                b.firstOf(
                    b.sequence(LPARENTHESIS, b.firstOf(EXPRESSION, DEFAULT), b.zeroOrMore(b.sequence(COMMA, b.firstOf(EXPRESSION, DEFAULT))), RPARENTHESIS),
                    EXPRESSION))

            b.rule(MULTI_TABLE_INSERT).define(
                b.firstOf(
                    b.sequence(ALL, b.oneOrMore(b.sequence(INSERT_INTO_CLAUSE, b.optional(VALUES_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE)))),
                    CONDITIONAL_INSERT_CLAUSE),
                SELECT_EXPRESSION)

            b.rule(CONDITIONAL_INSERT_CLAUSE).define(
                b.optional(b.firstOf(ALL, FIRST)),
                WHEN, EXPRESSION, THEN, INSERT_INTO_CLAUSE,
                b.optional(VALUES_CLAUSE),
                b.optional(ERROR_LOGGING_CLAUSE),
                b.zeroOrMore(INSERT_INTO_CLAUSE, b.optional(VALUES_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE)),
                b.zeroOrMore(WHEN, EXPRESSION, THEN, INSERT_INTO_CLAUSE,
                    b.optional(VALUES_CLAUSE),
                    b.optional(ERROR_LOGGING_CLAUSE),
                    b.zeroOrMore(INSERT_INTO_CLAUSE, b.optional(VALUES_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE))),
                b.optional(ELSE, INSERT_INTO_CLAUSE,
                    b.optional(VALUES_CLAUSE),
                    b.optional(ERROR_LOGGING_CLAUSE),
                    b.zeroOrMore(INSERT_INTO_CLAUSE, b.optional(VALUES_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE))))

        }

        private fun createMergeExpression(b: PlSqlGrammarBuilder) {

            b.rule(MERGE_UPDATE_CLAUSE).define(WHEN, MATCHED, THEN, UPDATE, SET, UPDATE_COLUMN, b.zeroOrMore(COMMA, UPDATE_COLUMN),
                    b.optional(WHERE_CLAUSE), b.optional(DELETE, WHERE_CLAUSE))

            b.rule(MERGE_INSERT_CLAUSE).define(WHEN, NOT, MATCHED, THEN, INSERT,
                    b.optional(LPARENTHESIS, OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE), RPARENTHESIS),
                    VALUES, b.firstOf(
                    b.sequence(LPARENTHESIS, b.firstOf(EXPRESSION, DEFAULT), b.zeroOrMore(COMMA, b.firstOf(EXPRESSION, DEFAULT)), RPARENTHESIS),
                    IDENTIFIER_NAME),
                    b.optional(WHERE_CLAUSE))

            b.rule(ERROR_LOGGING_CLAUSE).define(
                    LOG, ERRORS,
                    b.optional(INTO, TABLE_REFERENCE),
                    b.optional(LPARENTHESIS, EXPRESSION, RPARENTHESIS),
                    b.optional(REJECT, LIMIT, b.firstOf(EXPRESSION, UNLIMITED)))

            //https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_9016.htm#SQLRF01606
            b.rule(MERGE_EXPRESSION).define(
                    MERGE, INTO, TABLE_REFERENCE, b.optional(b.nextNot(USING), IDENTIFIER_NAME),
                    USING, DML_TABLE_EXPRESSION_CLAUSE, ON, LPARENTHESIS, BOOLEAN_EXPRESSION, RPARENTHESIS,
                    b.firstOf(
                            b.sequence(MERGE_UPDATE_CLAUSE, b.optional(MERGE_INSERT_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE)),
                            b.sequence(MERGE_INSERT_CLAUSE, b.optional(MERGE_UPDATE_CLAUSE), b.optional(ERROR_LOGGING_CLAUSE))))
        }
    }

}
