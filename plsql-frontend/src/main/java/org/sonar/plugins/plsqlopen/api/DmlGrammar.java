/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api;

import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlTokenType.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum DmlGrammar implements GrammarRuleKey {

    TABLE_REFERENCE,
    DML_TABLE_EXPRESSION_CLAUSE,
    ALIAS,
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
    JOIN_CLAUSE,
    SELECT_COLUMN,
    FROM_CLAUSE,
    WHERE_CLAUSE,
    INTO_CLAUSE,
    GROUP_BY_CLAUSE,
    HAVING_CLAUSE,
    ORDER_BY_ITEM,
    ORDER_BY_CLAUSE,
    FOR_UPDATE_CLAUSE,
    CONNECT_BY_CLAUSE,
    START_WITH_CLAUSE,
    HIERARCHICAL_QUERY_CLAUSE,
    SUBQUERY_FACTORING_CLAUSE,
    SELECT_EXPRESSION,
    DELETE_EXPRESSION,
    UPDATE_COLUMN,
    UPDATE_EXPRESSION,
    INSERT_COLUMNS,
    INSERT_EXPRESSION,
    DML_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        createSelectExpression(b);
        createDeleteExpression(b);
        createUpdateExpression(b);
        createInsertExpression(b);
        
        b.rule(DML_COMMAND).is(
                b.firstOf(
                        SELECT_EXPRESSION, 
                        DELETE_EXPRESSION, 
                        UPDATE_EXPRESSION, 
                        INSERT_EXPRESSION), 
                b.optional(SEMICOLON));
    }
    
    private static void createSelectExpression(LexerfulGrammarBuilder b) {
        
        b.rule(TABLE_REFERENCE).is(
                b.optional(IDENTIFIER_NAME, DOT),
                IDENTIFIER_NAME,
                b.optional(REMOTE, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)));
        
        b.rule(ALIAS).is(IDENTIFIER_NAME);
        
        b.rule(PARTITION_BY_CLAUSE).is(PARTITION, BY, EXPRESSION, b.optional(COMMA, EXPRESSION));
        
        b.rule(WINDOWING_LIMIT).is(b.firstOf(
                b.sequence(UNBOUNDED, b.firstOf(PRECEDING, FOLLOWING)),
                b.sequence(CURRENT, ROW),
                b.sequence(EXPRESSION, b.firstOf(PRECEDING, FOLLOWING))));
        
        b.rule(WINDOWING_CLAUSE).is(
                b.firstOf(ROWS, RANGE_KEYWORD),
                b.firstOf(
                        b.sequence(BETWEEN, WINDOWING_LIMIT, AND, WINDOWING_LIMIT),
                        WINDOWING_LIMIT));
        
        b.rule(KEEP_CLAUSE).is(
                KEEP, LPARENTHESIS,
                DENSE_RANK, b.firstOf(FIRST, LAST), ORDER_BY_CLAUSE,
                RPARENTHESIS);
        
        b.rule(ANALYTIC_CLAUSE).is(
                OVER, LPARENTHESIS, 
                b.optional(PARTITION_BY_CLAUSE), b.optional(ORDER_BY_CLAUSE, b.optional(WINDOWING_CLAUSE)),
                RPARENTHESIS);
        
        b.rule(ON_OR_USING_EXPRESSION).is(
                b.firstOf(
                        b.sequence(ON, EXPRESSION),
                        b.sequence(USING, LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)));
        
        b.rule(OUTER_JOIN_TYPE).is(b.firstOf(FULL, LEFT, RIGHT), b.optional(OUTER));
        
        b.rule(QUERY_PARTITION_CLAUSE).is(
                PARTITION, BY,
                b.firstOf(
                        b.sequence(IDENTIFIER, b.zeroOrMore(COMMA, IDENTIFIER)),
                        b.sequence(LPARENTHESIS, IDENTIFIER, b.zeroOrMore(COMMA, IDENTIFIER), RPARENTHESIS)));
        
        b.rule(INNER_CROSS_JOIN_CLAUSE).is(b.firstOf(
                b.sequence(b.optional(INNER), JOIN, DML_TABLE_EXPRESSION_CLAUSE, ON_OR_USING_EXPRESSION),
                b.sequence(
                        b.firstOf(
                                CROSS,
                                b.sequence(NATURAL, b.optional(INNER))),
                        JOIN, DML_TABLE_EXPRESSION_CLAUSE)
                ));
        
        b.rule(OUTER_JOIN_CLAUSE).is(
                b.optional(QUERY_PARTITION_CLAUSE),
                b.firstOf(
                        b.sequence(OUTER_JOIN_TYPE, JOIN),
                        b.sequence(NATURAL, b.optional(OUTER_JOIN_TYPE), JOIN)),
                b.sequence(DML_TABLE_EXPRESSION_CLAUSE, b.optional(QUERY_PARTITION_CLAUSE),
                b.optional(ON_OR_USING_EXPRESSION)));
        
        b.rule(JOIN_CLAUSE).is(DML_TABLE_EXPRESSION_CLAUSE, b.oneOrMore(b.firstOf(INNER_CROSS_JOIN_CLAUSE, OUTER_JOIN_CLAUSE)));
        
        b.rule(SELECT_COLUMN).is(EXPRESSION, b.optional(b.optional(AS), IDENTIFIER_NAME));
        
        b.rule(DML_TABLE_EXPRESSION_CLAUSE).is(
                b.firstOf(
                        b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS),
                        b.sequence(TABLE_REFERENCE, b.nextNot(LPARENTHESIS)),
                        OBJECT_REFERENCE),
                b.optional(b.sequence(b.nextNot(b.firstOf(PARTITION, CROSS, USING, FULL, NATURAL, INNER, LEFT, RIGHT, OUTER, JOIN)), ALIAS )));
        
        b.rule(FROM_CLAUSE).is(
                FROM, 
                b.firstOf(
                        JOIN_CLAUSE,
                        b.sequence(DML_TABLE_EXPRESSION_CLAUSE, b.zeroOrMore(COMMA, DML_TABLE_EXPRESSION_CLAUSE))));
        
        b.rule(WHERE_CLAUSE).is(WHERE, EXPRESSION);
        
        b.rule(INTO_CLAUSE).is(
                b.optional(BULK, COLLECT), INTO,
                OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE));
        
        b.rule(GROUP_BY_CLAUSE).is(
                GROUP, BY, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION));
        
        b.rule(HAVING_CLAUSE).is(HAVING, EXPRESSION);
        
        b.rule(ORDER_BY_ITEM).is(EXPRESSION, b.optional(b.firstOf(ASC, DESC)), b.optional(NULLS, b.firstOf(FIRST, LAST)));
        
        b.rule(ORDER_BY_CLAUSE).is(
                ORDER, b.optional(SIBLINGS), BY, ORDER_BY_ITEM, b.zeroOrMore(COMMA, ORDER_BY_ITEM));
        
        b.rule(FOR_UPDATE_CLAUSE).is(
                FOR, UPDATE,
                b.optional(OF, OBJECT_REFERENCE, b.zeroOrMore(COMMA, OBJECT_REFERENCE)),
                b.optional(b.firstOf(NOWAIT, b.sequence(WAIT, INTEGER_LITERAL), b.sequence(SKIP, LOCKED))));
        
        b.rule(CONNECT_BY_CLAUSE).is(CONNECT, BY, b.optional(NOCYCLE), EXPRESSION);
        
        b.rule(START_WITH_CLAUSE).is(START, WITH, EXPRESSION);
        
        b.rule(HIERARCHICAL_QUERY_CLAUSE).is(b.firstOf(
                b.sequence(CONNECT_BY_CLAUSE, b.optional(START_WITH_CLAUSE)),
                b.sequence(START_WITH_CLAUSE, CONNECT_BY_CLAUSE)));
        
        b.rule(SUBQUERY_FACTORING_CLAUSE).is(
                WITH,
                b.oneOrMore(IDENTIFIER_NAME, AS, LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS, b.optional(COMMA)));
        
        b.rule(SELECT_EXPRESSION).is(
                b.optional(SUBQUERY_FACTORING_CLAUSE),
                b.firstOf(
                    b.sequence(
                            SELECT, b.optional(b.firstOf(ALL, DISTINCT, UNIQUE)), SELECT_COLUMN, b.zeroOrMore(COMMA, SELECT_COLUMN),
                            b.optional(INTO_CLAUSE),
                            FROM_CLAUSE,
                            b.optional(WHERE_CLAUSE),
                            b.optional(b.firstOf(
                                    b.sequence(GROUP_BY_CLAUSE, b.optional(HAVING_CLAUSE)),
                                    b.sequence(HAVING_CLAUSE, b.optional(GROUP_BY_CLAUSE)))),
                            b.optional(HAVING_CLAUSE),
                            b.optional(HIERARCHICAL_QUERY_CLAUSE),
                            b.optional(ORDER_BY_CLAUSE)),
                    b.sequence(LPARENTHESIS, SELECT_EXPRESSION, RPARENTHESIS)),
                b.optional(b.firstOf(MINUS_KEYWORD, INTERSECT, b.sequence(UNION, b.optional(ALL))), SELECT_EXPRESSION),
                b.optional(FOR_UPDATE_CLAUSE));
    }
    
    private static void createDeleteExpression(LexerfulGrammarBuilder b) {
        b.rule(DELETE_EXPRESSION).is(
                DELETE, b.optional(FROM), 
                DML_TABLE_EXPRESSION_CLAUSE,
                b.optional(b.firstOf(
                        b.sequence(WHERE, CURRENT, OF, IDENTIFIER_NAME),
                        WHERE_CLAUSE)));
    }
    
    private static void createUpdateExpression(LexerfulGrammarBuilder b) {
        b.rule(UPDATE_COLUMN).is(OBJECT_REFERENCE, EQUALS, EXPRESSION);
        
        b.rule(UPDATE_EXPRESSION).is(
                UPDATE, DML_TABLE_EXPRESSION_CLAUSE, SET, UPDATE_COLUMN, b.zeroOrMore(COMMA, UPDATE_COLUMN),
                b.optional(b.firstOf(
                        b.sequence(WHERE, CURRENT, OF, IDENTIFIER_NAME),
                        WHERE_CLAUSE)));
    }
    
    private static void createInsertExpression(LexerfulGrammarBuilder b) {
        b.rule(INSERT_COLUMNS).is(LPARENTHESIS, MEMBER_EXPRESSION, b.zeroOrMore(COMMA, MEMBER_EXPRESSION), RPARENTHESIS);
        
        b.rule(INSERT_EXPRESSION).is(
                INSERT, INTO, TABLE_REFERENCE, b.optional(IDENTIFIER_NAME),
                b.optional(INSERT_COLUMNS),
                b.firstOf(
                        b.sequence(VALUES, LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS),
                        b.sequence(VALUES, EXPRESSION),
                        SELECT_EXPRESSION));
    }

}
