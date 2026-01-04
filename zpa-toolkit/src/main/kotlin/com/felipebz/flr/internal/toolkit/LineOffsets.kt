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
