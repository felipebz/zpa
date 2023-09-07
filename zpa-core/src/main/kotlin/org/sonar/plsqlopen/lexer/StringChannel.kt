/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plsqlopen.lexer

import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.flr.impl.channel.RegexpChannel

class StringChannel(private val regexpChannel: RegexpChannel)
    : Channel<LexerOutput> by regexpChannel {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val nextChar = code.peek().toChar().lowercaseChar()
        if (nextChar != '\'' && nextChar != 'n' && nextChar != 'q') {
            return false
        }

        return regexpChannel.consume(code, output)
    }

}
