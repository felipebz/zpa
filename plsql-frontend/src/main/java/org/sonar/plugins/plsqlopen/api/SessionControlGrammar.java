/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.IDENTIFIER_NAME;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum SessionControlGrammar implements GrammarRuleKey {
    
    ALTER_SESSION,
    SET_ROLE,
    SESSION_CONTROL_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        
        b.rule(ALTER_SESSION).is(ALTER, SESSION, b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))));
        
        b.rule(SET_ROLE).is(SET, ROLE,
                b.firstOf(
                        NONE,
                        b.sequence(ALL, b.optional(EXCEPT, b.oneOrMore(IDENTIFIER_NAME, b.optional(COMMA)))),
                        b.oneOrMore(IDENTIFIER_NAME, b.optional(IDENTIFIED, BY, b.anyToken()), b.optional(COMMA))
                        ));
        
        b.rule(SESSION_CONTROL_COMMAND).is(b.firstOf(ALTER_SESSION, SET_ROLE), b.optional(SEMICOLON));
    }

}
