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
import java.util.regex.Pattern


class QuotedIdentifierChannel(quotedIdentifierRegexp: String, simpleIdentifierRegexp: String) : Channel<LexerOutput> {

    private val quotedPattern = Pattern.compile(quotedIdentifierRegexp)
    private val quotedSimplePattern = Pattern.compile(""""$simpleIdentifierRegexp"""")

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val nextChar = code.peek().toChar()
        if (nextChar != '"') {
            return false
        }

        val tmpBuilder = StringBuilder()
        val matcher = quotedPattern.matcher("")

        if (code.popTo(matcher, tmpBuilder) > 0) {
            var word = tmpBuilder.toString()
            val wordOriginal = word
            if (quotedSimplePattern.matcher(word).matches() && word == word.uppercase()) {
                word = word.substring(1, word.length - 1)
            }

            val token = Token.builder()
                .setType(GenericTokenType.IDENTIFIER)
                .setValueAndOriginalValue(word, wordOriginal)
                .setLine(code.previousCursor.line)
                .setColumn(code.previousCursor.column)
                .build()
            output.addToken(token)
            return true
        }

        return false
    }

}
