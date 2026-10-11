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
import com.felipebz.zpa.project.QualifiedName

/*
 * Strict (token-type based) operations over a [TokenStream]. They never compare token values for structure:
 * quoted identifiers expose unquoted values that look like keywords or punctuators, so keywords are checked
 * with `isKeyword` and punctuators with `isPunctuator`. Moved verbatim from the original single-class extractor.
 */

internal fun TokenStream.isNameBefore(index: Int, limit: Int) = index < limit && index in tokens.indices && isName(tokens[index])

internal fun TokenStream.isOpener(index: Int) =
    isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) || isPunctuator(index, PlSqlPunctuator.LBRACKET)

internal fun TokenStream.isCloser(index: Int) =
    isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) || isPunctuator(index, PlSqlPunctuator.RBRACKET)

internal fun TokenStream.closerFor(opener: Int) =
    if (isPunctuator(opener, PlSqlPunctuator.LPARENTHESIS)) PlSqlPunctuator.RPARENTHESIS else PlSqlPunctuator.RBRACKET

/**
 * Index of the `)` matching the `(` at [open], or null when nesting is unbalanced or crossed
 * (`(` must close with `)`, `[` with `]`) before [limit], or a `;`/CREATE appears inside.
 * Never falls back to the end of the file.
 */
internal fun TokenStream.strictClose(open: Int, limit: Int): Int? {
    if (!isPunctuator(open, PlSqlPunctuator.LPARENTHESIS)) return null
    val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
    for (index in open until limit.coerceAtMost(tokens.size)) {
        when {
            isOpener(index) -> expected.addLast(closerFor(index))
            isCloser(index) -> {
                if (expected.removeLastOrNull() !== tokens[index].type) return null
                if (expected.isEmpty()) return index
            }
            isPunctuator(index, PlSqlPunctuator.SEMICOLON) || isKeyword(index, "CREATE") -> return null
        }
    }
    return null
}

/**
 * Depth-0 comma-separated parts of `(open, close)`; null for `()`, any empty part, or any
 * crossed or unmatched delimiter (nesting is re-validated, not assumed from [close]).
 */
internal fun TokenStream.splitStrict(open: Int, close: Int): List<IntRange>? {
    if (close == open + 1 || strictClose(open, close + 1) != close) return null
    val parts = mutableListOf<IntRange>()
    val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
    var partStart = open + 1
    for (index in open + 1..close) {
        if (index == close || (expected.isEmpty() && isPunctuator(index, PlSqlPunctuator.COMMA))) {
            if (partStart >= index) return null
            parts += partStart until index
            partStart = index + 1
            continue
        }
        when {
            isOpener(index) -> expected.addLast(closerFor(index))
            isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return null
        }
    }
    return parts.takeIf { expected.isEmpty() }
}

/** One or two name segments joined by a real `.`; a third segment is a failure. */
internal fun TokenStream.strictQualifiedName(start: Int, limit: Int): Pair<QualifiedName, Int>? {
    if (!isNameBefore(start, limit)) return null
    val segments = mutableListOf(identifier(tokens[start]))
    var index = start + 1
    if (isPunctuator(index, PlSqlPunctuator.DOT)) {
        if (!isNameBefore(index + 1, limit)) return null
        segments += identifier(tokens[index + 1])
        index += 2
        if (isPunctuator(index, PlSqlPunctuator.DOT)) return null
    }
    return QualifiedName(segments) to index
}

internal fun TokenStream.nameEnd(index: Int, limit: Int): Int? = if (isNameBefore(index, limit)) index + 1 else null

/** `name {. name}` of any length (object references in RELIES_ON). */
internal fun TokenStream.strictQualifiedNameAnyLength(start: Int, limit: Int): Int? {
    var index = nameEnd(start, limit) ?: return null
    while (isPunctuator(index, PlSqlPunctuator.DOT)) index = nameEnd(index + 1, limit) ?: return null
    return index
}

/**
 * `( item {, item} )` starting at [open], where [item] returns the index after one item or
 * null. Requires at least one item, real commas and a real closing parenthesis.
 */
internal inline fun TokenStream.delimitedListEnd(open: Int, limit: Int, item: (Int) -> Int?): Int? {
    if (!isPunctuator(open, PlSqlPunctuator.LPARENTHESIS)) return null
    var index = open + 1
    while (true) {
        index = item(index) ?: return null
        if (index >= limit) return null
        if (isPunctuator(index, PlSqlPunctuator.RPARENTHESIS)) return index + 1
        if (!isPunctuator(index, PlSqlPunctuator.COMMA)) return null
        index++
    }
}

internal fun TokenStream.isUnquotedWord(index: Int, word: String): Boolean =
    index in tokens.indices && tokens[index].type == GenericTokenType.IDENTIFIER &&
        tokens[index].originalValue.equals(word, ignoreCase = true)
