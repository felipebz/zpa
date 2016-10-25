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
    MEMBER_CONDITION,
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
                GREATERTHANOREQUAL));
        
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
        
        b.rule(MEMBER_CONDITION).is(CONCATENATION_EXPRESSION, b.optional(NOT), MEMBER, b.optional(OF), CONCATENATION_EXPRESSION).skip();
        
        b.rule(CONDITION).is(b.firstOf(
                RELATIONAL_CONDITION,
                LIKE_CONDITION,
                BETWEEN_CONDITION,
                MEMBER_CONDITION)).skip();
    }

}
