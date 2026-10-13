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

import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.*

/** Sentinel returned by [packageHeaderEnd] for a wrapped unit. */
private const val WRAPPED_UNIT = -2

/**
 * Strict walking of `CREATE PACKAGE` specifications: header, declaration-by-declaration boundaries, value-before-member
 * dispatch (the member parsers must agree with the strict boundary) and the package terminator.
 */
internal class PackageSpecificationExtractor(
    private val stream: TokenStream,
    private val bounds: UnitBounds,
    private val packageValues: PackageValueRecognizer,
    private val memberParser: PackageMemberParser,
    private val discardedText: DiscardedSourceText
) {
    /**
     * Package specification, bounded by the unit limit and validated with token types.
     *
     * The end of the specification is found by *walking declarations*, not by searching for an
     * `END`: every declaration ends at its first real `;`, and a real `END` keyword (reserved, so
     * never an identifier) is the terminator only where a declaration would start. A `CASE ... END`
     * inside an initializer, a quoted `"END"` or a variable named like a keyword therefore cannot
     * end the package. A specification that cannot be walked to its own `END [name] ;` before the
     * unit limit yields no fact at all (never a range to EOF, never the next unit's members).
     */
    fun parsePackageSpecification(create: Int, nameIndex: Int): ParsedUnit? {
        val limit = bounds.unitLimit(create)
        val name = stream.strictQualifiedName(nameIndex, limit) ?: return null
        val owner = name.first
        val header = packageHeaderEnd(name.second, limit)
        if (header == WRAPPED_UNIT) {
            // One opaque WRAPPED_SOURCE token: nothing can be recovered, and nothing after it is consumed.
            val wrapped = wrappedPayloadEnd(name.second, limit) ?: return null
            return ParsedUnit(emptyList(), wrapped + 1)
        }
        if (header < 0) return null

        val members = mutableListOf<ProjectDeclaration>()
        val omitted = mutableSetOf<String>()
        val gaps = mutableSetOf<CallableCoverageGap>()
        val names = mutableListOf<PackageMemberName>()
        val nameGaps = mutableSetOf<MemberNameGap>()
        var cursor = header + 1
        while (true) {
            if (cursor >= limit) return null
            if (stream.isKeyword(cursor, "END")) {
                val semicolon = packageTerminatorSemicolon(cursor, limit) ?: return null
                discardedText.firstGap(stream.tokens, header, cursor)?.let { gaps += it }
                val discarded = discardedText.scan(stream.tokens, header, cursor)
                discarded.kinds.forEach { nameGaps += discardedNameGap(it) }
                val conditionalRange = discarded.conditionalTokens?.let { stream.range(it.first, it.last) }
                val coverage = PackageCallableCoverage(omitted, gaps)
                val nameCoverage = PackageMemberNameCoverage(names, nameGaps, conditionalRange)
                val declarations = mutableListOf<ProjectDeclaration>()
                declarations += PackageDeclaration(owner, DeclarationRole.SPECIFICATION, stream.fileId, stream.range(create, semicolon), coverage, nameCoverage)
                declarations += members
                return ParsedUnit(declarations, semicolon + 1)
            }
            val declarationEnd = specificationDeclarationEnd(cursor, limit) ?: return null
            if (declarationEnd.terminated) {
                // Values are tried first, as the grammar's DECLARE_SECTION does (VARIABLE_DECLARATION precedes
                // subprograms, subtypes, cursors and types): anything that is not a complete, strictly valid
                // variable falls through to the member parsers.
                val retained = (packageValues.parseSpecificationValue(cursor, declarationEnd.index, owner)
                    ?: parseSpecificationMember(cursor, owner)
                        // Agreement rule: the member parser (value-based) must end exactly where the
                        // strict scanner ends the declaration; any disagreement drops the declaration.
                        ?.takeIf { it.nextIndex == declarationEnd.index + 1 })
                    ?.also { members += it.declaration }
                recordCoverage(cursor, declarationEnd.index, retained != null, omitted, gaps)
                if (retained == null) {
                    recordMemberName(cursor, declarationEnd.index, true, names, nameGaps)
                } else if (containsCallableHeader(cursor + 1, declarationEnd.index)) {
                    nameGaps += MemberNameGap.SWALLOWED_DECLARATION
                }
                cursor = declarationEnd.index + 1
            } else {
                // Stopped at a reserved PROCEDURE after a missing `;`: the broken declaration yields no
                // fact and the following subprogram is still visited.
                recordCoverage(cursor, declarationEnd.index, false, omitted, gaps)
                recordMemberName(cursor, declarationEnd.index, false, names, nameGaps)
                cursor = declarationEnd.index
            }
        }
    }

    private fun discardedNameGap(gap: CallableCoverageGap): MemberNameGap = when (gap) {
        CallableCoverageGap.CONDITIONAL_COMPILATION -> MemberNameGap.CONDITIONAL_COMPILATION
        CallableCoverageGap.SUBSTITUTION -> MemberNameGap.SUBSTITUTION
        CallableCoverageGap.UNVERIFIED_SOURCE_POSITIONS -> MemberNameGap.UNVERIFIED_SOURCE_POSITIONS
        else -> MemberNameGap.DISCARDED_SOURCE_TEXT
    }

    /**
     * The leading name of a declaration `[start, end)` that produced no fact. A name is recorded only for a recognized
     * declaration shape; a `PRAGMA` declares no name; anything else is a gap, never an invented name.
     */
    private fun recordMemberName(
        start: Int,
        end: Int,
        terminated: Boolean,
        names: MutableList<PackageMemberName>,
        gaps: MutableSet<MemberNameGap>
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
        names += PackageMemberName(stream.identifier(stream.tokens[nameIndex]), kind, stream.range(start, last))
        if (mayHideAnotherDeclaration(kind, nameIndex + 1, end)) gaps += MemberNameGap.SWALLOWED_DECLARATION
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
     * which suggests a missing `;` merged two declarations. Callables are covered by [recordCoverage]; types and
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

    /**
     * Callable coverage of the declaration `[start, end)`. A dropped `PROCEDURE`/`FUNCTION` header contributes its
     * Oracle-normalized name when it is identified safely; anything that may hide a callable without a safe name makes
     * the coverage unknown. A retained declaration only matters if a whole callable header may sit inside it.
     */
    private fun recordCoverage(start: Int, end: Int, retained: Boolean, omitted: MutableSet<String>, gaps: MutableSet<CallableCoverageGap>) {
        if (retained) {
            if (containsCallableHeader(start + 1, end)) gaps += CallableCoverageGap.SWALLOWED_CALLABLE_HEADER
            return
        }
        when {
            stream.isKeyword(start, "PROCEDURE") ->
                if (start + 1 < end && stream.isName(stream.tokens[start + 1]) && !stream.isPunctuator(start + 2, PlSqlPunctuator.DOT)) {
                    omitted += stream.identifier(stream.tokens[start + 1]).lookupName
                } else {
                    gaps += CallableCoverageGap.UNIDENTIFIED_CALLABLE_HEADER
                }
            stream.isKeyword(start, "FUNCTION") ->
                if (isFunctionHeader(start, end)) {
                    omitted += stream.identifier(stream.tokens[start + 1]).lookupName
                } else {
                    gaps += CallableCoverageGap.AMBIGUOUS_FUNCTION_DECLARATION
                }
            hasTopLevelCallableKeyword(start + 1, end) -> gaps += CallableCoverageGap.SWALLOWED_CALLABLE_HEADER
        }
        // A dropped callable heading missing its `;` can absorb the next heading, which then has no fact of its own.
        if ((stream.isKeyword(start, "PROCEDURE") || stream.isKeyword(start, "FUNCTION")) &&
            hasTopLevelCallableKeyword(start + 2, end)
        ) gaps += CallableCoverageGap.SWALLOWED_CALLABLE_HEADER
    }

    /** `FUNCTION name (` or `FUNCTION name RETURN`: the shape the member parser selects for a function. */
    private fun isFunctionHeader(index: Int, end: Int): Boolean =
        index + 2 < end && stream.isName(stream.tokens[index + 1]) &&
            (stream.isPunctuator(index + 2, PlSqlPunctuator.LPARENTHESIS) || stream.isKeyword(index + 2, "RETURN"))

    /** A depth-0 `PROCEDURE`/`FUNCTION` keyword in `[from, end)`, inside a declaration that produced no fact. */
    private fun hasTopLevelCallableKeyword(from: Int, end: Int): Boolean {
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

    /** A complete function header shape at depth 0 in `[from, end)`, which no retained declaration can contain legitimately. */
    private fun containsCallableHeader(from: Int, end: Int): Boolean {
        var depth = 0
        for (index in from until end) {
            when {
                stream.isOpener(index) -> depth++
                stream.isCloser(index) -> depth--
                depth == 0 && stream.isKeyword(index, "FUNCTION") && isFunctionHeader(index, end) -> return true
            }
        }
        return false
    }

    /**
     * Index of the `IS`/`AS` keyword ending the header, [WRAPPED_UNIT] for a wrapped unit, or -1.
     * Typed delimiters only, bounded by [limit]; a `;` or `END` first means there is no header.
     */
    private fun packageHeaderEnd(start: Int, limit: Int): Int {
        val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
        for (index in start until limit.coerceAtMost(stream.tokens.size)) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return -1
                expected.isNotEmpty() -> Unit
                stream.isKeyword(index, "WRAPPED") -> return WRAPPED_UNIT
                stream.isKeyword(index, "IS") || stream.isKeyword(index, "AS") -> return index
                stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) || stream.isKeyword(index, "END") -> return -1
            }
        }
        return -1
    }

    /** Index of the opaque payload of a wrapped unit (`WRAPPED` immediately followed by `WRAPPED_SOURCE`). */
    private fun wrappedPayloadEnd(start: Int, limit: Int): Int? {
        val wrapped = (start until limit.coerceAtMost(stream.tokens.size)).firstOrNull { stream.isKeyword(it, "WRAPPED") } ?: return null
        return (wrapped + 1).takeIf { it < limit && stream.tokenTypeAt(it) === PlSqlTokenType.WRAPPED_SOURCE }
    }

    /** `;` of `END [name] ;` for the `END` keyword at [end], strictly below [limit]. */
    private fun packageTerminatorSemicolon(end: Int, limit: Int): Int? {
        var index = end + 1
        if (index < limit && stream.isName(stream.tokens[index])) index++
        return index.takeIf { it < limit && stream.isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
    }

    /**
     * End of the declaration starting at [start]: its first real `;` at typed delimiter depth 0
     * (`terminated`), or, after a missing `;`, the reserved `PROCEDURE` that starts the next
     * subprogram (not terminated). Null when delimiters are crossed/unbalanced or no `;` exists
     * before [limit]. `FUNCTION` is deliberately not a recovery boundary: it is not reserved and
     * the parser accepts `g NUMBER := function(1);`.
     */
    private fun specificationDeclarationEnd(start: Int, limit: Int): DeclarationEnd? {
        val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
        for (index in start until limit.coerceAtMost(stream.tokens.size)) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return null
                expected.isNotEmpty() -> Unit
                stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return DeclarationEnd(index, true)
                index > start && stream.isKeyword(index, "PROCEDURE") -> return DeclarationEnd(index, false)
            }
        }
        return null
    }

    /**
     * Existing member parsers, selected by structural lookahead on token types. `type NUMBER;`,
     * `"TYPE" NUMBER;` and a variable named `function` do not match any member shape.
     */
    private fun parseSpecificationMember(cursor: Int, owner: QualifiedName): ParsedDeclaration? = when {
        stream.isKeyword(cursor, "TYPE") && stream.isNameBefore(cursor + 1, stream.tokens.size) && stream.isKeyword(cursor + 2, "IS") ->
            memberParser.parsePackageType(cursor, owner)
        stream.isKeyword(cursor, "SUBTYPE") && stream.isNameBefore(cursor + 1, stream.tokens.size) && stream.isKeyword(cursor + 2, "IS") ->
            memberParser.parsePackageSubtype(cursor, owner)
        stream.isKeyword(cursor, "PROCEDURE") && stream.isNameBefore(cursor + 1, stream.tokens.size) -> memberParser.parseSubprogram(cursor, owner)
        stream.isKeyword(cursor, "FUNCTION") && stream.isNameBefore(cursor + 1, stream.tokens.size) &&
            (stream.isPunctuator(cursor + 2, PlSqlPunctuator.LPARENTHESIS) || stream.isKeyword(cursor + 2, "RETURN")) ->
            memberParser.parseSubprogram(cursor, owner)
        else -> null
    }

    private class DeclarationEnd(val index: Int, val terminated: Boolean)
}
