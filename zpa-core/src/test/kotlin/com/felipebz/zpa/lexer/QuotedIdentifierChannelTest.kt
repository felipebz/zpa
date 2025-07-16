/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
