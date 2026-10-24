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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlPunctuator
import java.util.*

/**
 * Unit boundaries of one file: real `CREATE` keywords, lone-line `/` and EOF. The sorted index array is built
 * once, lazily (default synchronized `lazy`, as before), and lives exactly as long as the extraction.
 */
internal class UnitBounds(private val stream: TokenStream) {
    /** Sorted indices of unit boundaries: real CREATE keywords, lone-line `/` and EOF. Built once. */
    private val unitBoundaries: IntArray by lazy {
        stream.tokens.indices.filter {
            stream.isKeyword(it, "CREATE") || isLoneSlash(it) || stream.tokens[it].type == GenericTokenType.EOF
        }.toIntArray()
    }

    private val loneSlashes: IntArray by lazy { unitBoundaries.filter(::isLoneSlash).toIntArray() }

    private fun isLoneSlash(index: Int): Boolean {
        if (!stream.isPunctuator(index, PlSqlPunctuator.DIVISION)) return false
        val token = stream.tokens[index]
        val previousOnEarlierLine = index == 0 || stream.tokens[index - 1].endLine < token.line
        val next = stream.tokens.getOrNull(index + 1)
        val nextOnLaterLine = next == null || next.type == GenericTokenType.EOF || next.line > token.endLine
        return previousOnEarlierLine && nextOnLaterLine
    }

    /** First boundary strictly after [create]; scans of that unit never reach it. */
    fun unitLimit(create: Int): Int = firstFrom(unitBoundaries, create + 1)

    /**
     * Last token of a SQL statement that continues at [start]: its top-level `;`, or, as in SQL*Plus, the token before
     * the next `/` alone on its line. That is `start - 1` when the `/` is at [start].
     */
    fun statementEnd(start: Int): Int = minOf(stream.semicolonAfter(start), firstFrom(loneSlashes, start) - 1)

    private fun firstFrom(sorted: IntArray, index: Int): Int {
        var position = Arrays.binarySearch(sorted, index)
        if (position < 0) position = -position - 1
        return if (position < sorted.size) sorted[position] else stream.tokens.size
    }
}
