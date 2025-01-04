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
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URISyntaxException

class LineOffsetsTest {
    @Test
    fun startOffset() {
        val foo = mockToken(1, 0, "foo")
        val bar = mockToken(2, 2, "bar")
        val lineOffsets = LineOffsets("foo\n??bar")
        assertThat(lineOffsets.getStartOffset(foo)).isEqualTo(0)
        assertThat(lineOffsets.getStartOffset(bar)).isEqualTo(6)
    }

    @Test
    fun endOffsetSingleLine() {
        val foo = mockToken(1, 0, "foo")
        val bar = mockToken(2, 2, "bar")
        val lineOffsets = LineOffsets("foo\n??bar...")
        assertThat(lineOffsets.getEndOffset(foo)).isEqualTo(3)
        assertThat(lineOffsets.getEndOffset(bar)).isEqualTo(9)
    }

    @Test
    fun endOffsetMultiLine() {
        val foo = mockToken(1, 0, "foo")
        val bar = mockToken(2, 2, "bar\nbaz")
        val lineOffsets = LineOffsets("foo\n??bar\nbaz...")
        assertThat(lineOffsets.getEndOffset(foo)).isEqualTo(3)
        assertThat(lineOffsets.getEndOffset(bar)).isEqualTo(13)
    }

    @Test
    fun endOffsetMultiLineRNSingleOffsetIncrement() {
        val foo = mockToken(1, 0, "foo")
        val bar = mockToken(2, 2, "bar\r\nbaz")
        val lineOffsets = LineOffsets("foo\n??bar\r\nbaz...")
        assertThat(lineOffsets.getEndOffset(foo)).isEqualTo(3)
        assertThat(lineOffsets.getEndOffset(bar)).isEqualTo(13)
    }

    @Test
    fun endOffsetMultiLineRNewLine() {
        val foo = mockToken(1, 0, "foo")
        val bar = mockToken(2, 2, "bar\rbaz")
        val lineOffsets = LineOffsets("foo\n??bar\rbaz...")
        assertThat(lineOffsets.getEndOffset(foo)).isEqualTo(3)
        assertThat(lineOffsets.getEndOffset(bar)).isEqualTo(13)
    }

    @Test
    fun offset() {
        val lineOffsets = LineOffsets("int a = 0;\nint b = 0;")
        assertThat(lineOffsets.getOffset(2, 4)).isEqualTo(15)
        assertThat(lineOffsets.getOffset(2, 100)).isEqualTo(21)
        assertThat(lineOffsets.getOffset(100, 100)).isEqualTo(21)
    }

    @Test
    fun offsetCariageReturnAsNewLine() {
        val lineOffsets = LineOffsets("\rfoo")
        assertThat(lineOffsets.getOffset(1, 0)).isEqualTo(0)
        assertThat(lineOffsets.getOffset(2, 0)).isEqualTo(1)
    }

    @Test
    fun offsetCariageReturnAndLineFeedAsSingleOffset() {
        val lineOffsets = LineOffsets("\r\nfoo")
        assertThat(lineOffsets.getOffset(1, 0)).isEqualTo(0)
        assertThat(lineOffsets.getOffset(2, 0)).isEqualTo(1)
    }

    @Test
    fun offsetBadLine() {
        assertThrows<IllegalArgumentException> {
            val lineOffsets = LineOffsets("")
            lineOffsets.getOffset(0, 0)
        }
    }

    @Test
    fun offsetBadColumn() {
        assertThrows<IllegalArgumentException> {
            val lineOffsets = LineOffsets("")
            lineOffsets.getOffset(1, -1)
        }
    }

    companion object {
        fun mockToken(line: Int, column: Int, value: String?): Token {
            return try {
                Token.builder()
                    .setLine(line)
                    .setColumn(column)
                    .setValueAndOriginalValue(value!!)
                    .setType(GenericTokenType.IDENTIFIER)
                    .build()
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }
    }
}
