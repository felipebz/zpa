//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput

internal object ByteOrderMark {
    private const val CHAR = '\uFEFF'

    fun length(text: CharSequence): Int = if (text.isNotEmpty() && text[0] == CHAR) 1 else 0

    class LeadingChannel : Channel<LexerOutput> {
        override fun consume(code: CodeReader, output: LexerOutput): Boolean {
            if (code.lastChar() != -1 || code.peek() != CHAR.code) return false
            code.pop()
            code.setColumnPosition(0)
            return true
        }
    }
}
