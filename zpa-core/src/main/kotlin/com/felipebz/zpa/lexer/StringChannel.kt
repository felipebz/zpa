//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.impl.channel.RegexpChannel

class StringChannel(private val regexpChannel: RegexpChannel)
    : Channel<LexerOutput> by regexpChannel {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        if (!canStartStringLiteral(code)) return false
        return regexpChannel.consume(code, output)
    }

    private fun canStartStringLiteral(code: CodeReader): Boolean = when (code.peek()) {
        '\''.code -> true
        'n'.code, 'N'.code -> when (code.intAt(1)) {
            '\''.code -> true
            'q'.code, 'Q'.code -> code.intAt(2) == '\''.code
            else -> false
        }
        'q'.code, 'Q'.code -> code.intAt(1) == '\''.code
        else -> false
    }
}
