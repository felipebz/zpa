//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.lexer

import com.felipebz.flr.impl.Lexer
import com.felipebz.flr.impl.channel.BlackHoleChannel
import com.felipebz.flr.impl.channel.PunctuatorChannel
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.and
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.o2n
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.or
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.regexp
import com.felipebz.flr.impl.channel.UnknownCharacterChannel
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.squid.PlSqlConfiguration

object PlSqlLexer {
    private val NUMBER_LITERAL = "(?is)(?:" + or(
        """(?:(?:\d++(?![.][.])[.]\d*+)|(?![.][.])[.]\d++)(?:e[+-]?\d++)?[fd]?""", // decimal value in floating-point literal
        """\d++(?:e[+-]?\d++)?[fd]""", // integer value in floating-point literal
        """\d++(?:e[+-]?\d++)""" // number literal in scientific notation
    ) + ")"

    // Oracle tolerates padding inside the literal.
    private const val DATE_LITERAL = """(?i)(?:DATE\s*?'\s*\d{1,4}-\d{1,2}-\d{1,2}\s*')"""

    // Undocumented, but Oracle 26 parses TIME followed by any string literal and validates the value afterwards.
    private val TIME_LITERAL = "(?i)TIME\\s*?${StringLiteralSyntax.PATTERN}"

    private const val TIMESTAMP_LITERAL = """(?i)TIMESTAMP\s*?'\d{4}-\d{2}-\d{2}\s++\d{1,2}:\d{2}:\d{2}(?:.\d{1,9})?(?:\s++[A-Z0-9_/+-:]++(?:\s++[A-Z0-9_/+-]{1,5})?)?'"""

    private val SIMPLE_IDENTIFIER = and("""[\w\p{L}]""", o2n("""[\w\p{L}#$]"""))

    private const val QUOTED_IDENTIFIER = """".+?""""

    fun create(conf: PlSqlConfiguration): Lexer =
        Lexer.builder()
            .withCharset(conf.charset)
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(DiscardWhitespaceChannel())
            .withChannel(MleInlineSourceChannel())
            .withChannel(CommentChannel())
            .withChannel(MleModuleSourceChannel())
            .withChannel(WrappedSourceChannel())
            .withChannel(GraphQlDualitySourceChannel())
            .withChannel(NumericChannel(regexp(PlSqlTokenType.NUMBER_LITERAL, NUMBER_LITERAL)))
            .withChannel(IntegerChannel())
            .withChannel(StringChannel(regexp(PlSqlTokenType.STRING_LITERAL, StringLiteralSyntax.PATTERN)))
            .withChannel(DateChannel(regexp(PlSqlTokenType.DATE_LITERAL, DATE_LITERAL), 'd'))
            .withChannel(DateChannel(regexp(PlSqlTokenType.TIMESTAMP_LITERAL, TIMESTAMP_LITERAL), 't'))
            .withChannel(DateChannel(regexp(PlSqlTokenType.TIME_LITERAL, TIME_LITERAL), 't'))
            .withChannel(IdentifierChannel(PlSqlKeyword.entries.toTypedArray()))
            .withChannel(QuotedIdentifierChannel(QUOTED_IDENTIFIER, SIMPLE_IDENTIFIER))
            .withChannel(BlackHoleChannel("(?is)\\s&&?$SIMPLE_IDENTIFIER"))
            .withChannel(ConditionalCompilationChannel(conf.conditionalCompilation))
            .withChannel(PunctuatorChannel(*PlSqlPunctuator.entries.toTypedArray()))
            .withChannel(UnknownCharacterChannel())
            .build()
}
