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
package com.felipebz.zpa.lexer

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType

class GraphQlDualitySourceChannel : Channel<LexerOutput> {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val first = code.peek()
        if (first != '"'.code && first != '_'.code && !Character.isLetter(first)) return false
        if (output.tokens.lastOrNull()?.type != PlSqlKeyword.AS) return false
        if (!followsDualityViewHeader(output.tokens)) return false

        val scan = RawCodeScanner(code, output)
        if (scan.word(0, "select") >= 0) return false

        val end = end(scan)
        if (end < 0) return false

        scan.emit(PlSqlTokenType.GRAPHQL_DUALITY_SOURCE, 0, end, false)
        return true
    }

    // The root field, with its arguments and directives, up to the closing brace of its selection set.
    // Hash comments after that brace still belong to the definition.
    private fun end(scan: RawCodeScanner): Int {
        var position = skipName(scan, 0)
        if (position < 0) return -1
        val dot = skipTrivia(scan, position)
        if (scan.char(dot) == '.'.code) {
            position = skipName(scan, skipTrivia(scan, dot + 1))
            if (position < 0) return -1
        }
        while (true) {
            position = skipTrivia(scan, position)
            when (scan.char(position)) {
                '{'.code -> break
                '('.code -> position = skipArguments(scan, position)
                '@'.code -> position = skipName(scan, position + 1)
                else -> return -1
            }
            if (position < 0) return -1
        }

        var depth = 0
        while (true) {
            val c = scan.char(position)
            if (c <= 0) return -1
            val next = skipLexicalItem(scan, position)
            if (next < 0) return -1
            if (next != position) {
                position = next
                continue
            }
            position++
            if (c == '{'.code) {
                depth++
            } else if (c == '}'.code && --depth == 0) {
                break
            }
        }

        val closed = position
        var lastHashEnd = -1
        while (true) {
            while (scan.isWhitespace(position)) position++
            val c = scan.char(position)
            val next = when {
                c == '#'.code -> skipLine(scan, position).also { lastHashEnd = it }
                c == '-'.code && scan.char(position + 1) == '-'.code -> skipLine(scan, position)
                c == '/'.code && scan.char(position + 1) == '*'.code -> skipBlockComment(scan, position)
                else -> -1
            }
            if (next < 0) break
            position = next
        }
        return if (lastHashEnd >= 0) lastHashEnd else closed
    }

    private fun skipTrivia(scan: RawCodeScanner, start: Int): Int {
        var position = start
        while (true) {
            while (scan.isWhitespace(position)) position++
            val next = skipLexicalItem(scan, position)
            if (next < 0) return start
            if (next == position || scan.char(position) == '"'.code) return position
            position = next
        }
    }

    private fun skipName(scan: RawCodeScanner, start: Int): Int {
        if (scan.char(start) == '"'.code) return skipString(scan, start)
        var position = start
        while (scan.char(position).let { it > 0 && (Character.isLetterOrDigit(it) || it == '_'.code || it == '$'.code) }) position++
        return if (position == start) -1 else position
    }

    private fun skipArguments(scan: RawCodeScanner, start: Int): Int {
        var position = start
        var depth = 0
        while (true) {
            val c = scan.char(position)
            if (c <= 0) return -1
            val next = skipLexicalItem(scan, position)
            if (next < 0) return -1
            if (next != position) {
                position = next
                continue
            }
            position++
            if (c == '('.code) depth++ else if (c == ')'.code && --depth == 0) return position
        }
    }

    // Returns the position after a string or comment starting here, the same position when none starts,
    // or -1 when it is unterminated.
    private fun skipLexicalItem(scan: RawCodeScanner, position: Int): Int {
        val c = scan.char(position)
        return when {
            c == '"'.code -> skipString(scan, position)
            c == '#'.code -> skipLine(scan, position)
            c == '-'.code && scan.char(position + 1) == '-'.code -> skipLine(scan, position)
            c == '/'.code && scan.char(position + 1) == '*'.code -> skipBlockComment(scan, position)
            else -> position
        }
    }

    private fun skipLine(scan: RawCodeScanner, start: Int): Int {
        var position = start
        while (scan.char(position) > 0 && scan.char(position) != '\n'.code && scan.char(position) != '\r'.code) position++
        return position
    }

    private fun skipBlockComment(scan: RawCodeScanner, start: Int): Int {
        val close = scan.indexOf("*/", start + 2)
        return if (close < 0) -1 else close + 2
    }

    private fun skipString(scan: RawCodeScanner, start: Int): Int {
        if (scan.char(start + 1) == '"'.code && scan.char(start + 2) == '"'.code) {
            val close = scan.indexOf("\"\"\"", start + 3)
            return if (close < 0) -1 else close + 3
        }
        var position = start + 1
        while (scan.char(position) > 0) {
            when (scan.char(position)) {
                '\\'.code -> position++
                '"'.code -> return position + 1
            }
            position++
        }
        return -1
    }

    private fun followsDualityViewHeader(tokens: List<Token>): Boolean {
        var index = tokens.size - 2

        fun type(position: Int) = tokens.getOrNull(position)?.type
        fun isName(position: Int) = type(position).let {
            it == GenericTokenType.IDENTIFIER || (it is PlSqlKeyword && !it.isReserved)
        }

        if (type(index) == PlSqlKeyword.REPLICATION && type(index - 1) == PlSqlKeyword.LOGICAL &&
            (type(index - 2) == PlSqlKeyword.ENABLE || type(index - 2) == PlSqlKeyword.DISABLE)) {
            index -= 3
        }

        if (type(index) == PlSqlPunctuator.RPARENTHESIS) {
            index--
            while (true) {
                if (!isName(index)) return false
                index--
                if (type(index) == PlSqlPunctuator.COMMA) index-- else break
            }
            if (type(index) != PlSqlPunctuator.LPARENTHESIS) return false
            index--
        }

        if (!isName(index)) return false
        index -= if (type(index - 1) == PlSqlPunctuator.DOT && isName(index - 2)) 3 else 1

        if (type(index) == PlSqlKeyword.EXISTS && type(index - 1) == PlSqlKeyword.NOT && type(index - 2) == PlSqlKeyword.IF) {
            index -= 3
        }

        if (type(index) != PlSqlKeyword.VIEW || type(index - 1) != PlSqlKeyword.DUALITY) return false
        index -= 2
        if (type(index) == PlSqlKeyword.RELATIONAL) index--
        if (type(index) != PlSqlKeyword.JSON) return false
        index--

        if (type(index) == PlSqlKeyword.EDITIONABLE || type(index) == PlSqlKeyword.NONEDITIONABLE) index--
        if (type(index) == PlSqlKeyword.FORCE) index--
        if (type(index) == PlSqlKeyword.REPLACE && type(index - 1) == PlSqlKeyword.OR) index -= 2
        return type(index) == PlSqlKeyword.CREATE
    }
}
