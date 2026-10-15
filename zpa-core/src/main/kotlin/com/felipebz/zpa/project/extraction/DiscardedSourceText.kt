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
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.CallableCoverageGap

/**
 * Finds source text the lexer discarded (no token, no trivia) between consecutive tokens, by mapping token starts
 * back to the source string, and the conditional-compilation text the lexer reported as trivia
 * ([PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY], [PlSqlTokenType.CONDITIONAL_DIRECTIVE]). Lines follow the lexer's cursor rules (LF, or CR not followed by LF) and columns are
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
     * the last conditional-compilation text that opens or closes a kept branch. The lexer reports the directives as
     * trivia and keeps the tokens of one branch, so those tokens may be absent for Oracle: a
     * [PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY] trivia bounds that branch, a [PlSqlTokenType.CONDITIONAL_DIRECTIVE]
     * trivia (`$ERROR … $END`, a `$IF … $END` keeping nothing) bounds none. A `$` left in a gap counts as a boundary.
     * Scanning stops at an unlocatable token ([CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS]).
     */
    fun scan(tokens: List<Token>, from: Int, to: Int): Scan {
        val kinds = mutableSetOf<CallableCoverageGap>()
        var firstConditional = -1
        var lastConditional = -1
        var boundaries = 0
        visitGaps(tokens, from, to) { kind, before, boundsBranch, _ ->
            kinds += kind
            if (kind == CallableCoverageGap.CONDITIONAL_COMPILATION && boundsBranch) {
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

    /**
     * Every discarded gap of the file whose tokens are [tokens], in order, with the index of the token right after it:
     * before the first token, between tokens and after the last one. The `EOF` token's position is not checked (it has
     * no text), but its trivia is read. Conditional-compilation trivia is not included (read its token types instead);
     * a `CONDITIONAL_COMPILATION` segment here is a `$` left in a gap. Stops after an
     * [CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS] segment.
     */
    fun fileSegments(tokens: List<Token>): List<Segment> {
        val segments = ArrayList<Segment>()
        val last = tokens.indexOfLast { it.type !== GenericTokenType.EOF }
        if (last < 0) {
            edgeSegment(0, tokens.flatMap { it.trivia }, source.length, maxOf(0, tokens.size - 1), segments)
            return segments
        }
        val first = start(tokens[0])
        if (first == null) {
            segments += Segment(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, 0)
            return segments
        }
        if (!edgeSegment(0, tokens[0].trivia, first, 0, segments)) return segments
        var verified = true
        visitGaps(tokens, 0, last) { kind, before, _, trivia ->
            if (kind == CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS) verified = false
            if (!trivia) segments += Segment(kind, before)
            true
        }
        if (verified) {
            val trailing = tokens.subList(last + 1, tokens.size)
            edgeSegment(end(tokens[last])!!, trailing.flatMap { it.trivia }, source.length, last + 1, segments)
        }
        return segments
    }

    /** Classifies the text from [from] to [to] around [trivia]; false when a trivia piece cannot be located. */
    private fun edgeSegment(from: Int, trivia: List<Trivia>, to: Int, before: Int, segments: MutableList<Segment>): Boolean {
        var cursor = from
        for (piece in trivia.flatMap { it.tokens }) {
            val pieceStart = start(piece)
            if (pieceStart == null) {
                segments += Segment(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, before)
                return false
            }
            classify(cursor, pieceStart)?.let { segments += Segment(it, before) }
            cursor = maxOf(cursor, pieceStart + piece.originalValue.length)
        }
        classify(cursor, to)?.let { segments += Segment(it, before) }
        return true
    }

    /** A discarded gap of [kind] lying right before `tokens[before]`. */
    class Segment(val kind: CallableCoverageGap, val before: Int)

    /** [kinds] of discarded text; [conditionalTokens] are the indexes of tokens inside the conditional span, if any. */
    class Scan(val kinds: Set<CallableCoverageGap>, val conditionalTokens: IntRange?)

    /**
     * Calls [visit] with each discarded kind, the index of the token right after it, whether it bounds a kept
     * conditional-compilation branch and whether it comes from conditional-compilation trivia (rather than a gap),
     * until false.
     */
    private inline fun visitGaps(tokens: List<Token>, from: Int, to: Int, visit: (CallableCoverageGap, Int, Boolean, Boolean) -> Boolean) {
        var previousEnd = end(tokens[from]) ?: run {
            visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, from, false, false)
            return
        }
        for (index in from + 1..to) {
            val token = tokens[index]
            val start = start(token) ?: run {
                visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, index, false, false)
                return
            }
            var cursor = previousEnd
            for (trivia in token.trivia) {
                for (piece in trivia.tokens) {
                    val pieceStart = start(piece) ?: run {
                        visit(CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS, index, false, false)
                        return
                    }
                    if (!visitSegment(cursor, pieceStart, index, visit)) return
                    val boundsBranch = when (piece.type) {
                        PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY -> true
                        PlSqlTokenType.CONDITIONAL_DIRECTIVE -> false
                        else -> null
                    }
                    if (boundsBranch != null && !visit(CallableCoverageGap.CONDITIONAL_COMPILATION, index, boundsBranch, true)) return
                    cursor = maxOf(cursor, pieceStart + piece.originalValue.length)
                }
            }
            if (!visitSegment(cursor, start, index, visit)) return
            previousEnd = start + token.originalValue.length
        }
    }

    /**
     * Visits the kind of one discarded segment. Conditional-compilation text is never a gap (the lexer reports it as
     * trivia); a `$` found here is a conservative fallback and counts as a branch boundary.
     */
    private inline fun visitSegment(from: Int, to: Int, before: Int, visit: (CallableCoverageGap, Int, Boolean, Boolean) -> Boolean): Boolean {
        val kind = classify(from, to) ?: return true
        return visit(kind, before, kind == CallableCoverageGap.CONDITIONAL_COMPILATION, false)
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
}
