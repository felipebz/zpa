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
                            "HO", "HOST",
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
