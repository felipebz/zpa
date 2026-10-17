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

internal data class BodyHeaderDelimiter(
    val kind: BodyHeaderDelimiterKind,
    val index: Int
)

internal enum class BodyHeaderDelimiterKind {
    IMPLEMENTATION,
    FORWARD_DECLARATION
}

internal enum class BlockKind {
    BEGIN,
    CASE,
    IF,
    LOOP
}

/** What an `END` token did to the stack of open blocks. */
internal enum class BlockClose {
    /** `END IF` or `END LOOP` closed its own `IF` or `LOOP`. */
    STATEMENT,
    CASE,
    BEGIN,
    UNBALANCED
}

/**
 * Value-based boundary scanning of subprogram bodies and implementations: BEGIN/CASE/IF/LOOP block
 * tracking and nested-subprogram skipping. It only identifies token indices and never produces facts.
 */
internal class BodyBoundaryScanner(private val stream: TokenStream) {
    fun firstBodyHeaderDelimiter(start: Int, end: Int): BodyHeaderDelimiter? {
        var depth = 0
        for (index in start until end.coerceAtMost(stream.tokens.size)) {
            when (stream.valueAt(index)) {
                "(", "[" -> depth++
                ")", "]" -> depth--
            }
            if (depth == 0) {
                when (stream.valueAt(index)) {
                    "IS", "AS" -> return BodyHeaderDelimiter(BodyHeaderDelimiterKind.IMPLEMENTATION, index)
                    ";" -> return BodyHeaderDelimiter(BodyHeaderDelimiterKind.FORWARD_DECLARATION, index)
                }
            }
        }
        return null
    }

    fun implementationEnd(start: Int, end: Int): Int? {
        if (stream.valueAt(start) == "LANGUAGE" || stream.valueAt(start) == "EXTERNAL") {
            return stream.semicolonAfter(start).coerceAtMost(end)
        }
        val blocks = mutableListOf<BlockKind>()
        var bodyStarted = false
        var index = start
        while (index < end.coerceAtMost(stream.tokens.size)) {
            val nestedEnd = nestedSubprogramEnd(index, end, bodyStarted)
            if (nestedEnd != null) {
                index = nestedEnd + 1
                continue
            }
            val opened = statementStart(index)
            when {
                stream.isKeyword(index, "BEGIN") -> {
                    bodyStarted = true
                    blocks += BlockKind.BEGIN
                }
                opened != null -> blocks += opened
                stream.isKeyword(index, "END") -> {
                    val closed = closeBlock(index, blocks)
                    if (closed == BlockClose.UNBALANCED) return null
                    if (closed == BlockClose.BEGIN && bodyStarted && blocks.isEmpty()) return stream.semicolonAfter(index).coerceAtMost(end)
                }
            }
            index++
        }
        return null
    }

    /** The end of a nested subprogram that starts at [index], only before the body of the enclosing one has started. */
    private fun nestedSubprogramEnd(index: Int, end: Int, bodyStarted: Boolean): Int? =
        if (!bodyStarted && isSubprogramKeyword(index)) bodySubprogramEnd(index, end) else null

    /**
     * Applies the `END` at [index] to [blocks] by popping the innermost block. `END IF` / `END LOOP` close an open `IF` /
     * `LOOP`; on a `BEGIN` or `CASE` the word is only a label (Oracle accepts any identifier after a block's `END`), so
     * they close that block as `END x` does.
     */
    fun closeBlock(index: Int, blocks: MutableList<BlockKind>): BlockClose {
        val last = blocks.removeLastOrNull() ?: return BlockClose.UNBALANCED
        return when (last) {
            BlockKind.BEGIN -> BlockClose.BEGIN
            BlockKind.CASE -> BlockClose.CASE
            BlockKind.IF, BlockKind.LOOP -> BlockClose.STATEMENT
        }
    }

    fun bodySubprogramEnd(index: Int, end: Int): Int? {
        val name = stream.identifierAt(index + 1) ?: return null
        var cursor = name.second
        if (stream.valueAt(cursor) == "(") {
            val close = stream.matching(cursor)
            if (close >= end) return null
            cursor = close + 1
        }
        val header = firstBodyHeaderDelimiter(cursor, end) ?: return null
        return if (header.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
            header.index
        } else {
            implementationEnd(header.index + 1, end)
        }
    }

    fun executableBlockEnd(start: Int, end: Int): Int? {
        val blocks = mutableListOf<BlockKind>()
        var index = start
        while (index < end.coerceAtMost(stream.tokens.size)) {
            val opened = statementStart(index)
            when {
                stream.isKeyword(index, "BEGIN") -> blocks += BlockKind.BEGIN
                opened != null -> blocks += opened
                stream.isKeyword(index, "END") -> when (closeBlock(index, blocks)) {
                    BlockClose.UNBALANCED -> return null
                    BlockClose.BEGIN -> if (blocks.isEmpty()) return stream.semicolonAfter(index).coerceAtMost(end)
                    else -> Unit
                }
            }
            index++
        }
        return null
    }

    fun isSubprogramKeyword(index: Int): Boolean =
        stream.isKeyword(index, "PROCEDURE") || stream.isKeyword(index, "FUNCTION")

    /**
     * The block a `CASE`, `IF` or `LOOP` keyword at [index] opens, unless it follows `END`. A `FOR`/`WHILE` loop opens at
     * its `LOOP`. Quoted identifiers are not keywords, and conditional-compilation `$IF` is trivia, never a token.
     */
    fun statementStart(index: Int): BlockKind? {
        if (stream.isKeyword(index - 1, "END")) return null
        return when {
            stream.isKeyword(index, "CASE") -> BlockKind.CASE
            stream.isKeyword(index, "IF") -> BlockKind.IF
            stream.isKeyword(index, "LOOP") -> BlockKind.LOOP
            else -> null
        }
    }
}
