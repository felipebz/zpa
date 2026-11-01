//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.impl.channel.RegexpChannel

class DateChannel(
    private val regexpChannel: RegexpChannel,
    private val expectedInitial: Char
)
    : Channel<LexerOutput> by regexpChannel {
    private val uppercaseInitial = expectedInitial.uppercaseChar()

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val nextChar = code.peek()
        if (nextChar != expectedInitial.code && nextChar != uppercaseInitial.code) return false
        return regexpChannel.consume(code, output)
    }

}
