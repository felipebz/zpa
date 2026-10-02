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
import com.felipebz.flr.api.Trivia
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlKeyword

/**
 * Character-level lookahead and token emission shared by the channels that read MLE source as opaque text.
 * Offsets are relative to the reader's current position.
 */
internal class RawCodeScanner(private val code: CodeReader, private val output: LexerOutput) {

    private var consumed = 0
    private val comments = java.util.TreeMap<Int, Int>()

    fun char(offset: Int): Int = code.intAt(offset)

    /** True when the last emitted token is `AS` or `IS`, the only prefix of an inline MLE call specification. */
    fun followsAsOrIs(): Boolean {
        val type = output.tokens.lastOrNull()?.type
        return type == PlSqlKeyword.AS || type == PlSqlKeyword.IS
    }

    /** True when the emitted tokens end with `CREATE` or `CREATE OR REPLACE`, the only prefix of an MLE module. */
    fun followsCreateOrReplace(): Boolean {
        val tokens = output.tokens
        val size = tokens.size
        if (size >= 1 && tokens[size - 1].type == PlSqlKeyword.CREATE) return true
        return size >= 3 && tokens[size - 1].type == PlSqlKeyword.REPLACE &&
            tokens[size - 2].type == PlSqlKeyword.OR && tokens[size - 3].type == PlSqlKeyword.CREATE
    }

    fun isWhitespace(offset: Int): Boolean = char(offset).let { it > 0 && Character.isWhitespace(it) }

    fun isIdentifierPart(offset: Int): Boolean = char(offset).let {
        it > 0 && (Character.isLetterOrDigit(it) || it == '_'.code || it == '$'.code || it == '#'.code)
    }

    /** End of [word] matched case-insensitively at [start] as a whole word, or -1. */
    fun word(start: Int, word: String): Int {
        for (i in word.indices) {
            val c = char(start + i)
            if (c != word[i].code && c != word[i].uppercaseChar().code) return -1
        }
        val end = start + word.length
        return if (isIdentifierPart(end)) -1 else end
    }

    /**
     * End of the run of whitespace and SQL comments starting at [start] if it is not empty, or -1. Comments met on
     * the way are emitted as trivia, in order, before the next token emitted.
     */
    fun skipTrivia(start: Int, minimum: Int): Int {
        var position = start
        while (true) {
            if (isWhitespace(position)) {
                position++
                continue
            }
            val end = commentEnd(position)
            if (end < 0) break
            comments[position] = end
            position = end
        }
        return if (position - start >= minimum) position else -1
    }

    private fun commentEnd(start: Int): Int {
        if (char(start) == '-'.code && char(start + 1) == '-'.code) {
            var position = start + 2
            while (char(position) > 0 && char(position) != '\n'.code && char(position) != '\r'.code) position++
            return position
        }
        if (char(start) == '/'.code && char(start + 1) == '*'.code) {
            val close = indexOf("*/", start + 2)
            return if (close < 0) -1 else close + 2
        }
        return -1
    }

    fun identifierEnd(start: Int): Int {
        var position = start
        while (isIdentifierPart(position)) position++
        return position
    }

    /** End of a double-quoted identifier starting at [start], or -1. */
    fun quotedIdentifierEnd(start: Int): Int {
        if (char(start) != '"'.code) return -1
        var position = start + 1
        while (char(position) > 0 && char(position) != '"'.code && char(position) != '\n'.code) position++
        return if (char(position) == '"'.code && position > start + 1) position + 1 else -1
    }

    /**
     * End of a string literal starting at [start], or -1. Accepts the forms of the regular lexer: `'text'` with
     * doubled quotes, and an optional `n` prefix and `q` alternative quoting (`q'[text]'`, `nq'!text!'`).
     */
    fun stringEnd(start: Int): Int {
        var quote = start
        if (char(quote) == 'n'.code || char(quote) == 'N'.code) quote++
        val alternative = (char(quote) == 'q'.code || char(quote) == 'Q'.code) && char(quote + 1) == '\''.code
        if (alternative) quote++
        if (char(quote) != '\''.code) return -1
        if (alternative) {
            val opener = char(quote + 1)
            if (opener <= 0 || Character.isWhitespace(opener)) return -1
            val closer = when (opener.toChar()) {
                '(' -> ')'.code
                '[' -> ']'.code
                '<' -> '>'.code
                '{' -> '}'.code
                else -> opener
            }
            var position = quote + 2
            while (char(position) > 0) {
                if (char(position) == closer && char(position + 1) == '\''.code) return position + 2
                position++
            }
            return -1
        }
        var position = quote + 1
        while (char(position) > 0) {
            if (char(position) == '\''.code) {
                if (char(position + 1) == '\''.code) position++ else return position + 1
            }
            position++
        }
        return -1
    }

    fun text(start: Int, end: Int): String {
        val builder = StringBuilder(end - start)
        for (i in start until end) builder.append(char(i).toChar())
        return builder.toString()
    }

    fun indexOf(needle: String, from: Int): Int {
        var position = from
        while (char(position) > 0) {
            if (char(position) == needle[0].code) {
                var matched = 1
                while (matched < needle.length && char(position + matched) == needle[matched].code) matched++
                if (matched == needle.length) return position
            }
            position++
        }
        return -1
    }

    private fun advanceTo(target: Int) {
        while (consumed < target) {
            code.pop()
            consumed++
        }
    }

    fun emit(type: TokenType, start: Int, stop: Int, normalized: Boolean, value: String? = null) {
        for ((commentStart, commentEnd) in comments.headMap(start)) {
            if (commentStart < consumed) continue
            advanceTo(commentStart)
            val line = code.getLinePosition()
            val column = code.getColumnPosition()
            val comment = text(0, commentEnd - commentStart)
            advanceTo(commentEnd)
            output.addTrivia(Trivia.createComment(
                Token.builder().setType(GenericTokenType.COMMENT).setValueAndOriginalValue(comment)
                    .setLine(line).setColumn(column).build()))
        }
        advanceTo(start)
        val line = code.getLinePosition()
        val column = code.getColumnPosition()
        val original = text(0, stop - start)
        advanceTo(stop)
        val tokenValue = value ?: if (normalized) original.uppercase() else original
        output.addToken(
            Token.builder().setType(type).setValueAndOriginalValue(tokenValue, original)
                .setLine(line).setColumn(column).build())
    }
}
