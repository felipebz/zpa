//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.*

internal class MemberNameRecorder(private val stream: TokenStream) {
    /**
     * The leading name of a declaration `[start, end)`, [end] being its `;` when [terminated]. A name is recorded only
     * for a recognized declaration shape; a `PRAGMA` declares no name; anything else is a gap, never an invented name.
     */
    fun record(
        start: Int,
        end: Int,
        terminated: Boolean,
        names: MutableList<PackageMemberName>,
        gaps: MutableSet<MemberNameGap>,
        recordName: Boolean = true
    ) {
        if (!terminated) gaps += MemberNameGap.UNTERMINATED_DECLARATION
        if (isPragma(start, end)) return
        val head = memberHead(start, end)
        if (head == null) {
            gaps += MemberNameGap.UNIDENTIFIED_DECLARATION
            return
        }
        val (kind, nameIndex) = head
        val last = if (terminated) end else end - 1
        if (recordName) names += PackageMemberName(stream.identifier(stream.tokens[nameIndex]), kind, stream.range(start, last))
        if (mayHideAnotherDeclaration(kind, nameIndex + 1, end)) gaps += MemberNameGap.SWALLOWED_DECLARATION
    }

    fun discardedGap(gap: CallableCoverageGap): MemberNameGap = when (gap) {
        CallableCoverageGap.CONDITIONAL_COMPILATION -> MemberNameGap.CONDITIONAL_COMPILATION
        CallableCoverageGap.SUBSTITUTION -> MemberNameGap.SUBSTITUTION
        CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS -> MemberNameGap.UNVERIFIED_SOURCE_POSITIONS
        else -> MemberNameGap.DISCARDED_SOURCE_TEXT
    }

    /** Kind and name index of a recognized declaration shape starting at [start], or null. */
    private fun memberHead(start: Int, end: Int): Pair<PackageMemberKind, Int>? {
        fun nameAt(index: Int) = index < end && stream.isName(stream.tokens[index]) && !stream.isPunctuator(index + 1, PlSqlPunctuator.DOT)
        return when {
            stream.isKeyword(start, "CURSOR") -> (start + 1).takeIf {
                nameAt(it) && (stream.isPunctuator(it + 1, PlSqlPunctuator.LPARENTHESIS) ||
                    stream.isKeyword(it + 1, "RETURN") || stream.isKeyword(it + 1, "IS"))
            }?.let { PackageMemberKind.CURSOR to it }
            stream.isKeyword(start, "TYPE") -> (start + 1).takeIf {
                nameAt(it) && (stream.isKeyword(it + 1, "IS") || stream.isKeyword(it + 1, "AS"))
            }?.let { PackageMemberKind.TYPE to it }
            stream.isKeyword(start, "SUBTYPE") -> (start + 1).takeIf { nameAt(it) && stream.isKeyword(it + 1, "IS") }
                ?.let { PackageMemberKind.SUBTYPE to it }
            stream.isKeyword(start, "PROCEDURE") -> (start + 1).takeIf { nameAt(it) }?.let { PackageMemberKind.PROCEDURE to it }
            stream.isKeyword(start, "FUNCTION") -> (start + 1).takeIf { isFunctionHeader(start, end) && nameAt(it) }
                ?.let { PackageMemberKind.FUNCTION to it }
            isPragma(start, end) -> null
            // A value or exception: the declaration starts with its own name, never with a declaration keyword.
            start + 1 < end && stream.isName(stream.tokens[start]) && !stream.isPunctuator(start + 1, PlSqlPunctuator.DOT) ->
                if (stream.isKeyword(start + 1, "EXCEPTION") && start + 2 == end) PackageMemberKind.EXCEPTION to start
                else PackageMemberKind.VALUE to start
            else -> null
        }
    }

    /**
     * A depth-0 declaration keyword after the name of a short declaration (value or exception) that produced no fact,
     * which suggests a missing `;` merged two declarations. Callables are covered by the callable coverage; types and
     * cursors legitimately contain `CURSOR`/`TYPE` tokens (`REF CURSOR`, `%TYPE`, queries) and are not scanned.
     */
    private fun mayHideAnotherDeclaration(kind: PackageMemberKind, from: Int, end: Int): Boolean {
        if (kind != PackageMemberKind.VALUE && kind != PackageMemberKind.EXCEPTION) return hasTopLevelCallableKeyword(from, end)
        var depth = 0
        for (index in from until end) {
            when {
                stream.isOpener(index) -> depth++
                stream.isCloser(index) -> depth--
                depth > 0 -> Unit
                stream.isPunctuator(index - 1, PlSqlPunctuator.MOD) || stream.isPunctuator(index - 1, PlSqlPunctuator.DOT) -> Unit
                stream.isKeyword(index, "PROCEDURE") || stream.isKeyword(index, "FUNCTION") ||
                    stream.isKeyword(index, "CURSOR") || stream.isKeyword(index, "TYPE") || stream.isKeyword(index, "SUBTYPE") ||
                    (stream.isKeyword(index, "EXCEPTION") && kind == PackageMemberKind.VALUE) -> return true
            }
        }
        return false
    }

    /** `PRAGMA name ;` or `PRAGMA name ( ... ) ;`; a variable named `pragma` has another shape. */
    private fun isPragma(start: Int, end: Int): Boolean =
        stream.isKeyword(start, "PRAGMA") && start + 1 < end && stream.isName(stream.tokens[start + 1]) &&
            (start + 2 == end || stream.isPunctuator(start + 2, PlSqlPunctuator.LPARENTHESIS))

    /** `FUNCTION name (` or `FUNCTION name RETURN`: the shape the member parser selects for a function. */
    fun isFunctionHeader(index: Int, end: Int): Boolean =
        index + 2 < end && stream.isName(stream.tokens[index + 1]) &&
            (stream.isPunctuator(index + 2, PlSqlPunctuator.LPARENTHESIS) || stream.isKeyword(index + 2, "RETURN"))

    /** A depth-0 `PROCEDURE`/`FUNCTION` keyword in `[from, end)`, inside a declaration that produced no fact. */
    fun hasTopLevelCallableKeyword(from: Int, end: Int): Boolean {
        var depth = 0
        for (index in from until end) {
            when {
                stream.isOpener(index) -> depth++
                stream.isCloser(index) -> depth--
                depth == 0 && (stream.isKeyword(index, "PROCEDURE") || stream.isKeyword(index, "FUNCTION")) -> return true
            }
        }
        return false
    }
}
