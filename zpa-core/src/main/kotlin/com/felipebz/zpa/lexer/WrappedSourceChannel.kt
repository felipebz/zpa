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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerOutput
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType

class WrappedSourceChannel : Channel<LexerOutput> {

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val first = code.peek()
        if (first != 'w'.code && first != 'W'.code) return false

        val scan = RawCodeScanner(code, output)
        val wrappedEnd = scan.word(0, "wrapped")
        if (wrappedEnd < 0 || !scan.isWhitespace(wrappedEnd)) return false
        if (!followsWrappableUnitHeader(output)) return false

        var textStart = wrappedEnd
        while (scan.isWhitespace(textStart)) textStart++
        val textEnd = scan.slashTerminatedEnd(textStart)
        if (textEnd <= textStart) return false

        scan.emit(PlSqlKeyword.WRAPPED, 0, wrappedEnd, true)
        scan.emit(PlSqlTokenType.WRAPPED_SOURCE, textStart, textEnd, false)
        return true
    }

    private fun followsWrappableUnitHeader(output: LexerOutput): Boolean {
        var index = output.tokenCount - 1

        fun type(position: Int) = output.tokenAtOrNull(position)?.type
        fun isName(position: Int) = type(position).let {
            it == GenericTokenType.IDENTIFIER || (it is PlSqlKeyword && !it.isReserved)
        }

        index -= when {
            type(index) == PlSqlKeyword.DATA && type(index - 1) == PlSqlKeyword.EXTENDED &&
                type(index - 2) == PlSqlPunctuator.EQUALS && type(index - 3) == PlSqlKeyword.SHARING -> 4
            (type(index) == PlSqlKeyword.METADATA || type(index) == PlSqlKeyword.DATA || type(index) == PlSqlKeyword.NONE) &&
                type(index - 1) == PlSqlPunctuator.EQUALS && type(index - 2) == PlSqlKeyword.SHARING -> 3
            else -> 0
        }

        if (!isName(index)) return false
        index -= if (type(index - 1) == PlSqlPunctuator.DOT && isName(index - 2)) 3 else 1

        if (type(index) == PlSqlKeyword.EXISTS && type(index - 1) == PlSqlKeyword.NOT && type(index - 2) == PlSqlKeyword.IF) {
            index -= 3
        }

        index -= when (type(index)) {
            PlSqlKeyword.PROCEDURE, PlSqlKeyword.FUNCTION, PlSqlKeyword.PACKAGE, PlSqlKeyword.TYPE -> 1
            PlSqlKeyword.BODY -> if (type(index - 1) == PlSqlKeyword.PACKAGE || type(index - 1) == PlSqlKeyword.TYPE) 2 else return false
            else -> return false
        }

        if (type(index) == PlSqlKeyword.EDITIONABLE || type(index) == PlSqlKeyword.NONEDITIONABLE) index--
        if (type(index) == PlSqlKeyword.REPLACE && type(index - 1) == PlSqlKeyword.OR) index -= 2
        return type(index) == PlSqlKeyword.CREATE
    }
}
