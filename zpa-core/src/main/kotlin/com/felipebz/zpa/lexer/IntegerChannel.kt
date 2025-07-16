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
