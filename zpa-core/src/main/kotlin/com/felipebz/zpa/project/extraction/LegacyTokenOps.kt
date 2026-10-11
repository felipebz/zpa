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

import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName

/*
 * Legacy (value based) operations over a [TokenStream]: string comparison on upper-cased token values, EOF
 * fallbacks and parenthesis-only matching. Their current behavior is deliberate and preserved.
 * Moved verbatim from the former TokenDeclarationScanner.
 */

internal fun TokenStream.firstAtTopLevel(start: Int, wanted: Set<String>, endExclusive: Int = tokens.size): Int? {
    var depth = 0
    for (index in start until endExclusive.coerceAtMost(tokens.size)) {
        when (valueAt(index)) {
            "(", "[" -> depth++
            ")", "]" -> depth--
        }
        if (depth == 0 && valueAt(index) in wanted) return index
    }
    return null
}

internal fun TokenStream.matching(open: Int): Int {
    var depth = 0
    for (index in open until tokens.size) {
        when (valueAt(index)) {
            "(" -> depth++
            ")" -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return tokens.lastIndex
}

internal fun TokenStream.semicolonAfter(start: Int): Int {
    var depth = 0
    for (index in start until tokens.size) {
        when (valueAt(index)) {
            "(", "[" -> depth++
            ")", "]" -> depth--
            ";" -> if (depth == 0) return index
        }
    }
    return tokens.lastIndex.coerceAtLeast(start)
}

internal fun TokenStream.nextValue(start: Int, wanted: String): Int? =
    (start until tokens.size).firstOrNull { valueAt(it) == wanted }

internal fun TokenStream.identifierAt(start: Int): Pair<OracleIdentifier, Int>? {
    if (start !in tokens.indices || !isName(tokens[start])) return null
    return identifier(tokens[start]) to start + 1
}

internal fun TokenStream.qualifiedName(start: Int): Pair<QualifiedName, Int>? {
    val first = identifierAt(start) ?: return null
    val segments = mutableListOf(first.first)
    var cursor = first.second
    while (valueAt(cursor) == ".") {
        val next = identifierAt(cursor + 1) ?: break
        segments += next.first
        cursor = next.second
    }
    return QualifiedName(segments) to cursor
}
