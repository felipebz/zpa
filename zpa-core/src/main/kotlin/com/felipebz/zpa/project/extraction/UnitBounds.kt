//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
