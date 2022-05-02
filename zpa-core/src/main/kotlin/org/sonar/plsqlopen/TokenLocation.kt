/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plsqlopen

import com.felipebz.flr.api.Token

class TokenLocation private constructor(
    private val line: Int,
    private val column: Int,
    private val endLine: Int,
    private val endColumn: Int
) {

    fun line() = line

    fun column() = column

    fun endLine() = endLine

    fun endColumn() = endColumn

    companion object {
        private val pattern = Regex("\\R")

        fun from(token: Token): TokenLocation {
            var lastLineLength = 0

            val lines = pattern.split(token.value)
            val lineCount = if (lines.size > 1) {
                lastLineLength = lines[lines.size - 1].length
                lines.size
            } else {
                1
            }

            val endLine = token.line + lineCount - 1
            val endLineOffset = if (endLine != token.line) {
                lastLineLength
            } else {
                token.column + token.value.length
            }
            return TokenLocation(token.line, token.column, endLine, endLineOffset)
        }
    }

}
