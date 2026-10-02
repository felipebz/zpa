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
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType

/**
 * Reads the body of an inline MLE call specification, `MLE LANGUAGE lang [PURE] <delim> source <delim>`, as one
 * opaque token, because the source is not SQL and must not be tokenized.
 *
 * Oracle 26 takes the opening delimiter as the run of non-blank characters after the language (or PURE) and the
 * closing delimiter as the same text, except that a delimiter made only of `{ [ ( <` closes with the reversed,
 * mirrored brackets. The body ends at the first occurrence of the closing delimiter.
 * Without a closing delimiter the channel declines, and the text is tokenized normally.
 */
class MleInlineSourceChannel : Channel<LexerOutput> {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val first = code.peek()
        if (first != 'm'.code && first != 'M'.code) return false

        val scan = RawCodeScanner(code, output)
        val mleEnd = scan.word(0, "mle")
        if (mleEnd < 0) return false
        // Only `AS|IS MLE LANGUAGE ...` starts an inline call specification; elsewhere these are ordinary words.
        if (!scan.followsAsOrIs()) return false
        var position = scan.skipTrivia(mleEnd, 1)
        if (position < 0) return false
        val languageStart = position
        val languageEnd = scan.word(position, "language")
        if (languageEnd < 0) return false
        position = scan.skipTrivia(languageEnd, 1)
        if (position < 0) return false

        val nameParts = mutableListOf<IntArray>()
        while (true) {
            val end = scan.identifierEnd(position)
            if (end == position) return false
            nameParts.add(intArrayOf(position, end))
            position = end
            if (nameParts.size == 1 && scan.char(position) == '.'.code) position++ else break
        }
        position = scan.skipTrivia(position, 1)
        if (position < 0) return false

        var pure: IntArray? = null
        val pureEnd = scan.word(position, "pure")
        val afterPure = if (pureEnd >= 0) scan.skipTrivia(pureEnd, 1) else -1
        if (afterPure >= 0) {
            pure = intArrayOf(position, pureEnd)
            position = afterPure
        }

        val delimiterStart = position
        var delimiterEnd = delimiterStart
        while (scan.char(delimiterEnd) > 0 && !scan.isWhitespace(delimiterEnd)) delimiterEnd++
        if (delimiterEnd == delimiterStart) return false
        val delimiter = scan.text(delimiterStart, delimiterEnd)
        if (delimiter.startsWith("--") || delimiter.startsWith("/*")) return false

        val closing = closingDelimiter(delimiter)
        val sourceEnd = scan.indexOf(closing, delimiterEnd)
        if (sourceEnd < 0) return false

        scan.emit(PlSqlKeyword.MLE, 0, mleEnd, true)
        scan.emit(PlSqlKeyword.LANGUAGE, languageStart, languageEnd, true)
        nameParts.forEachIndexed { index, part ->
            if (index > 0) scan.emit(PlSqlPunctuator.DOT, part[0] - 1, part[0], false)
            scan.emit(GenericTokenType.IDENTIFIER, part[0], part[1], true)
        }
        if (pure != null) scan.emit(PlSqlKeyword.PURE, pure[0], pure[1], true)
        scan.emit(PlSqlTokenType.MLE_INLINE_SOURCE, delimiterStart, sourceEnd + closing.length, false)
        return true
    }

    private fun closingDelimiter(delimiter: String): String =
        if (delimiter.all { it in OPENING }) {
            delimiter.reversed().map { CLOSING[OPENING.indexOf(it)] }.joinToString("")
        } else {
            delimiter
        }

    private companion object {
        const val OPENING = "{[(<"
        const val CLOSING = "}])>"
    }
}
