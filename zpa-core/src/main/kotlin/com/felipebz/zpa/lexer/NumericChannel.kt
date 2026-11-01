//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
