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

import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import java.util.Arrays;
import java.util.List;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import com.sonar.sslr.api.GenericTokenType;

public enum DclGrammar implements GrammarRuleKey {
    IDENTIFIER_OR_KEYWORD,
    GRANT_STATEMENT,
    PRIVILEGE_PART,
    PRIVILEGE_COLUMNS,
    GRANT_SYSTEM_PRIVILEGES,
    GRANT_OBJECT_PRIVILEGES,
    GRANT_ROLES_TO_PROGRAMS,
    DCL_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        createDclCommands(b);
    }

    private static void createDclCommands(LexerfulGrammarBuilder b) {
        List<PlSqlKeyword> keywords = Arrays.asList(PlSqlKeyword.values());
        PlSqlKeyword[] rest = keywords.subList(1, keywords.size()).toArray(new PlSqlKeyword[keywords.size() - 1]);
        b.rule(IDENTIFIER_OR_KEYWORD).is(b.firstOf(GenericTokenType.IDENTIFIER, keywords.get(0), (Object[]) rest));
        
        b.rule(PRIVILEGE_PART).is(b.nextNot(b.firstOf(COMMA, ON, TO, LPARENTHESIS)), IDENTIFIER_OR_KEYWORD);
        
        b.rule(PRIVILEGE_COLUMNS).is(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS);
        
        b.rule(GRANT_SYSTEM_PRIVILEGES).is(
                b.oneOrMore(PRIVILEGE_PART), 
                b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART)),
                TO, IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD),
                b.optional(IDENTIFIED, BY, b.anyToken(), b.zeroOrMore(COMMA, b.anyToken())),
                b.optional(WITH, b.firstOf(ADMIN, DELEGATE), OPTION),
                b.optional(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)));
        
        b.rule(GRANT_OBJECT_PRIVILEGES).is(
                b.oneOrMore(PRIVILEGE_PART), b.optional(PRIVILEGE_COLUMNS), 
                b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART, b.optional(PRIVILEGE_COLUMNS))),
                b.optional(ON, b.oneOrMore(b.anyTokenButNot(TO))),
                TO, IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD),
                b.optional(WITH, HIERARCHY, OPTION),
                b.optional(WITH, GRANT, OPTION));
        
        b.rule(GRANT_ROLES_TO_PROGRAMS).is(
                b.oneOrMore(PRIVILEGE_PART), 
                b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART)),
                TO, b.firstOf(FUNCTION, PROCEDURE, PACKAGE), UNIT_NAME, 
                b.zeroOrMore(COMMA, b.firstOf(FUNCTION, PROCEDURE, PACKAGE), UNIT_NAME)
                );
        
        b.rule(GRANT_STATEMENT).is(GRANT, b.firstOf(GRANT_ROLES_TO_PROGRAMS, GRANT_SYSTEM_PRIVILEGES, GRANT_OBJECT_PRIVILEGES), b.optional(SEMICOLON));
        
        b.rule(DCL_COMMAND).is(GRANT_STATEMENT);
    }

}
