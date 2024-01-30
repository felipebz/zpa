/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
import com.felipebz.flr.channel.Channel
import com.felipebz.flr.channel.CodeReader
import com.felipebz.flr.impl.LexerException
import com.felipebz.flr.impl.LexerOutput
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import java.util.*
import java.util.regex.Pattern

class RegexPunctuatorChannel(vararg punctuators: PlSqlPunctuator) : Channel<LexerOutput> {
    private val tokenMatchers = LinkedHashMap<PlSqlPunctuator, Pattern>()

    init {
        Arrays.sort(punctuators) { a, b ->
            if (a.value.length == b.value.length) {
                0
            } else if (a.value.length > b.value.length) {
                -1
            } else {
                1
            }
        }

        for (punctuator in punctuators) {
            tokenMatchers[punctuator] = Pattern.compile(punctuator.value)
        }
    }

    override fun consume(code: CodeReader, output: LexerOutput): Boolean {
        val tmpBuilder = StringBuilder()
        val tokenBuilder = Token.builder()

        for ((punctuator, pattern) in tokenMatchers) {
            // if the next character matches the first character of the punctuator
            if (code.peek() == punctuator.firstChar.code) {
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
                    throw LexerException(
                        "The regular expression ${punctuator.value} has led to a stack overflow error.",
                        e
                    )
                }
            }
        }
        return false
    }
}
