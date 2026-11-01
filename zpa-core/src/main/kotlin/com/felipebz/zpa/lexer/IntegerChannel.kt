//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.api.Token
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlTokenType

class IntegerChannel : Channel<LexerOutput>  {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        var tmpBuilder: StringBuilder? = null

        var pos = 0
        var nextChar = code.intAt(pos++).toChar()
        while (nextChar.isDigit()) {
            tmpBuilder = tmpBuilder ?: StringBuilder(5)
            tmpBuilder.append(nextChar)
            nextChar = code.intAt(pos++).toChar()
        }

        if (tmpBuilder.isNullOrEmpty()) {
            return false
        }

        val value = tmpBuilder.toString()
        val token = Token.builder()
            .setType(PlSqlTokenType.INTEGER_LITERAL)
            .setValueAndOriginalValue(value)
            .setLine(code.getLinePosition())
            .setColumn(code.getColumnPosition())
            .build()
        output.addToken(token)

        /* Advance the CodeReader stream by the length of the punctuator */
        for (j in value.indices) {
            code.pop()
        }
        return true

    }

}
