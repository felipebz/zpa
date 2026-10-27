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
package com.felipebz.zpa.api

/**
 * Where an unquoted identifier starts and how it goes on, as the lexer reads it, by code point:
 * - it starts with a letter of the Basic Multilingual Plane ([Character.isLetter]); a supplementary letter does not
 *   start one;
 * - it goes on with ASCII letters, digits, `_`, `$` and `#`, and with any letter, supplementary ones included.
 *
 * Its lookup name is the text upper-cased with `Locale.ROOT`, as [com.felipebz.zpa.project.OracleIdentifier] does.
 */
object UnquotedIdentifier {
    /** Whether [codePoint] starts an unquoted identifier. */
    @JvmStatic
    fun isStart(codePoint: Int): Boolean =
        if (codePoint < 0x80) isAsciiLetter(codePoint) else codePoint <= Character.MAX_VALUE.code && Character.isLetter(codePoint)

    /** Whether [codePoint] goes on an unquoted identifier. */
    @JvmStatic
    fun isPart(codePoint: Int): Boolean =
        if (codePoint < 0x80) {
            isAsciiLetter(codePoint) || codePoint in '0'.code..'9'.code || codePoint == '_'.code || codePoint == '$'.code || codePoint == '#'.code
        } else {
            Character.isLetter(codePoint)
        }

    /** The ASCII letters are the only letters below 0x80. */
    private fun isAsciiLetter(codePoint: Int) = codePoint in 'a'.code..'z'.code || codePoint in 'A'.code..'Z'.code
}
