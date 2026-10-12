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
package com.felipebz.zpa.project.extraction

import com.felipebz.flr.api.Token
import com.felipebz.zpa.project.CallableCoverageGap

/**
 * Finds source text the lexer discarded (no token, no trivia) between consecutive tokens, by mapping token starts
 * back to the source string. Lines follow the lexer's cursor rules (LF, or CR not followed by LF) and columns are
 * character offsets (tab width 1, the lexer default). Used only during one extraction; nothing retains it.
 */
internal class DiscardedSourceText(private val source: String) {
    private val lineStarts: IntArray by lazy {
        val starts = ArrayList<Int>()
        starts += 0
        var index = 0
        while (index < source.length) {
            val c = source[index]
            if (c == '\n' || (c == '\r' && (index + 1 >= source.length || source[index + 1] != '\n'))) starts += index + 1
            index++
        }
        starts.toIntArray()
    }

    /**
     * The kind of the first discarded text between `tokens[from]` and `tokens[to]` (inclusive bounds, consecutive
     * pairs), [CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS] when a token cannot be located, or null when every
     * gap holds only whitespace and comment trivia.
     */
    fun firstGap(tokens: List<Token>, from: Int, to: Int): CallableCoverageGap? {
        var previousEnd = end(tokens[from]) ?: return CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS
        for (index in from + 1..to) {
            val token = tokens[index]
            val start = start(token) ?: return CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS
            var cursor = previousEnd
            for (trivia in token.trivia) {
                for (piece in trivia.tokens) {
                    val pieceStart = start(piece) ?: return CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS
                    classify(cursor, pieceStart)?.let { return it }
                    cursor = maxOf(cursor, pieceStart + piece.originalValue.length)
                }
            }
            classify(cursor, start)?.let { return it }
            previousEnd = start + token.originalValue.length
        }
        return null
    }

    private fun classify(from: Int, to: Int): CallableCoverageGap? {
        for (index in from until to) {
            val c = source[index]
            if (c.isWhitespace()) continue
            return when (c) {
                '$' -> CallableCoverageGap.CONDITIONAL_COMPILATION
                '&' -> CallableCoverageGap.SUBSTITUTION
                else -> CallableCoverageGap.DISCARDED_SOURCE_TEXT
            }
        }
        return null
    }

    private fun end(token: Token): Int? = start(token)?.let { it + token.originalValue.length }

    /** Offset of the token start, verified against its original spelling. */
    private fun start(token: Token): Int? {
        if (token.line < 1 || token.line > lineStarts.size) return null
        val offset = lineStarts[token.line - 1] + token.column
        return offset.takeIf { it <= source.length && source.startsWith(token.originalValue, it) }
    }
}
