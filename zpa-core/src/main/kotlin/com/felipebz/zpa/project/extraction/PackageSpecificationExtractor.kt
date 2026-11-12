//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.*

/** Sentinel returned by [PackageSpecificationExtractor.packageHeaderEnd] for a wrapped unit. */
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
    private val nameRecorder = MemberNameRecorder(stream)

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
                discarded.kinds.forEach { nameGaps += nameRecorder.discardedGap(it) }
                val conditionalRange = discarded.conditionalTokens?.let { stream.range(it.first, it.last) }
                val coverage = PackageCallableCoverage(omitted, gaps)
                val nameCoverage = PackageMemberNameCoverage(names, nameGaps, conditionalRange, discarded.conditionalMentions)
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
                    ?: parseSpecificationMember(cursor, declarationEnd.index, owner)
                        // Agreement rule: the member parser (value-based) must end exactly where the
                        // strict scanner ends the declaration; any disagreement drops the declaration.
                        ?.takeIf { it.nextIndex == declarationEnd.index + 1 })
                    ?.also { members += it.declaration }
                val fact = retained?.declaration
                if (fact is PackageExceptionDeclaration || fact is PackageCursorDeclaration) {
                    // Formerly name-only members: their declaration keeps exactly the coverage checks it had as an
                    // unretained name (a cursor query may still absorb a following declaration), without the name.
                    recordCoverage(cursor, declarationEnd.index, false, omitted, gaps)
                    nameRecorder.record(cursor, declarationEnd.index, true, names, nameGaps, recordName = false)
                } else {
                    recordCoverage(cursor, declarationEnd.index, retained != null, omitted, gaps)
                    if (retained == null) {
                        nameRecorder.record(cursor, declarationEnd.index, true, names, nameGaps)
                    } else if (containsCallableHeader(cursor + 1, declarationEnd.index)) {
                        nameGaps += MemberNameGap.SWALLOWED_DECLARATION
                    }
                }
                cursor = declarationEnd.index + 1
            } else {
                // Stopped at a reserved PROCEDURE after a missing `;`: the broken declaration yields no
                // fact and the following subprogram is still visited.
                recordCoverage(cursor, declarationEnd.index, false, omitted, gaps)
                nameRecorder.record(cursor, declarationEnd.index, false, names, nameGaps)
                cursor = declarationEnd.index
            }
        }
    }

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
                if (nameRecorder.isFunctionHeader(start, end)) {
                    omitted += stream.identifier(stream.tokens[start + 1]).lookupName
                } else {
                    gaps += CallableCoverageGap.AMBIGUOUS_FUNCTION_DECLARATION
                }
            nameRecorder.hasTopLevelCallableKeyword(start + 1, end) -> gaps += CallableCoverageGap.SWALLOWED_CALLABLE_HEADER
        }
        // A dropped callable heading missing its `;` can absorb the next heading, which then has no fact of its own.
        if ((stream.isKeyword(start, "PROCEDURE") || stream.isKeyword(start, "FUNCTION")) &&
            nameRecorder.hasTopLevelCallableKeyword(start + 2, end)
        ) gaps += CallableCoverageGap.SWALLOWED_CALLABLE_HEADER
    }

    /** A complete function header shape at depth 0 in `[from, end)`, which no retained declaration can contain legitimately. */
    private fun containsCallableHeader(from: Int, end: Int): Boolean {
        var depth = 0
        for (index in from until end) {
            when {
                stream.isOpener(index) -> depth++
                stream.isCloser(index) -> depth--
                depth == 0 && stream.isKeyword(index, "FUNCTION") && nameRecorder.isFunctionHeader(index, end) -> return true
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
     * `"TYPE" NUMBER;` and a variable named `function` do not match any member shape. Cursors and exceptions are
     * parsed against the strict `;` at [semicolon].
     */
    private fun parseSpecificationMember(cursor: Int, semicolon: Int, owner: QualifiedName): ParsedDeclaration? = when {
        stream.isKeyword(cursor, "CURSOR") -> memberParser.parseCursor(cursor, semicolon, owner)
        stream.isKeyword(cursor + 1, "EXCEPTION") -> memberParser.parseException(cursor, semicolon, owner)
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
