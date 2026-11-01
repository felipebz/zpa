//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.Token
import kotlin.math.min

internal class LineOffsets(code: String) {
    private val lineOffsets: MutableMap<Int, Int> = HashMap()
    private val endOffset: Int

    fun getStartOffset(token: Token): Int {
        return getOffset(token.line, token.column)
    }

    fun getEndOffset(token: Token): Int {
        return getOffset(token.endLine, token.endColumn)
    }

    fun getOffset(line: Int, column: Int): Int {
        require(line >= 1)
        require(column >= 0)
        val lineOffset = lineOffsets[line]
        return if (lineOffset != null) {
            min(lineOffset + column, endOffset)
        } else {
            endOffset
        }
    }

    companion object {
        private val NEWLINE_REGEX = Regex("\\R")
    }

    init {
        var currentOffset = 0
        val lines = code.split(NEWLINE_REGEX).toTypedArray()
        for (line in 1..lines.size) {
            lineOffsets[line] = currentOffset
            currentOffset += lines[line - 1].length + 1
        }
        endOffset = currentOffset - 1
    }
}
