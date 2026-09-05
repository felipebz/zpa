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

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.impl.channel.RegexpChannel

class NumericChannel(private val regexpChannel: RegexpChannel)
    : Channel<LexerOutput> by regexpChannel {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        if (!canStartNumberLiteral(code)) {
            return false
        }

        return regexpChannel.consume(code, output)
    }

    private fun canStartNumberLiteral(code: CodeReader): Boolean {
        val first = code.peek()
        if (first == '.'.code) {
            return isAsciiDigit(code.intAt(1))
        }
        if (!isAsciiDigit(first)) {
            return false
        }

        var position = 1
        while (isAsciiDigit(code.intAt(position))) {
            position++
        }

        return when (code.intAt(position)) {
            '.'.code -> code.intAt(position + 1) != '.'.code
            'e'.code, 'E'.code -> hasExponentDigits(code, position)
            'f'.code, 'F'.code, 'd'.code, 'D'.code -> true
            else -> false
        }
    }

    private fun hasExponentDigits(code: CodeReader, exponentPosition: Int): Boolean {
        var position = exponentPosition + 1
        if (code.intAt(position) == '+'.code || code.intAt(position) == '-'.code) {
            position++
        }
        return isAsciiDigit(code.intAt(position))
    }

    private fun isAsciiDigit(character: Int): Boolean = character in '0'.code..'9'.code

}
