/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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

import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum ConditionsGrammar implements GrammarRuleKey {

    // internal
    RELATIONAL_OPERATOR,
    
    // conditions
    RELATIONAL_CONDITION,
    LIKE_CONDITION,
    BETWEEN_CONDITION,
    MULTISET_CONDITION,
    IS_A_SET_CONDITION,
    IS_EMPTY_CONDITION,
    MEMBER_CONDITION,
    SUBMULTISET_CONDITION,
    CONDITION;

    public static void buildOn(LexerfulGrammarBuilder b) {
        b.rule(RELATIONAL_OPERATOR).is(b.firstOf(
                EQUALS,
                NOTEQUALS, 
                NOTEQUALS2, 
                NOTEQUALS3, 
                NOTEQUALS4, 
                LESSTHAN, 
                GREATERTHAN, 
                LESSTHANOREQUAL, 
                GREATERTHANOREQUAL),
        		b.optional(b.firstOf(ANY, SOME, ALL)));
        
        b.rule(RELATIONAL_CONDITION).is(
                CONCATENATION_EXPRESSION, RELATIONAL_OPERATOR, CONCATENATION_EXPRESSION).skip();
        
        b.rule(LIKE_CONDITION).is(
                CONCATENATION_EXPRESSION,
                b.optional(NOT), LIKE,
                CONCATENATION_EXPRESSION,
                b.optional(ESCAPE, CONCATENATION_EXPRESSION)).skip();
        
        b.rule(BETWEEN_CONDITION).is(
                CONCATENATION_EXPRESSION,
                b.optional(NOT), BETWEEN, 
                CONCATENATION_EXPRESSION, AND, CONCATENATION_EXPRESSION).skip();
        
        b.rule(IS_A_SET_CONDITION).is(CONCATENATION_EXPRESSION, IS, b.optional(NOT), A, SET);
        
        b.rule(IS_EMPTY_CONDITION).is(CONCATENATION_EXPRESSION, IS, b.optional(NOT), EMPTY);
        
        b.rule(MEMBER_CONDITION).is(CONCATENATION_EXPRESSION, b.optional(NOT), MEMBER, b.optional(OF), CONCATENATION_EXPRESSION).skip();
        
        b.rule(SUBMULTISET_CONDITION).is(CONCATENATION_EXPRESSION, b.optional(NOT), SUBMULTISET, b.optional(OF), CONCATENATION_EXPRESSION);
        
        //https://docs.oracle.com/cloud/latest/db112/SQLRF/conditions006.htm#SQLRF52128
        b.rule(MULTISET_CONDITION).is(b.firstOf(
                IS_A_SET_CONDITION,
                IS_EMPTY_CONDITION,
                MEMBER_CONDITION, 
                SUBMULTISET_CONDITION));
        
        b.rule(CONDITION).is(b.firstOf(
                RELATIONAL_CONDITION,
                LIKE_CONDITION,
                BETWEEN_CONDITION,
                MULTISET_CONDITION)).skip();
    }

}
