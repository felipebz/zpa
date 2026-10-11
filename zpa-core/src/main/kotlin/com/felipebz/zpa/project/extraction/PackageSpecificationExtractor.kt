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
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.PackageDeclaration
import com.felipebz.zpa.project.ProjectDeclaration
import com.felipebz.zpa.project.QualifiedName

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
    private val memberParser: PackageMemberParser
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
        var cursor = header + 1
        while (true) {
            if (cursor >= limit) return null
            if (stream.isKeyword(cursor, "END")) {
                val semicolon = packageTerminatorSemicolon(cursor, limit) ?: return null
                val declarations = mutableListOf<ProjectDeclaration>()
                declarations += PackageDeclaration(owner, DeclarationRole.SPECIFICATION, stream.fileId, stream.range(create, semicolon))
                declarations += members
                return ParsedUnit(declarations, semicolon + 1)
            }
            val declarationEnd = specificationDeclarationEnd(cursor, limit) ?: return null
            if (declarationEnd.terminated) {
                // Values are tried first, as the grammar's DECLARE_SECTION does (VARIABLE_DECLARATION precedes
                // subprograms, subtypes, cursors and types): anything that is not a complete, strictly valid
                // variable falls through to the member parsers.
                (packageValues.parseSpecificationValue(cursor, declarationEnd.index, owner)
                    ?: parseSpecificationMember(cursor, owner)
                        // Agreement rule: the member parser (value-based) must end exactly where the
                        // strict scanner ends the declaration; any disagreement drops the declaration.
                        ?.takeIf { it.nextIndex == declarationEnd.index + 1 })
                    ?.let { members += it.declaration }
                cursor = declarationEnd.index + 1
            } else {
                // Stopped at a reserved PROCEDURE after a missing `;`: the broken declaration yields no
                // fact and the following subprogram is still visited.
                cursor = declarationEnd.index
            }
        }
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
