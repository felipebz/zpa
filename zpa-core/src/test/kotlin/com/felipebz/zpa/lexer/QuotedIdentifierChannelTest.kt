//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.Test

class QuotedIdentifierChannelTest {
    private val channel = QuotedIdentifierChannel("\".*?\"", "[A-Z]+")
    private val output = LexerOutput()

    @Test
    fun doesNotConsumeUnquotedWord() {
        assertThat(channel).doesNotConsume("word", output)
    }

    @Test
    fun consumeQuotedWord() {
        assertThat(channel).consume("\"some word\"", output)
        assertThat(output.tokens).hasToken("\"some word\"", GenericTokenType.IDENTIFIER)
    }

    @Test
    fun consumeQuotedWordWithSimpleIdentifier() {
        assertThat(channel).consume("\"WORD\"", output)
        assertThat(output.tokens).hasToken("WORD", GenericTokenType.IDENTIFIER)
    }
}
