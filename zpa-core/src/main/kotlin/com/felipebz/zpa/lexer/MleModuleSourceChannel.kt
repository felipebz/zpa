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
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType

/**
 * Reads the text of `MLE MODULE [IF NOT EXISTS] name LANGUAGE lang [VERSION 'v'] AS <text>` as one opaque token,
 * because the module text is source code, not SQL: quotes, comments and backticks in it must not be tokenized.
 *
 * The text runs up to the line that holds only `/` (the SQL*Plus terminator) or the end of the input, without its
 * trailing blank space. Without such text, or when the header differs, the channel declines.
 */
class MleModuleSourceChannel : Channel<LexerOutput> {

    private class Piece(
        val type: TokenType,
        val start: Int,
        val end: Int,
        val normalized: Boolean,
        val value: String? = null)

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val first = code.peek()
        if (first != 'm'.code && first != 'M'.code) return false

        val scan = RawCodeScanner(code, output)
        // Only a `CREATE [OR REPLACE] MLE MODULE` header starts module source; elsewhere these are ordinary words.
        if (scan.word(0, "mle") < 0 || !scan.followsCreateOrReplace()) return false
        val pieces = mutableListOf<Piece>()

        fun keyword(start: Int, word: String, type: PlSqlKeyword): Int {
            val end = scan.word(start, word)
            if (end >= 0) pieces.add(Piece(type, start, end, true))
            return end
        }

        fun space(start: Int): Int = scan.skipTrivia(start, 1)

        fun name(start: Int): Int {
            var position = start
            for (index in 0..1) {
                val quotedEnd = scan.quotedIdentifierEnd(position)
                val end = if (quotedEnd >= 0) quotedEnd else scan.identifierEnd(position)
                if (end <= position) return -1
                pieces.add(
                    if (quotedEnd >= 0) Piece(GenericTokenType.IDENTIFIER, position, end, false, quotedValue(scan.text(position, end)))
                    else Piece(GenericTokenType.IDENTIFIER, position, end, true))
                position = end
                if (index == 0 && scan.char(position) == '.'.code) {
                    pieces.add(Piece(PlSqlPunctuator.DOT, position, position + 1, false))
                    position++
                } else {
                    return position
                }
            }
            return position
        }

        var position = keyword(0, "mle", PlSqlKeyword.MLE)
        if (position < 0) return false
        position = space(position)
        if (position < 0) return false
        position = keyword(position, "module", PlSqlKeyword.MODULE)
        if (position < 0) return false
        position = space(position)
        if (position < 0) return false

        if (keyword(position, "if", PlSqlKeyword.IF) >= 0) {
            position = space(position + 2)
            if (position < 0) return false
            position = keyword(position, "not", PlSqlKeyword.NOT)
            if (position < 0) return false
            position = space(position)
            if (position < 0) return false
            position = keyword(position, "exists", PlSqlKeyword.EXISTS)
            if (position < 0) return false
            position = space(position)
            if (position < 0) return false
        }

        position = name(position)
        if (position < 0) return false
        position = space(position)
        if (position < 0) return false
        position = keyword(position, "language", PlSqlKeyword.LANGUAGE)
        if (position < 0) return false
        position = space(position)
        if (position < 0) return false
        position = name(position)
        if (position < 0) return false
        position = space(position)
        if (position < 0) return false

        val versionEnd = keyword(position, "version", PlSqlKeyword.VERSION)
        if (versionEnd >= 0) {
            position = space(versionEnd)
            if (position < 0) return false
            val stringEnd = scan.stringEnd(position)
            if (stringEnd < 0) return false
            pieces.add(Piece(PlSqlTokenType.STRING_LITERAL, position, stringEnd, false))
            position = space(stringEnd)
            if (position < 0) return false
        }

        position = keyword(position, "as", PlSqlKeyword.AS)
        if (position < 0) return false
        // Only whitespace separates AS from the text: a comment there already belongs to the module source.
        var textStart = position
        while (scan.isWhitespace(textStart)) textStart++
        if (textStart == position || scan.char(textStart) <= 0) return false

        val textEnd = textEnd(scan, textStart)
        if (textEnd <= textStart) return false

        pieces.forEach { scan.emit(it.type, it.start, it.end, it.normalized, it.value) }
        scan.emit(PlSqlTokenType.MLE_MODULE_SOURCE, textStart, textEnd, false)
        return true
    }

    private fun quotedValue(quoted: String): String {
        val inner = quoted.substring(1, quoted.length - 1)
        val simple = inner.isNotEmpty() && (inner[0].isLetterOrDigit() || inner[0] == '_') &&
            inner.all { it.isLetterOrDigit() || it == '_' || it == '#' || it == '$' }
        return if (simple && inner == inner.uppercase()) inner else quoted
    }

    /** End of the module text: the last non-blank character before a line holding only `/`, or the input end. */
    private fun textEnd(scan: RawCodeScanner, start: Int): Int {
        var position = start
        var lineStart = start
        var lineHasContent = false
        var slashOnly = false
        while (true) {
            val c = scan.char(position)
            if (c <= 0 || c == '\n'.code) {
                if (slashOnly || c <= 0) return trimEnd(scan, start, if (slashOnly) lineStart else position)
                lineStart = position + 1
                lineHasContent = false
            } else if (!Character.isWhitespace(c)) {
                slashOnly = c == '/'.code && !lineHasContent
                lineHasContent = true
            }
            position++
        }
    }

    private fun trimEnd(scan: RawCodeScanner, start: Int, limit: Int): Int {
        var end = limit
        while (end > start && scan.isWhitespace(end - 1)) end--
        return end
    }
}
