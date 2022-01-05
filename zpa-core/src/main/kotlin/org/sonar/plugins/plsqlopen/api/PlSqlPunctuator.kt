/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.TokenType

enum class PlSqlPunctuator(override val value: String) : TokenType {
    // Based on http://docs.oracle.com/cd/B19306_01/appdev.102/b14261/fundamentals.htm#sthref297
    COMMA(","),
    PLUS("\\+"),
    MINUS("-"),
    MOD("%"),
    DOT("\\."),
    DIVISION("/"),
    LPARENTHESIS("\\("),
    RPARENTHESIS("\\)"),
    COLON(":"),
    SEMICOLON(";"),
    MULTIPLICATION("\\*"),
    EQUALS("="),
    LESSTHAN("<"),
    GREATERTHAN(">"),
    REMOTE("@"),
    SUBTRACTION("-"),
    ASSIGNMENT(":="),
    ASSOCIATION("=\\s*?>"),
    CONCATENATION("\\|\\|"),
    EXPONENTIATION("\\*\\*"),
    LLABEL("<<"),
    RLABEL(">>"),
    RANGE("\\.\\."),
    NOTEQUALS("<\\s*?>"),
    NOTEQUALS2("!\\s*?="),
    NOTEQUALS3("~\\s*?="),
    NOTEQUALS4("\\^\\s*?="),
    LESSTHANOREQUAL("<\\s*?="),
    GREATERTHANOREQUAL(">\\s*?="),
    DOUBLEDOLLAR("\\$\\$");

    override fun hasToBeSkippedFromAst(node: AstNode?) = false
}
