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
