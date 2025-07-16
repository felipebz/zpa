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

import com.felipebz.flr.impl.Lexer
import com.felipebz.flr.impl.channel.BlackHoleChannel
import com.felipebz.flr.impl.channel.IdentifierAndKeywordChannel
import com.felipebz.flr.impl.channel.PunctuatorChannel
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.and
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.g
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.o2n
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.or
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.regexp
import com.felipebz.flr.impl.channel.UnknownCharacterChannel
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType

object PlSqlLexer {
    private val NUMBER_LITERAL = "(?is)(?:" + or(
        """(?:(?:\d++(?![.][.])[.]\d*+)|(?![.][.])[.]\d++)(?:e[+-]?\d++)?[fd]?""", // decimal value in floating-point literal
        """\d++(?:e[+-]?\d++)?[fd]""", // integer value in floating-point literal
        """\d++(?:e[+-]?\d++)""" // number literal in scientific notation
    ) + ")"

    private const val CUSTOM_DELIMITER_START = """[^\s{\[<\(]""" // any except spacing
    private const val CUSTOM_DELIMITER_END = """\1""" // same as the start

    private val STRING_LITERAL = ("(?is)(?:"
        + or("""?:n?'(?:[^']|'')*+'""", // simple text literal
        "n?q?'" + or("?:" + g("?:" + g(CUSTOM_DELIMITER_START) + ".*?(?:" + CUSTOM_DELIMITER_END + "')"),
            g("""?:\(.*?\)'"""),
            g("""?:\[.*?\]'"""),
            g("""?:<.*?>'"""),
            g("""?:\{.*?\}'"""))) // text with user-defined delimiter
        + ")")

    private const val DATE_LITERAL = """(?i)(?:DATE\s*?'\d{4}-\d{2}-\d{2}')"""

    private const val TIMESTAMP_LITERAL = """(?i)TIMESTAMP\s*?'\d{4}-\d{2}-\d{2}\s++\d{1,2}:\d{2}:\d{2}(?:.\d{1,9})?(?:\s++[A-Z0-9_/+-:]++(?:\s++[A-Z0-9_/+-]{1,5})?)?'"""

    private val SIMPLE_IDENTIFIER = and("""[\w\p{L}]""", o2n("""[\w\p{L}#$]"""))

    private const val QUOTED_IDENTIFIER = """".+?""""

    fun create(conf: PlSqlConfiguration): Lexer =
        Lexer.builder()
            .withCharset(conf.charset)
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(DiscardWhitespaceChannel())
            .withChannel(CommentChannel())
            .withChannel(NumericChannel(regexp(PlSqlTokenType.NUMBER_LITERAL, NUMBER_LITERAL)))
            .withChannel(IntegerChannel())
            .withChannel(StringChannel(regexp(PlSqlTokenType.STRING_LITERAL, STRING_LITERAL)))
            .withChannel(DateChannel(regexp(PlSqlTokenType.DATE_LITERAL, DATE_LITERAL)))
            .withChannel(DateChannel(regexp(PlSqlTokenType.TIMESTAMP_LITERAL, TIMESTAMP_LITERAL)))
            .withChannel(IdentifierChannel(IdentifierAndKeywordChannel(SIMPLE_IDENTIFIER, false,
                PlSqlKeyword.entries.toTypedArray()
            )))
            .withChannel(QuotedIdentifierChannel(QUOTED_IDENTIFIER, SIMPLE_IDENTIFIER))
            .withChannel(PunctuatorChannel(*PlSqlPunctuator.entries.toTypedArray()))
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
