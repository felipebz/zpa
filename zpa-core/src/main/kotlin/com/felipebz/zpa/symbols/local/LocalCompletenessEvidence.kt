//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.CallableCoverageGap
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.extraction.DiscardedSourceText
import java.util.*

/**
 * The completeness evidence of one file, in token-index space (indexes into the parser's token list, `EOF` last). It is
 * construction-only, like the drafts: it holds the token list and must not escape a build call.
 *
 * - **Points**: hidden text between two consecutive tokens `a` and `b` — a conditional-compilation branch boundary
 *   reported by the lexer as trivia of `b`, or text the lexer discarded (substitutions, other discarded text, a `$`
 *   left in a gap). A point is recorded as the index of `b`. It is inside a region whose tokens hold both `a` and `b`;
 *   right before an anonymous block's first token too, where a hidden `DECLARE` section would belong to the block.
 *   Text right after a region's last token, or right before a subprogram or unit, is outside it.
 * - **Spans**: the kept branches of conditional-compilation selections (tokens that may not exist for Oracle), as
 *   disjoint, sorted token-index ranges.
 * - **Recovery**: the token index where each file-level `RECOVERY` node starts.
 *
 * Each conditional-compilation point and each span is also an undecided piece ([LocalConditionalPiece]) with the names
 * it mentions, read once here: a region's completeness keeps the pieces behind its conditional-compilation reason, so
 * that a lookup or an evidence counts it only for the names the text writes.
 */
internal class LocalCompletenessEvidence private constructor(
    private val tokens: List<Token>,
    /** Sorted token indexes touched by points, per reason. */
    private val points: Map<LocalIncompleteness, IntArray>,
    private val spans: List<IntRange>,
    private val recoveryStarts: IntArray,
    /** Reasons that apply to every region. */
    private val global: Set<LocalIncompleteness>,
    /**
     * Sorted token indexes of the conditional-compilation points whose dropped text is block-balanced: it cannot end the
     * block or subprogram it is written in, so among statements it declares nothing a lookup elsewhere passes through.
     */
    private val balanced: IntArray = IntArray(0),
    /** The piece of each conditional-compilation point, by its token index. */
    private val pointPieces: Map<Int, LocalConditionalPiece> = emptyMap(),
    /** The piece of each span, in [spans] order. */
    private val spanPieces: List<LocalConditionalPiece> = emptyList()
) {
    /** Index of the last token before `EOF`, or -1. */
    private val lastToken = tokens.indexOfLast { it.type !== GenericTokenType.EOF }

    /**
     * True when the tokens before `EOF` start at strictly increasing positions. Token bounds and the gap walk rely on
     * it; when it fails, [global] holds `UNVERIFIED_SOURCE_POSITIONS` and nothing is located.
     */
    private val ordered = isStrictlyOrdered(tokens)

    /** The completeness of every region, by region id. */
    fun attribute(regions: List<RegionDraft>): List<LocalRegionCompleteness> {
        if (!ordered) return unlocated(regions)
        // The file region holds the whole text, including hidden text before the first token and after the last.
        // A region whose ends are not token boundaries (never seen) is treated as the whole file, conservatively.
        val bounds = regions.map { if (it.kind == LocalRegionKind.FILE) tokens.indices else tokenBounds(it.range) ?: tokens.indices }
        val beforeRecovery = topLevelBeforeRecovery(regions, bounds)
        // Each CREATE and each top-level block is compiled on its own: hidden text inside one cannot declare or
        // change another, nor a name of the file. The file region keeps only what lies outside every unit.
        val units = regions.indices.filter { regions[it].parent?.kind == LocalRegionKind.FILE }.map { bounds[it] to regions[it].kind }
        val kinds = regions.associate { it.id to it.kind }
        return regions.mapIndexed { index, region ->
            val range = bounds[index]
            val reasons = wholeRegion(region, range, index in beforeRecovery)
            val lookup = EnumSet.copyOf(reasons)
            val pieces = ArrayList<LocalConditionalPiece>()
            val lookupPieces = ArrayList<LocalConditionalPiece>()
            if (region.kind == LocalRegionKind.FILE) {
                fileText(range, units, reasons, pieces)
                lookup += reasons
                lookupPieces += pieces
            } else {
                regionText(region, range, reasons, pieces)
                lookupText(region, range, bounds, kinds, lookup, lookupPieces)
            }
            LocalRegionCompleteness.of(reasons, lookup, pieces, lookupPieces)
        }
    }

    /** What applies to the whole region: the global reasons, partial extraction and recovery. */
    private fun wholeRegion(region: RegionDraft, range: IntRange, beforeRecovery: Boolean): EnumSet<LocalIncompleteness> {
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        reasons += global
        if (region.gaps.isNotEmpty()) reasons += LocalIncompleteness.PARTIAL_EXTRACTION
        if (recoveryStarts.isNotEmpty() && (range.last >= recoveryStarts[0] || beforeRecovery)) reasons += LocalIncompleteness.PARSE_RECOVERY
        return reasons
    }

    /** Nothing can be located: every region carries the global reasons, and recovery anywhere taints all. */
    private fun unlocated(regions: List<RegionDraft>): List<LocalRegionCompleteness> = regions.map { region ->
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        reasons += global
        if (region.gaps.isNotEmpty()) reasons += LocalIncompleteness.PARTIAL_EXTRACTION
        if (recoveryStarts.isNotEmpty()) reasons += LocalIncompleteness.PARSE_RECOVERY
        LocalRegionCompleteness.of(reasons)
    }

    /** The hidden text of the file region: what lies outside every unit. */
    private fun fileText(
        range: IntRange, units: List<Pair<IntRange, LocalRegionKind>>, reasons: MutableSet<LocalIncompleteness>, pieces: MutableList<LocalConditionalPiece>
    ) {
        for ((reason, indexes) in points) {
            val outside = indexes.filter { b -> units.none { (unit, kind) -> holds(unit, kind, b) } }
            if (outside.isNotEmpty()) reasons += reason
            if (reason == LocalIncompleteness.CONDITIONAL_COMPILATION) outside.forEach { pieces += pointPiece(it) }
        }
        spans.forEachIndexed { i, span ->
            if (span.first <= range.last && range.first <= span.last && units.none { (unit, _) -> unit.first <= span.first && span.last <= unit.last }) {
                reasons += LocalIncompleteness.CONDITIONAL_COMPILATION
                pieces += spanPiece(i)
            }
        }
    }

    /** All hidden text in the tokens of [region], nested regions included. */
    private fun regionText(region: RegionDraft, range: IntRange, reasons: MutableSet<LocalIncompleteness>, pieces: MutableList<LocalConditionalPiece>) {
        val own = ownTokens(region, range)
        for ((reason, indexes) in points) if (anyWithin(indexes, own)) reasons += reason
        points[LocalIncompleteness.CONDITIONAL_COMPILATION]?.forEach { if (it in own) pieces += pointPiece(it) }
        spans.forEachIndexed { i, span ->
            if (span.first <= range.last && range.first <= span.last) {
                reasons += LocalIncompleteness.CONDITIONAL_COMPILATION
                pieces += spanPiece(i)
            }
        }
    }

    /**
     * The hidden text that can change what a lookup through [region] finds: text never seen (a substitution, discarded
     * text, a dropped branch that may end a block), which may restructure the region wherever it is in it; and other
     * text in the region's declarative part, up to its statements (hidden text right before `BEGIN` included), less
     * the nested regions, which declare nothing the region holds.
     */
    private fun lookupText(
        region: RegionDraft, range: IntRange, bounds: List<IntRange>, kinds: Map<LocalRegionId, LocalRegionKind>, lookup: MutableSet<LocalIncompleteness>,
        pieces: MutableList<LocalConditionalPiece>
    ) {
        val own = ownTokens(region, range)
        for ((reason, indexes) in points) {
            val counted = indexes.filter { it in own && !(reason == LocalIncompleteness.CONDITIONAL_COMPILATION && isBalanced(it)) }
            if (counted.isNotEmpty()) lookup += reason
            if (reason == LocalIncompleteness.CONDITIONAL_COMPILATION) counted.forEach { pieces += pointPiece(it) }
        }
        val statements = region.statementsStart?.let { (line, column) -> tokenAt(line, column) }?.takeIf { it in range }
        val declarative = when {
            statements == null -> own
            region.statementsOpenWithBegin -> own.first..statements
            else -> own.first until statements
        }
        val children = region.children.map { child -> bounds[child.ordinal] to kinds.getValue(child) }
        for ((reason, indexes) in points) {
            val counted = indexes.filter { b -> b in declarative && children.none { (child, kind) -> holds(child, kind, b) } }
            if (counted.isNotEmpty()) lookup += reason
            if (reason == LocalIncompleteness.CONDITIONAL_COMPILATION) counted.forEach { pieces += pointPiece(it) }
        }
        val spanEnd = statements?.minus(1) ?: range.last
        spans.forEachIndexed { i, span ->
            if (span.first <= spanEnd && range.first <= span.last && children.none { (child, _) -> child.first <= span.first && span.last <= child.last }) {
                lookup += LocalIncompleteness.CONDITIONAL_COMPILATION
                pieces += spanPiece(i)
            }
        }
    }

    /** The piece of the conditional-compilation point at token [index]; wide when it was not read. */
    private fun pointPiece(index: Int) = pointPieces[index] ?: LocalConditionalPiece.WIDE

    /** The piece of span [index]; wide when it was not read. */
    private fun spanPiece(index: Int) = spanPieces.getOrNull(index) ?: LocalConditionalPiece.WIDE

    /** The tokens whose preceding hidden text is in [region]: a block's own first token included (a hidden `DECLARE`). */
    private fun ownTokens(region: RegionDraft, range: IntRange) = if (opensAtFirst(region.kind)) range else range.first + 1..range.last

    private fun isBalanced(index: Int) = Arrays.binarySearch(balanced, index) >= 0

    /** The index of the token starting at line, column, or null. */
    private fun tokenAt(line: Int, column: Int): Int? =
        if (lastToken < 0) null else search { compareValues(it.line, line).takeIf { c -> c != 0 } ?: compareValues(it.column, column) }

    /**
     * The positions where undecided conditional text, dropped or kept, writes each name, by lookup name. Empty when
     * token positions are not verified: every region is then incomplete anyway.
     */
    fun conditionalMentions(): Map<String, LongArray> {
        if (!ordered) return emptyMap()
        val positions = HashMap<String, MutableList<Long>>()
        for (piece in pointPieces.values + spanPieces) piece.mentionsByName().forEach { (name, at) -> positions.getOrPut(name) { ArrayList() }.addAll(at.asList()) }
        return positions.mapValues { it.value.toLongArray() }
    }

    /**
     * The hidden text of the file as source ranges: the gap before each token a point is recorded at, and each kept
     * branch. Empty when the token positions cannot be trusted.
     */
    fun hiddenText(fileId: FileId): List<LocalHiddenText> {
        if (!ordered) return emptyList()
        val result = ArrayList<LocalHiddenText>()
        for ((reason, indexes) in points) {
            for (b in indexes) {
                val after = tokens.getOrNull(b) ?: continue
                val before = tokens.getOrNull(b - 1)
                result += LocalHiddenText(reason, gap(fileId, before, after))
            }
        }
        for (span in spans) {
            val first = tokens.getOrNull(span.first) ?: continue
            val last = tokens.getOrNull(span.last) ?: continue
            result += LocalHiddenText(LocalIncompleteness.CONDITIONAL_COMPILATION, SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn))
        }
        return result.sortedWith(compareBy({ it.range.startLine }, { it.range.startColumn }))
    }

    private fun gap(fileId: FileId, before: Token?, after: Token): SourceRange =
        if (before == null) SourceRange(fileId, 1, 0, after.line, after.column)
        else SourceRange(fileId, before.endLine, before.endColumn, after.line, after.column)

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

    /** Whether the hidden text right before token [b] is inside a region of [kind] whose tokens are [range]. */
    private fun holds(range: IntRange, kind: LocalRegionKind, b: Int): Boolean = b in range && (b > range.first || opensAtFirst(kind))

    /** Hidden text right before the first token of a region of [kind] belongs to it: a block's hidden `DECLARE`. */
    private fun opensAtFirst(kind: LocalRegionKind) = kind == LocalRegionKind.ANONYMOUS_BLOCK || kind == LocalRegionKind.FILE

    private fun anyWithin(sorted: IntArray, range: IntRange): Boolean {
        if (range.isEmpty()) return false
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
         * and recovery nodes from the tree. [tokensAreWholeText] when the tokens and their trivia are all of the text,
         * as for a dynamic SQL text built from known parts only.
         */
        fun of(root: AstNode, source: String?, tokensAreWholeText: Boolean = false): LocalCompletenessEvidence {
            val recovery = ArrayList<AstNode>()
            val tokens = parserTokens(root, recovery)
            val global = EnumSet.noneOf(LocalIncompleteness::class.java)
            if (!isStrictlyOrdered(tokens)) global += LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS
            val points = HashMap<LocalIncompleteness, MutableList<Int>>()
            fun point(reason: LocalIncompleteness, before: Int) {
                points.getOrPut(reason) { ArrayList() } += before
            }

            // Conditional compilation, from the lexer's trivia types. A selection the configured settings decide is what
            // Oracle compiles: its trivia types are the DECIDED ones, not read here.
            val runs = ArrayList<Pair<Int, Boolean>>() // (token index, opens a level)
            val unbalanced = HashSet<Int>()
            val balancedPoints = HashSet<Int>()
            val pointWords = HashMap<Int, MutableList<ConditionalWord>>()
            for ((index, boundary) in boundaries(tokens)) {
                point(LocalIncompleteness.CONDITIONAL_COMPILATION, index)
                val text = boundary.originalValue
                runs += index to text.startsWith("\$IF", ignoreCase = true)
                val words = ConditionalText.words(text, boundary.line, boundary.column)
                pointWords.getOrPut(index) { ArrayList() } += words
                if (ConditionalText.isBlockBalanced(words)) balancedPoints += index else unbalanced += index
            }

            // Other discarded text, including before the first token and after the last one.
            var dollarFallback = false
            if (source == null) {
                if (!tokensAreWholeText) global += LocalIncompleteness.SOURCE_TEXT_UNAVAILABLE
            } else if (LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS !in global) {
                for (segment in DiscardedSourceText(source).fileSegments(tokens)) {
                    when (segment.kind) {
                        CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS -> global += LocalIncompleteness.UNVERIFIED_SOURCE_POSITIONS
                        CallableCoverageGap.SUBSTITUTION -> point(LocalIncompleteness.SUBSTITUTION, segment.before)
                        CallableCoverageGap.DISCARDED_SOURCE_TEXT -> point(LocalIncompleteness.DISCARDED_SOURCE_TEXT, segment.before)
                        CallableCoverageGap.CONDITIONAL_COMPILATION -> {
                            dollarFallback = true
                            point(LocalIncompleteness.CONDITIONAL_COMPILATION, segment.before)
                            unbalanced += segment.before
                        }
                        // Callable-heading gaps are package-extraction verdicts; segments never report them.
                        else -> Unit
                    }
                }
            }

            val (spans, delimited) = keptBranches(runs, dollarFallback, points[LocalIncompleteness.CONDITIONAL_COMPILATION].orEmpty())
            // The names each piece mentions, read once: a point's boundaries together, a kept branch's tokens.
            val pointPieces = pointWords.mapValues { (index, words) -> ConditionalText.piece(words, wide = index in unbalanced) }
            val spanPieces = if (!delimited) spans.map { LocalConditionalPiece.WIDE }
            else spans.map {
                val first = tokens[it.first]
                val last = tokens[it.last]
                val words = ConditionalText.words(tokens.subList(it.first, it.last + 1))
                ConditionalText.piece(words, kept = LocalConditionalPiece.Span(first.line, first.column, last.endLine, last.endColumn))
            }

            // Recovery: file-level nodes only; one below the file level taints everything.
            val recoveryStarts = recoveryStarts(root, recovery, tokens, global)

            return LocalCompletenessEvidence(
                tokens, points.mapValues { it.value.distinct().sorted().toIntArray() }, spans, recoveryStarts, global,
                (balancedPoints - unbalanced).sorted().toIntArray(), pointPieces, spanPieces
            )
        }

        /**
         * The sorted indexes of the tokens the `RECOVERY` nodes [recovery] start at; a node below the file level, or one
         * whose start is not found, adds [LocalIncompleteness.PARSE_RECOVERY] to [global].
         */
        private fun recoveryStarts(root: AstNode, recovery: List<AstNode>, tokens: List<Token>, global: MutableSet<LocalIncompleteness>): IntArray {
            if (recovery.isEmpty()) return IntArray(0)
            if (recovery.any { it.parent !== root }) global += LocalIncompleteness.PARSE_RECOVERY
            val index = IdentityHashMap<Token, Int>()
            tokens.forEachIndexed { i, token -> index[token] = i }
            val starts = recovery.mapNotNull { if (it.hasToken()) index[it.token] else null }
            // A recovery node whose start cannot be found would otherwise go unnoticed.
            if (starts.size != recovery.size) global += LocalIncompleteness.PARSE_RECOVERY
            return starts.sorted().toIntArray()
        }

        /** The undecided conditional-compilation boundaries, as (index of the token they precede, their trivia token). */
        private fun boundaries(tokens: List<Token>): List<Pair<Int, Token>> = tokens.withIndex().flatMap { (index, token) ->
            token.trivia.filter { it.token.type === PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY }.map { index to it.token }
        }

        /** Whether the tokens before `EOF` start at strictly increasing (line, column) positions. */
        private fun isStrictlyOrdered(tokens: List<Token>): Boolean {
            var previous: Token? = null
            for (token in tokens) {
                if (token.type === GenericTokenType.EOF) continue
                val last = previous
                if (last != null && (token.line < last.line || token.line == last.line && token.column <= last.column)) return false
                previous = token
            }
            return true
        }

        /**
         * The tokens of the tree in order, each once. `AstNode.tokens` repeats a token reached through two nodes; without
         * the repeats it equals the lexer output, except where the grammar consumes tokens without nodes (the Java source
         * of `CREATE JAVA`), which [DiscardedSourceText] then reports as discarded text.
         */
        private fun parserTokens(root: AstNode, recovery: MutableList<AstNode>): List<Token> {
            val tokens = ArrayList<Token>()
            val seen = Collections.newSetFromMap(IdentityHashMap<Token, Boolean>())
            // The root is not one of its own descendants: only the nodes below it are recovery candidates.
            if (!root.hasChildren()) root.tokenOrNull?.let { if (seen.add(it)) tokens += it }
            else {
                val children = root.children
                for (i in 0 until children.size) leafTokens(children[i], seen, tokens, recovery)
            }
            return tokens
        }

        /**
         * The tokens of the leaves under [node], in order, as `AstNode.tokens` lists them, each once; the same walk
         * collects the `RECOVERY` nodes in document order.
         */
        private fun leafTokens(node: AstNode, seen: MutableSet<Token>, tokens: MutableList<Token>, recovery: MutableList<AstNode>) {
            if (node.type === PlSqlGrammar.RECOVERY) recovery += node
            if (!node.hasChildren()) {
                val token = node.tokenOrNull
                if (token != null && seen.add(token)) tokens += token
            } else {
                val children = node.children
                for (i in 0 until children.size) leafTokens(children[i], seen, tokens, recovery)
            }
        }

        /**
         * The kept-branch spans: boundary runs pair in token order (a run starting with `$IF` opens a level, any other
         * closes one); each top-level construct spans from the token carrying its opening run to the token before the
         * one carrying its closing run. When the runs do not pair, or a `$` was left in a gap, one span from the first
         * to the last conditional point stands for the whole file. The second value says whether the spans are the
         * kept branches themselves (false for that one span).
         */
        private fun keptBranches(runs: List<Pair<Int, Boolean>>, dollarFallback: Boolean, conditionalPoints: List<Int>): Pair<List<IntRange>, Boolean> {
            val spans = pairedSpans(runs)
            if (spans != null && !dollarFallback) return spans to true
            if (conditionalPoints.isEmpty()) return emptyList<IntRange>() to false
            val first = conditionalPoints.min()
            val last = conditionalPoints.max()
            return (if (first <= last) listOf(first..last) else emptyList()) to false
        }

        /** The spans of the top-level constructs the boundary [runs] pair into; null when they do not pair. */
        private fun pairedSpans(runs: List<Pair<Int, Boolean>>): List<IntRange>? {
            val spans = ArrayList<IntRange>()
            var depth = 0
            var start = -1
            for ((index, opens) in runs) {
                if (opens) {
                    if (depth++ == 0) start = index
                } else {
                    if (--depth < 0) return null
                    if (depth == 0 && start <= index - 1) spans += start until index
                }
            }
            return if (depth == 0) spans else null
        }
    }
}
