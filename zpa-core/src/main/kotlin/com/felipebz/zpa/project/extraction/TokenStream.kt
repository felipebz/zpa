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
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange
import java.util.Locale
import kotlin.collections.ArrayDeque

/**
 * Read-only, per-extraction view of one lexed file: its identity and the lexer's token list (never copied,
 * never retained beyond the extraction that created it). It only holds the primitive accessors shared by every
 * extraction path; strict (token-type) and value-based operations live in separate extension files.
 */
internal class TokenStream(val fileId: FileId, val tokens: List<Token>) {
    fun tokenTypeAt(index: Int) = tokens.getOrNull(index)?.type
    fun isPunctuator(index: Int, punctuator: PlSqlPunctuator): Boolean =
        index in tokens.indices && tokens[index].type === punctuator

    fun isKeyword(index: Int, keyword: String): Boolean =
        index in tokens.indices && tokens[index].type is PlSqlKeyword && valueAt(index) == keyword

    /** Finds a syntactic keyword at delimiter depth zero; quoted identifiers never match. */
    fun firstKeywordAtTopLevel(start: Int, wanted: Set<String>, endExclusive: Int = tokens.size): Int? {
        val expected = ArrayDeque<PlSqlPunctuator>()
        for (index in start until endExclusive.coerceAtMost(tokens.size)) {
            when {
                isOpener(index) -> expected.addLast(closerFor(index))
                isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return null
                expected.isEmpty() && wanted.any { isKeyword(index, it) } -> return index
            }
        }
        return null
    }
    fun range(start: Int, end: Int): SourceRange {
        val first = tokens[start.coerceIn(0, tokens.lastIndex)]
        val last = tokens[end.coerceIn(0, tokens.lastIndex)]
        return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
    }

    fun isName(token: Token): Boolean =
        token.type == GenericTokenType.IDENTIFIER ||
            (token.type is PlSqlKeyword && !(token.type as PlSqlKeyword).isReserved)

    fun valueAt(index: Int) = if (index in tokens.indices) tokens[index].value.uppercase(Locale.ROOT) else ""

    fun value(token: Token) = token.value.uppercase(Locale.ROOT)

    fun identifier(token: Token) = OracleIdentifier.fromToken(token.originalValue, token.value)
}
