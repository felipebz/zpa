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

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum SqlPlusGrammar implements GrammarRuleKey {

    SQLPLUS_COMMAND;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        createSqlPlusCommands(b);
    }
    
    private static void createSqlPlusCommands(LexerfulGrammarBuilder b) {
        b.rule(SQLPLUS_COMMAND).is(
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
                b.tillNewLine());
    }
    
}
