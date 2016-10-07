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

import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.IDENTIFIER_NAME;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.BATCH;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.COMMENT;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.COMMIT;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.FORCE;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.IMMEDIATE;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.NOWAIT;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.ROLLBACK;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.SAVEPOINT;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.TO;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.WAIT;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.WORK;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.WRITE;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.COMMA;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.SEMICOLON;
import static org.sonar.plugins.plsqlopen.api.PlSqlTokenType.INTEGER_LITERAL;
import static org.sonar.plugins.plsqlopen.api.PlSqlTokenType.STRING_LITERAL;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum TclGrammar implements GrammarRuleKey {
    
    COMMIT_EXPRESSION,
    ROLLBACK_EXPRESSION,
    SAVEPOINT_EXPRESSION,
    TCL_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        b.rule(COMMIT_EXPRESSION).is(
                COMMIT,
                b.optional(WORK),
                b.optional(b.firstOf(
                        b.sequence(FORCE, STRING_LITERAL, b.optional(COMMA, INTEGER_LITERAL)),
                        b.sequence(
                                b.optional(COMMENT, STRING_LITERAL),
                                b.optional(WRITE, b.optional(b.firstOf(IMMEDIATE, BATCH)), b.optional(b.firstOf(WAIT, NOWAIT))))))).skip();
        
        b.rule(ROLLBACK_EXPRESSION).is(
                ROLLBACK,
                b.optional(WORK),
                b.optional(b.firstOf(
                        b.sequence(FORCE, STRING_LITERAL),
                        b.sequence(TO, b.optional(SAVEPOINT), IDENTIFIER_NAME)))).skip();
        
        b.rule(SAVEPOINT_EXPRESSION).is(SAVEPOINT, IDENTIFIER_NAME).skip();
        
        b.rule(TCL_COMMAND).is(
                b.firstOf(
                        COMMIT_EXPRESSION,
                        ROLLBACK_EXPRESSION,
                        SAVEPOINT_EXPRESSION), 
                b.optional(SEMICOLON));
    }

}
