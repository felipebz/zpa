//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.impl.channel.CommentRegexpChannel

private const val inlineComment = "--[^\\n\\r]*+"
private const val multiLineComment = "/\\*[\\s\\S]*?\\*/"

class CommentChannel private constructor(private val commentRegexpChannel: CommentRegexpChannel)
    : Channel<LexerOutput> by commentRegexpChannel {

    constructor(): this(CommentRegexpChannel("(?:$inlineComment|$multiLineComment)"))

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val nextChar = code.peek().toChar()
        if (nextChar != '-' && nextChar != '/') {
            return false
        }

        return commentRegexpChannel.consume(code, output)
    }

}
