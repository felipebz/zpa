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

import com.felipebz.flr.api.Token
import com.felipebz.zpa.project.CallableCoverageGap

/**
 * Finds source text the lexer discarded (no token, no trivia) between consecutive tokens, by mapping token starts
 * back to the source string. Lines follow the lexer's cursor rules (LF, or CR not followed by LF) and columns are
 * character offsets (tab width 1, the lexer default). Used only during one extraction; nothing retains it.
 */
internal class DiscardedSourceText(private val source: String) {
    private val lineStarts: IntArray by lazy {
        val starts = ArrayList<Int>()
        starts += 0
        var index = 0
        while (index < source.length) {
            val c = source[index]
            if (c == '\n' || (c == '\r' && (index + 1 >= source.length || source[index + 1] != '\n'))) starts += index + 1
            index++
        }
        starts.toIntArray()
    }

    /**
     * The kind of the first discarded text between `tokens[from]` and `tokens[to]` (inclusive bounds, consecutive
     * pairs), [CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS] when a token cannot be located, or null when every
     * gap holds only whitespace and comment trivia.
     */
    fun firstGap(tokens: List<Token>, from: Int, to: Int): CallableCoverageGap? {
        var first: CallableCoverageGap? = null
        visitGaps(tokens, from, to) { kind, _, _, _ ->
            first = kind
            false
        }
        return first
    }

    /**
     * Every kind of discarded text between `tokens[from]` and `tokens[to]`, and the tokens lying between the first and
     * the last conditional-compilation directive that opens or closes a branch. The lexer drops `$IF … $THEN`,
     * `$ELSIF/$ELSE … $END`, `$ERROR … $END` and `$END` but keeps the tokens of a `$THEN` branch, so those tokens may be
     * absent for Oracle. A self-contained directive text (`$IF c $THEN $END`, `$ERROR … $END`) bounds no branch.
     * Scanning stops at an unlocatable token ([CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS]).
     */
    fun scan(tokens: List<Token>, from: Int, to: Int): Scan {
        val kinds = mutableSetOf<CallableCoverageGap>()
        var firstConditional = -1
        var lastConditional = -1
        var boundaries = 0
        visitGaps(tokens, from, to) { kind, before, segmentStart, segmentEnd ->
            kinds += kind
            if (kind == CallableCoverageGap.CONDITIONAL_COMPILATION && !selfContained(segmentStart, segmentEnd)) {
                if (firstConditional < 0) firstConditional = before
                lastConditional = before
                boundaries++
            }
            true
        }
        // A lone boundary (its pair lies outside the region) cannot bound a branch: everything is conditional.
        val conditionalTokens = when (boundaries) {
            0 -> null
            1 -> (from + 1) until to
            else -> firstConditional until lastConditional
        }
        return Scan(kinds, conditionalTokens?.takeUnless { it.isEmpty() })
    }

    /** [kinds] of discarded text; [conditionalTokens] are the indexes of tokens inside the conditional span, if any. */
    class Scan(val kinds: Set<CallableCoverageGap>, val conditionalTokens: IntRange?)

    /** Every `$END` in the text closes a `$IF`/`$ERROR` opened earlier in the same text, and none stays open. */
    private fun selfContained(from: Int, to: Int): Boolean {
        var depth = 0
        for (match in DIRECTIVE.findAll(source.substring(from, to))) {
            depth += if (match.groupValues[1].equals("end", ignoreCase = true)) -1 else 1
            if (depth < 0) return false
        }
        return depth == 0
    }

    /** Calls [visit] with each discarded kind, the index of the token right after it and the text offsets, until false. */
    private inline fun visitGaps(tokens: List<Token>, from: Int, to: Int, visit: (CallableCoverageGap, Int, Int, Int) -> Boolean) {
        var previousEnd = end(tokens[from]) ?: run {
            visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, from, 0, 0)
            return
        }
        for (index in from + 1..to) {
            val token = tokens[index]
            val start = start(token) ?: run {
                visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, index, 0, 0)
                return
            }
            var cursor = previousEnd
            for (trivia in token.trivia) {
                for (piece in trivia.tokens) {
                    val pieceStart = start(piece) ?: run {
                        visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, index, 0, 0)
                        return
                    }
                    if (!visitSegment(cursor, pieceStart, index, visit)) return
                    cursor = maxOf(cursor, pieceStart + piece.originalValue.length)
                }
            }
            if (!visitSegment(cursor, start, index, visit)) return
            previousEnd = start + token.originalValue.length
        }
    }

    /**
     * Visits the kind of one discarded segment, then conditional compilation again when a `$` follows another kind in
     * the same segment (for example `&x $END`), so the conditional span never misses a directive.
     */
    private inline fun visitSegment(from: Int, to: Int, before: Int, visit: (CallableCoverageGap, Int, Int, Int) -> Boolean): Boolean {
        val kind = classify(from, to) ?: return true
        if (!visit(kind, before, from, to)) return false
        if (kind != CallableCoverageGap.CONDITIONAL_COMPILATION && source.substring(from, to).contains('$')) {
            return visit(CallableCoverageGap.CONDITIONAL_COMPILATION, before, from, to)
        }
        return true
    }

    private fun classify(from: Int, to: Int): CallableCoverageGap? {
        for (index in from until to) {
            val c = source[index]
            if (c.isWhitespace()) continue
            return when (c) {
                '$' -> CallableCoverageGap.CONDITIONAL_COMPILATION
                '&' -> CallableCoverageGap.SUBSTITUTION
                else -> CallableCoverageGap.DISCARDED_SOURCE_TEXT
            }
        }
        return null
    }

    private fun end(token: Token): Int? = start(token)?.let { it + token.originalValue.length }

    /** Offset of the token start, verified against its original spelling. */
    private fun start(token: Token): Int? {
        if (token.line < 1 || token.line > lineStarts.size) return null
        val offset = lineStarts[token.line - 1] + token.column
        return offset.takeIf { it <= source.length && source.startsWith(token.originalValue, it) }
    }

    private companion object {
        /** `$IF`, `$ERROR` and `$END` directives, not inquiry directives such as `$$if_flag`. */
        val DIRECTIVE = Regex("(?i)(?<![\$\\w])\\$(if|error|end)(?!\\w)")
    }
}
