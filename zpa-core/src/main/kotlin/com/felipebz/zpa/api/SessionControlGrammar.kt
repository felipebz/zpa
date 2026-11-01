//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
