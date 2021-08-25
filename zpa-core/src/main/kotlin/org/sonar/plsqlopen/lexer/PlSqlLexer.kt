/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import com.sonar.sslr.impl.Lexer
import com.sonar.sslr.impl.channel.BlackHoleChannel
import com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel
import com.sonar.sslr.impl.channel.RegexpChannelBuilder.*
import com.sonar.sslr.impl.channel.UnknownCharacterChannel
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType

object PlSqlLexer {
    private const val INLINE_COMMENT = "--[^\\n\\r]*+"
    private const val MULTILINE_COMMENT = "/\\*[\\s\\S]*?\\*\\/"
    private const val COMMENT = "(?:$INLINE_COMMENT|$MULTILINE_COMMENT)"

    private const val INTEGER_LITERAL = "(?:\\d++)"

    private val NUMBER_LITERAL = "(?is)(?:" + or(
        "((\\d++(?![.][.])[.]\\d*+)|(?![.][.])[.]\\d++)(e[+-]?\\d++)?[fd]?", // decimal value in floating-point literal
        "\\d++(e[+-]?\\d++)?[fd]", // integer value in floating-point literal
        "\\d++(e[+-]?\\d++)") + ")" // number literal in scientific notation

    private const val CUSTOM_DELIMITER_START = "[^\\s{\\[<\\(]" // any except spacing
    private const val CUSTOM_DELIMITER_END = "\\5" // same as the start

    private val STRING_LITERAL = ("(?is)(?:"
        + or("'([^']|'')*+'", // simple text literal
        "n?q?'" + or(g(g(CUSTOM_DELIMITER_START) + ".*?(" + CUSTOM_DELIMITER_END + "')"),
            g("\\(.*?(\\)')"),
            g("\\[.*?(\\]')"),
            g("<.*?(>')"),
            g("\\{.*?(\\}')"))) // text with user-defined delimiter

        + ")")

    private const val DATE_LITERAL = "(?i)(?:DATE\\s*?'\\d{4}-\\d{2}-\\d{2}')"

    private val SIMPLE_IDENTIFIER = and("[\\w\\p{L}]", o2n("[\\w\\p{L}#$]"))

    private const val QUOTED_IDENTIFIER = "\".+?\""

    fun create(conf: PlSqlConfiguration): Lexer =
        Lexer.builder()
            .withCharset(conf.charset)
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(BlackHoleChannel("\\s(?!&)"))
            .withChannel(commentRegexp(COMMENT))
            .withChannel(regexp(PlSqlTokenType.NUMBER_LITERAL, NUMBER_LITERAL))
            .withChannel(regexp(PlSqlTokenType.INTEGER_LITERAL, INTEGER_LITERAL))
            .withChannel(regexp(PlSqlTokenType.STRING_LITERAL, STRING_LITERAL))
            .withChannel(regexp(PlSqlTokenType.DATE_LITERAL, DATE_LITERAL))
            .withChannel(IdentifierAndKeywordChannel(or(SIMPLE_IDENTIFIER, QUOTED_IDENTIFIER), false, PlSqlKeyword.values()))
            .withChannel(RegexPunctuatorChannel(*PlSqlPunctuator.values()))
            .withChannel(BlackHoleChannel("(?is)" + or(
                "\\s&&?$SIMPLE_IDENTIFIER",
                "\\\$if.*?\\\$then",
                "\\\$else.*?\\\$end",
                "\\\$error.*?\\\$end",
                "\\\$end"
            )))
            .withChannel(UnknownCharacterChannel())
            .build()
}
