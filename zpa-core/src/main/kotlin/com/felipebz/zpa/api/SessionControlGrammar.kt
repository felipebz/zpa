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

import com.felipebz.flr.api.GenericTokenType.EOF
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*

enum class SessionControlGrammar : GrammarRuleKey {

    ALTER_SESSION,
    SET_ROLE,
    SESSION_CONTROL_COMMAND;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {

            b.rule(ALTER_SESSION).define(ALTER, SESSION, b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))))

            b.rule(SET_ROLE).define(SET, ROLE,
                    b.firstOf(
                            NONE,
                            b.sequence(ALL, b.optional(EXCEPT, b.oneOrMore(IDENTIFIER_NAME, b.optional(COMMA)))),
                            b.oneOrMore(IDENTIFIER_NAME, b.optional(IDENTIFIED, BY, b.anyToken()), b.optional(COMMA))
                    ))

            b.rule(SESSION_CONTROL_COMMAND).define(b.firstOf(ALTER_SESSION, SET_ROLE), b.optional(SEMICOLON))
        }
    }

}
