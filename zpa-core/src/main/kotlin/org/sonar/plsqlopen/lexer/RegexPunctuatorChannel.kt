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

import com.felipebz.flr.api.Token
import com.felipebz.flr.api.TokenType
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerException
import com.felipebz.flr.impl.LexerOutput
import java.util.*
import java.util.regex.Pattern

class RegexPunctuatorChannel(vararg punctuators: TokenType) : Channel<LexerOutput> {
    private val tokenMatchers = LinkedHashMap<TokenType, Pattern>()

    private class PunctuatorComparator : Comparator<TokenType> {

        override fun compare(a: TokenType, b: TokenType): Int {
            if (a.value.length == b.value.length) {
                return 0
            }
            return if (a.value.length > b.value.length) -1 else 1
        }

    }

    init {
        Arrays.sort(punctuators, PunctuatorComparator())

        for (punctuator in punctuators) {
            tokenMatchers[punctuator] = Pattern.compile(punctuator.value)
        }
    }

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val tmpBuilder = StringBuilder()
        val tokenBuilder = Token.builder()

        for ((punctuator, pattern) in tokenMatchers) {
            val matcher = pattern.matcher("")
            try {
                if (code.popTo(matcher, tmpBuilder) > 0) {
                    val value = tmpBuilder.toString()

                    val token = tokenBuilder
                            .setType(punctuator)
                            .setValueAndOriginalValue(value)
                            .setLine(code.previousCursor.line)
                            .setColumn(code.previousCursor.column)
                            .build()

                    output.addToken(token)

                    tmpBuilder.delete(0, tmpBuilder.length)
                    return true
                }
            } catch (e: StackOverflowError) {
                throw LexerException("The regular expression ${punctuator.value} has led to a stack overflow error.", e)
            }

        }
        return false
    }
}
