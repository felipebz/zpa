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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.CallableCoverageGap
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.extraction.DiscardedSourceText
import java.util.*

/**
 * The completeness evidence of one file, in token-index space (indexes into the parser's token list, `EOF` last). It is
 * construction-only, like the drafts: it holds the token list and must not escape a build call.
 *
 * - **Points**: hidden text between two consecutive tokens `a` and `b` — a conditional-compilation branch boundary
 *   reported by the lexer as trivia of `b`, or text the lexer discarded (substitutions, other discarded text, a `$`
 *   left in a gap). A point is recorded as the indexes of `a` and `b`; a region containing either is touched.
 * - **Spans**: the kept branches of conditional-compilation selections (tokens that may not exist for Oracle), as
 *   disjoint, sorted token-index ranges.
 * - **Recovery**: the token index where each file-level `RECOVERY` node starts.
 */
internal class LocalCompletenessEvidence private constructor(
    private val tokens: List<Token>,
    /** Sorted token indexes touched by points, per reason. */
    private val points: Map<LocalIncompleteness, IntArray>,
    private val spans: List<IntRange>,
    private val recoveryStarts: IntArray,
    /** Reasons that apply to every region. */
    private val global: Set<LocalIncompleteness>
) {
    /** Index of the last token before `EOF`, or -1. */
    private val lastToken = tokens.indexOfLast { it.type !== GenericTokenType.EOF }

    /** The completeness of every region, by region id. */
    fun attribute(regions: List<RegionDraft>): List<LocalRegionCompleteness> {
        // The file region holds the whole text, including hidden text before the first token and after the last.
        // A region whose ends are not token boundaries (never seen) is treated as the whole file, conservatively.
        val bounds = regions.map { if (it.kind == LocalRegionKind.FILE) tokens.indices else tokenBounds(it.range) ?: tokens.indices }
        val beforeRecovery = topLevelBeforeRecovery(regions, bounds)
        return regions.mapIndexed { index, region ->
            val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
            reasons += global
            if (region.gaps.isNotEmpty()) reasons += LocalIncompleteness.PARTIAL_EXTRACTION
            val range = bounds[index]
            for ((reason, indexes) in points) if (anyWithin(indexes, range)) reasons += reason
            if (spans.any { it.first <= range.last && range.first <= it.last }) reasons += LocalIncompleteness.CONDITIONAL_COMPILATION
            if (recoveryStarts.isNotEmpty() && (range.last >= recoveryStarts[0] || index in beforeRecovery)) {
                reasons += LocalIncompleteness.PARSE_RECOVERY
            }
            LocalRegionCompleteness.of(reasons)
        }
    }

    /** For each file-level `RECOVERY`, the top-level region (child of the file region) right before it. */
    private fun topLevelBeforeRecovery(regions: List<RegionDraft>, bounds: List<IntRange>): Set<Int> {
        if (recoveryStarts.isEmpty()) return emptySet()
        val topLevel = regions.indices.filter { regions[it].parent?.kind == LocalRegionKind.FILE }
        val result = HashSet<Int>()
        for (start in recoveryStarts) {
            topLevel.filter { bounds[it].last < start }.maxByOrNull { bounds[it].last }?.let { result += it }
        }
        return result
    }

    private fun anyWithin(sorted: IntArray, range: IntRange): Boolean {
        var low = Arrays.binarySearch(sorted, range.first)
        if (low < 0) low = -low - 1
        return low < sorted.size && sorted[low] <= range.last
    }

    /** The token indexes a region range covers, or null if its ends are not token boundaries. */
    private fun tokenBounds(range: SourceRange): IntRange? {
        if (lastToken < 0) return null
        val first = search { compareValues(it.line, range.startLine).takeIf { c -> c != 0 } ?: compareValues(it.column, range.startColumn) }
        val last = search { compareValues(it.endLine, range.endLine).takeIf { c -> c != 0 } ?: compareValues(it.endColumn, range.endColumn) }
        return if (first == null || last == null || last < first) null else first..last
    }

    /** Binary search over the tokens before `EOF` (positions increase with the index). */
    private inline fun search(compare: (Token) -> Int): Int? {
        var low = 0
        var high = lastToken
        while (low <= high) {
            val middle = (low + high) ushr 1
            val c = compare(tokens[middle])
            when {
                c < 0 -> low = middle + 1
                c > 0 -> high = middle - 1
                else -> return middle
            }
        }
        return null
    }

    companion object {
        /**
         * Reads the evidence of the file whose parse tree is [root] and whose text is [source] (null when unknown):
         * conditional-compilation trivia from its token types, other discarded text through [DiscardedSourceText],
         * and recovery nodes from the tree.
         */
        fun of(root: AstNode, source: String?): LocalCompletenessEvidence {
            val tokens = parserTokens(root)
            val global = EnumSet.noneOf(LocalIncompleteness::class.java)
            val points = HashMap<LocalIncompleteness, MutableList<Int>>()
            fun point(reason: LocalIncompleteness, before: Int) {
                val list = points.getOrPut(reason) { ArrayList() }
                if (before > 0) list += before - 1
                list += before
            }

            // Conditional compilation, from the lexer's trivia types.
            val runs = ArrayList<Pair<Int, Boolean>>() // (token index, opens a level)
            for ((index, token) in tokens.withIndex()) {
                for (trivia in token.trivia) {
                    if (trivia.token.type === PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY) {
                        point(LocalIncompleteness.CONDITIONAL_COMPILATION, index)
                        runs += index to trivia.token.originalValue.startsWith("\$IF", ignoreCase = true)
                    }
                }
            }

            // Other discarded text, including before the first token and after the last one.
            var dollarFallback = false
            if (source == null) {
                global += LocalIncompleteness.SOURCE_TEXT_UNAVAILABLE
            } else {
                for (segment in DiscardedSourceText(source).fileSegments(tokens)) {
                    when (segment.kind) {
                        CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS -> global += LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS
                        CallableCoverageGap.SUBSTITUTION -> point(LocalIncompleteness.SUBSTITUTION, segment.before)
                        CallableCoverageGap.DISCARDED_SOURCE_TEXT -> point(LocalIncompleteness.DISCARDED_SOURCE_TEXT, segment.before)
                        CallableCoverageGap.CONDITIONAL_COMPILATION -> {
                            dollarFallback = true
                            point(LocalIncompleteness.CONDITIONAL_COMPILATION, segment.before)
                        }
                        // Callable-heading gaps are package-extraction verdicts; segments never report them.
                        else -> Unit
                    }
                }
            }

            val spans = keptBranches(runs, dollarFallback, points[LocalIncompleteness.CONDITIONAL_COMPILATION].orEmpty())

            // Recovery: file-level nodes only; one below the file level taints everything.
            val recovery = root.getDescendants(PlSqlGrammar.RECOVERY)
            val recoveryStarts = if (recovery.isEmpty()) IntArray(0) else {
                if (recovery.any { it.parent !== root }) global += LocalIncompleteness.PARSE_RECOVERY
                val index = IdentityHashMap<Token, Int>()
                tokens.forEachIndexed { i, token -> index[token] = i }
                recovery.mapNotNull { if (it.hasToken()) index[it.token] else null }.sorted().toIntArray()
            }

            return LocalCompletenessEvidence(
                tokens, points.mapValues { it.value.distinct().sorted().toIntArray() }, spans, recoveryStarts, global
            )
        }

        /**
         * The tokens of the tree in order, each once. `AstNode.tokens` repeats a token reached through two nodes; without
         * the repeats it equals the lexer output, except where the grammar consumes tokens without nodes (the Java source
         * of `CREATE JAVA`), which [DiscardedSourceText] then reports as discarded text.
         */
        private fun parserTokens(root: AstNode): List<Token> {
            val tokens = root.tokens
            val seen = Collections.newSetFromMap(IdentityHashMap<Token, Boolean>(tokens.size))
            return tokens.filter { seen.add(it) }
        }

        /**
         * The kept-branch spans: boundary runs pair in token order (a run starting with `$IF` opens a level, any other
         * closes one); each top-level construct spans from the token carrying its opening run to the token before the
         * one carrying its closing run. When the runs do not pair, or a `$` was left in a gap, one span from the first
         * to the last conditional point stands for the whole file.
         */
        private fun keptBranches(runs: List<Pair<Int, Boolean>>, dollarFallback: Boolean, conditionalPoints: List<Int>): List<IntRange> {
            val spans = ArrayList<IntRange>()
            var depth = 0
            var start = -1
            var paired = true
            for ((index, opens) in runs) {
                if (opens) {
                    if (depth++ == 0) start = index
                } else {
                    if (--depth < 0) {
                        paired = false
                        break
                    }
                    if (depth == 0 && start <= index - 1) spans += start until index
                }
            }
            if (depth != 0) paired = false
            if (paired && !dollarFallback) return spans
            if (conditionalPoints.isEmpty()) return emptyList()
            val first = conditionalPoints.min()
            val last = conditionalPoints.max()
            return if (first <= last) listOf(first..last) else emptyList()
        }
    }
}
