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
    CASE
}

/** What an `END` token did to the stack of open blocks. */
internal enum class BlockClose {
    IGNORED,
    CASE,
    BEGIN,
    UNBALANCED
}

/**
 * Value-based boundary scanning of subprogram bodies and implementations: BEGIN/END/CASE block
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
            when {
                stream.isKeyword(index, "BEGIN") -> {
                    bodyStarted = true
                    blocks += BlockKind.BEGIN
                }
                isCaseStart(index) -> blocks += BlockKind.CASE
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

    /** Applies the `END` at [index] to [blocks]: `END IF` and `END LOOP` close nothing, `END` pops the innermost block. */
    fun closeBlock(index: Int, blocks: MutableList<BlockKind>): BlockClose {
        if (stream.isKeyword(index + 1, "IF") || stream.isKeyword(index + 1, "LOOP")) return BlockClose.IGNORED
        val last = blocks.lastOrNull() ?: return BlockClose.UNBALANCED
        blocks.removeAt(blocks.lastIndex)
        return if (last == BlockKind.CASE) BlockClose.CASE else BlockClose.BEGIN
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
            when {
                stream.isKeyword(index, "BEGIN") -> blocks += BlockKind.BEGIN
                isCaseStart(index) -> blocks += BlockKind.CASE
                stream.isKeyword(index, "END") -> when {
                    stream.isKeyword(index + 1, "IF") || stream.isKeyword(index + 1, "LOOP") -> Unit
                    blocks.lastOrNull() == BlockKind.CASE -> blocks.removeAt(blocks.lastIndex)
                    blocks.lastOrNull() == BlockKind.BEGIN -> {
                        blocks.removeAt(blocks.lastIndex)
                        if (blocks.isEmpty()) return stream.semicolonAfter(index).coerceAtMost(end)
                    }
                    else -> return null
                }
            }
            index++
        }
        return null
    }

    fun isSubprogramKeyword(index: Int): Boolean =
        stream.isKeyword(index, "PROCEDURE") || stream.isKeyword(index, "FUNCTION")

    fun isCaseStart(index: Int): Boolean =
        stream.isKeyword(index, "CASE") && !stream.isKeyword(index - 1, "END")
}
