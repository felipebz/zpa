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

    /** Each keyword by its uppercase text, with that text as one string that the lexes of every file can share. */
    private val keywordsMap = keywordTypes.associate { it.value.uppercase(Locale.getDefault()).let { value -> value to Keyword(it, value) } }

    private class Keyword(val type: TokenType, val value: String)

    private class Word(val originalValue: String, val value: String, val type: TokenType)

    /** The value and type of a word from its shared original value; computed once per distinct word in a lex. */
    private val resolveWord: LexerOutput.(String) -> Word = { originalValue ->
        val normalizedValue = originalValue.uppercase(Locale.getDefault())
        val keyword = keywordsMap[normalizedValue]
        // Written in uppercase, the value is the original value. Otherwise a keyword's value is its constant, unless
        // that text already has a string in this lex.
        val value = if (normalizedValue === originalValue) originalValue else sharedString(keyword?.value ?: normalizedValue)
        Word(originalValue, value, keyword?.type ?: GenericTokenType.IDENTIFIER)
    }

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        if (!UnquotedIdentifier.isStart(code.peek())) {
            return false
        }

        val line = code.getLinePosition()
        val column = code.getColumnPosition()
        val valueBuilder = output.tokenText()
        var characterLength = identifierCharacterLength(code)
        while (characterLength > 0) {
            repeat(characterLength) {
                valueBuilder.append(code.pop().toChar())
            }
            characterLength = identifierCharacterLength(code)
        }

        val word = output.sharedResult(valueBuilder, this, resolveWord)
        val token = Token.builder()
            .setType(word.type)
            .setValueAndOriginalValue(word.value, word.originalValue)
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
