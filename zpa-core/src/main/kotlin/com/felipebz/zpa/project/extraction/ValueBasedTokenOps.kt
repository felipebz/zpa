//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName

/*
 * Value-based token operations used by permissive declaration parsing: they compare upper-cased token values instead of
 * token types. These operations intentionally preserve historical EOF fallbacks and delimiter-matching behavior
 * (parenthesis-only matching, last-token fallbacks).
 * Moved verbatim from the original single-class extractor.
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
