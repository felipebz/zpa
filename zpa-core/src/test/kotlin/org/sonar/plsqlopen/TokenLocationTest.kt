/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenLocationTest {

    @Test
    fun simpleToken() {
        val token = createToken(1, 0, "a")
        val location = TokenLocation.from(token)
        assertThat(location.line()).isEqualTo(1)
        assertThat(location.column()).isEqualTo(0)
        assertThat(location.endLine()).isEqualTo(1)
        assertThat(location.endColumn()).isEqualTo(1)
    }

    @Test
    fun simpleTokenDifferentStart() {
        val token = createToken(10, 5, "a")
        val location = TokenLocation.from(token)
        assertThat(location.line()).isEqualTo(10)
        assertThat(location.column()).isEqualTo(5)
        assertThat(location.endLine()).isEqualTo(10)
        assertThat(location.endColumn()).isEqualTo(6)
    }

    @Test
    fun multilineTokenLF() {
        val token = createToken(1, 0, "abc\n123")
        val location = TokenLocation.from(token)
        assertThat(location.line()).isEqualTo(1)
        assertThat(location.column()).isEqualTo(0)
        assertThat(location.endLine()).isEqualTo(2)
        assertThat(location.endColumn()).isEqualTo(3)
    }

    @Test
    fun multilineTokenCRLF() {
        val token = createToken(1, 0, "abc\r\n123")
        val location = TokenLocation.from(token)
        assertThat(location.line()).isEqualTo(1)
        assertThat(location.column()).isEqualTo(0)
        assertThat(location.endLine()).isEqualTo(2)
        assertThat(location.endColumn()).isEqualTo(3)
    }

    @Test
    fun multilineTokenCR() {
        val token = createToken(1, 0, "abc\r123")
        val location = TokenLocation.from(token)
        assertThat(location.line()).isEqualTo(1)
        assertThat(location.column()).isEqualTo(0)
        assertThat(location.endLine()).isEqualTo(2)
        assertThat(location.endColumn()).isEqualTo(3)
    }

    private fun createToken(startLine: Int, startCharacter: Int, value: String) =
        Token.builder()
            .setLine(startLine)
            .setColumn(startCharacter)
            .setValueAndOriginalValue(value)
            .setType(GenericTokenType.IDENTIFIER)
            .build()
}
