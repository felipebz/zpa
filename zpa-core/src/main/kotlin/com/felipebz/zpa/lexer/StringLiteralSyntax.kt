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

import com.felipebz.flr.impl.channel.RegexpChannelBuilder.g
import com.felipebz.flr.impl.channel.RegexpChannelBuilder.or

/**
 * The syntax of a PL/SQL character literal, in one place: the lexer's pattern for `STRING_LITERAL` tokens and the
 * decoding of such a token into the value Oracle sees.
 *
 * Forms: `'…'` (a doubled `''` stands for one quote), `N'…'`, and the alternative quoting `q'X…X'` / `nq'X…X'`, where
 * `X` is any character except blanks and the pairs `[]`, `()`, `<>`, `{}` close with their partner. No escaping applies
 * inside the alternative quoting. Prefixes are case-insensitive.
 */
internal object StringLiteralSyntax {
    private const val CUSTOM_DELIMITER_START = """[^\s{\[<\(]""" // any except spacing
    private const val CUSTOM_DELIMITER_END = """\1""" // same as the start

    val PATTERN = ("(?is)(?:"
        + or("""?:n?'(?:[^']|'')*+'""", // simple text literal
        "n?q?'" + or("?:" + g("?:" + g(CUSTOM_DELIMITER_START) + ".*?(?:" + CUSTOM_DELIMITER_END + "')"),
            g("""?:\(.*?\)'"""),
            g("""?:\[.*?\]'"""),
            g("""?:<.*?>'"""),
            g("""?:\{.*?\}'"""))) // text with user-defined delimiter
        + ")")

    /** The value of a literal and, for each of its characters, its offset in the token's original text. */
    class Decoded(val value: String, val offsets: IntArray)

    /** Decodes the original text of a `STRING_LITERAL` token; null when it is not one of the literal forms. */
    fun decode(original: String): Decoded? {
        var start = 0
        if (original.startsWith("n", ignoreCase = true)) start++
        val alternative = original.startsWith("q'", start, ignoreCase = true)
        if (alternative) start++
        if (!original.startsWith("'", start) || !original.endsWith("'")) return null
        start++
        val value = StringBuilder()
        val offsets = ArrayList<Int>()
        if (alternative) {
            if (original.length < start + 3) return null
            val open = original[start]
            val close = when (open) {
                '[' -> ']'
                '(' -> ')'
                '<' -> '>'
                '{' -> '}'
                else -> open
            }
            val end = original.length - 2
            if (end <= start || original[end] != close) return null
            for (index in start + 1 until end) {
                value.append(original[index])
                offsets += index
            }
        } else {
            var index = start
            val end = original.length - 1
            if (end < start) return null
            while (index < end) {
                value.append(original[index])
                offsets += index
                index += if (original[index] == '\'' && index + 1 < end && original[index + 1] == '\'') 2 else 1
            }
        }
        return Decoded(value.toString(), offsets.toIntArray())
    }
}
