//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.UnquotedIdentifier
import java.util.*

class IdentifierChannel(keywordTypes: Array<out TokenType>) : Channel<LexerOutput> {

    private val keywordsMap = keywordTypes.associateBy { it.value.uppercase(Locale.getDefault()) }

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        if (!UnquotedIdentifier.isStart(code.peek())) {
            return false
        }

        val line = code.getLinePosition()
        val column = code.getColumnPosition()
        val valueBuilder = StringBuilder()
        var characterLength = identifierCharacterLength(code)
        while (characterLength > 0) {
            repeat(characterLength) {
                valueBuilder.append(code.pop().toChar())
            }
            characterLength = identifierCharacterLength(code)
        }

        val originalValue = valueBuilder.toString()
        val normalizedValue = originalValue.uppercase(Locale.getDefault())
        val token = Token.builder()
            .setType(keywordsMap[normalizedValue] ?: GenericTokenType.IDENTIFIER)
            .setValueAndOriginalValue(normalizedValue, originalValue)
            .setLine(line)
            .setColumn(column)
            .build()
        output.addToken(token)
        return true
    }

    /** The UTF-16 length of the next character if it goes on the identifier, or 0. */
    private fun identifierCharacterLength(code: CodeReader): Int {
        val character = code.peek()
        if (character in Character.MIN_HIGH_SURROGATE.code..Character.MAX_HIGH_SURROGATE.code) {
            val nextCharacter = code.intAt(1)
            val pair = nextCharacter in Character.MIN_LOW_SURROGATE.code..Character.MAX_LOW_SURROGATE.code &&
                UnquotedIdentifier.isPart(Character.toCodePoint(character.toChar(), nextCharacter.toChar()))
            return if (pair) 2 else 0
        }
        return if (UnquotedIdentifier.isPart(character)) 1 else 0
    }
}
