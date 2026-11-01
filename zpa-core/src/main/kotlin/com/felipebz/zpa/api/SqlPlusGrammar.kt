//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

enum class SqlPlusGrammar : GrammarRuleKey {

    SQLPLUS_COMMAND;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createSqlPlusCommands(b)
        }

        private fun createSqlPlusCommands(b: PlSqlGrammarBuilder) {
            b.rule(SQLPLUS_COMMAND).define(
                    b.nextNot(PlSqlKeyword.SET, b.firstOf(PlSqlKeyword.ROLE, PlSqlKeyword.CONSTRAINT, PlSqlKeyword.CONSTRAINTS)),
                    b.firstOf(
                            "@",
                            "A", "APPEND",
                            "ACC", "ACCEPT",
                            "ARCHIVE",
                            "ATTR", "ATTRIBUTE",
                            "BRE", "BREAK",
                            "BTI", "BTITLE",
                            "C", "CHANGE",
                            "CL", "CLEAR",
                            "COL", "COLUMN",
                            "COMP", "COMPUTE",
                            "CONN", "CONNECT",
                            "COPY",
                            "DEF", "DEFINE",
                            "DEL",
                            "DESC", "DESCRIBE",
                            "DISC", "DISCONNECT",
                            "ED", "EDIT",
                            "EXEC", "EXECUTE",
                            "EXIT", "QUIT",
                            "GET",
                            "HELP", "?",
                            "HO", "HOST", "!", "$",
                            "I", "INPUT",
                            "L", "LIST",
                            "PASSW", "PASSWORD",
                            "PAU", "PAUSE",
                            "PRINT",
                            "PRO", "PROMPT",
                            "RECOVER",
                            "REM", "REMARK",
                            "REPF", "REPFOOTER",
                            "REPH", "REPHEADER",
                            "R", "RUN",
                            "SAV", "SAVE",
                            "SET",
                            "SHO", "SHOW",
                            "SHUTDOWN",
                            "SPO", "SPOOL",
                            "STA", "START",
                            "STARTUP",
                            "STORE",
                            "TIMI", "TIMING",
                            "TTI", "TTITLE",
                            "UNDEF", "UNDEFINE",
                            "VAR", "VARIABLE",
                            "WHENEVER",
                            "XQUERY"),
                    b.firstOf(b.tillNewLine(), b.till(GenericTokenType.EOF)))
        }
    }

}
