//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.TokenType

enum class PlSqlPunctuator(override val value: String) : TokenType {
    // Based on http://docs.oracle.com/cd/B19306_01/appdev.102/b14261/fundamentals.htm#sthref297
    COMMA(","),
    PLUS("+"),
    MINUS("-"),
    MOD("%"),
    DOT("."),
    DIVISION("/"),
    LPARENTHESIS("("),
    RPARENTHESIS(")"),
    COLON(":"),
    SEMICOLON(";"),
    MULTIPLICATION("*"),
    EQUALS("="),
    LESSTHAN("<"),
    GREATERTHAN(">"),
    REMOTE("@"),
    SUBTRACTION("-"),
    ASSIGNMENT(":="),
    SINGLE_PIPE("|"),
    EXPONENTIATION("**"),
    LLABEL("<<"),
    VECTOR_COSINE_DISTANCE("<=>"),
    VECTOR_EUCLIDEAN_DISTANCE("<->"),
    VECTOR_NEGATIVE_DOT_PRODUCT("<#>"),
    RLABEL(">>"),
    RANGE(".."),
    DOUBLEDOLLAR("$$"),
    DOLLAR("$"),
    ASSOCIATION("=>"),
    EXCLAMATION("!"),
    TILDE("~"),
    CARET("^"),
    LBRACKET("["),
    RBRACKET("]"),
    LBRACE("{"),
    RBRACE("}"),
    QUESTION_MARK("?"),
    HASH("#");

    override fun hasToBeSkippedFromAst(node: AstNode?) = false
}
