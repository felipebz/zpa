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

import com.felipebz.flr.api.GenericTokenType.IDENTIFIER
import com.felipebz.flr.grammar.GrammarRuleKey
import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.IDENTIFIER_NAME
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.COMMA
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.SEMICOLON
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.INTEGER_LITERAL
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.STRING_LITERAL

enum class TclGrammar : GrammarRuleKey {

    TRANSACTION_NAME,

    COMMIT_EXPRESSION,
    ROLLBACK_EXPRESSION,
    SAVEPOINT_EXPRESSION,
    SET_TRANSACTION_EXPRESSION,
    TCL_COMMAND;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            b.rule(COMMIT_EXPRESSION).define(
                    COMMIT,
                    b.optional(WORK),
                    b.optional(b.firstOf(
                            b.sequence(FORCE, STRING_LITERAL, b.optional(COMMA, INTEGER_LITERAL)),
                            b.sequence(
                                    b.optional(COMMENT, STRING_LITERAL),
                                    b.optional(WRITE, b.optional(b.firstOf(IMMEDIATE, BATCH)), b.optional(b.firstOf(WAIT, NOWAIT))))))).skip()

            b.rule(ROLLBACK_EXPRESSION).define(
                    ROLLBACK,
                    b.optional(WORK),
                    b.optional(b.firstOf(
                            b.sequence(FORCE, STRING_LITERAL),
                            b.sequence(TO, b.optional(SAVEPOINT), IDENTIFIER_NAME)))).skip()

            b.rule(SAVEPOINT_EXPRESSION).define(SAVEPOINT, IDENTIFIER_NAME).skip()

            b.rule(TRANSACTION_NAME).define(NAME, STRING_LITERAL)

            //https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_10005.htm#SQLRF01705
            b.rule(SET_TRANSACTION_EXPRESSION).define(
                    SET, TRANSACTION,
                    b.firstOf(
                            b.sequence(
                                    b.firstOf(b.sequence(READ, b.firstOf(ONLY, WRITE)),
                                            b.sequence(ISOLATION, LEVEL, b.firstOf(SERIALIZABLE, b.sequence(READ, COMMITTED))),
                                            b.sequence(USE, ROLLBACK, SEGMENT, IDENTIFIER)),
                                    b.optional(TRANSACTION_NAME)),
                            TRANSACTION_NAME))

            b.rule(TCL_COMMAND).define(
                    b.firstOf(
                            COMMIT_EXPRESSION,
                            ROLLBACK_EXPRESSION,
                            SAVEPOINT_EXPRESSION,
                            SET_TRANSACTION_EXPRESSION),
                    b.optional(SEMICOLON))
        }
    }

}
