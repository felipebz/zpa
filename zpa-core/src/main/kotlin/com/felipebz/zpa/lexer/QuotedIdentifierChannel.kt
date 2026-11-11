//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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

        val tmpBuilder = output.tokenText()
        val matcher = quotedPattern.matcher("")

        if (code.popTo(matcher, tmpBuilder) > 0) {
            var word = output.sharedString(tmpBuilder)
            val wordOriginal = word
            if (quotedSimplePattern.matcher(word).matches() && word == word.uppercase()) {
                word = output.sharedString(word.substring(1, word.length - 1))
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
