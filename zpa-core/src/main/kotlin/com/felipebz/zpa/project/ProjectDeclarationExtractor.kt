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
package com.felipebz.zpa.project

import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.extraction.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets

/** Sentinel returned by [packageHeaderEnd] for a wrapped unit. */
private const val WRAPPED_UNIT = -2

/**
 * Extracts the small, project-relevant declaration slice without constructing an AST.
 * This scanner deliberately understands declaration boundaries only; it does not parse
 * PL/SQL statements or expressions.
 */
fun interface ProjectDeclarationSourceExtractor {
    fun extract(fileId: FileId, source: String): List<ProjectDeclaration>
}

class ProjectDeclarationExtractor(
    configuration: PlSqlConfiguration = PlSqlConfiguration(StandardCharsets.UTF_8, true)
) : ProjectDeclarationSourceExtractor {
    /** FLR lexers keep mutable cursor state, so a concurrent preparation needs one per worker. */
    private val lexers = ThreadLocal.withInitial { PlSqlLexer.create(configuration) }

    override fun extract(fileId: FileId, source: String): List<ProjectDeclaration> {
        val tokens = lexers.get().lex(source)
        return TokenDeclarationScanner(fileId, tokens).extract()
    }

    private class TokenDeclarationScanner(
        private val fileId: FileId,
        private val tokens: List<Token>
    ) {
        private val stream = TokenStream(fileId, tokens)
        private val bounds = UnitBounds(stream)
        private val datatypeShape = DatatypeShape(stream)
        private val typeReferences = TypeReferences(stream)
        private val schemaUnits = SchemaUnitExtractor(stream, typeReferences)
        private val bodyBoundaries = BodyBoundaryScanner(stream)
        private val initializerShape = InitializerShape(stream)
        private val packageValues = PackageValueRecognizer(stream, datatypeShape, typeReferences, initializerShape)
        private val memberParser = LegacyMemberParser(stream, typeReferences)
        private val callableProperties = CallableProperties(stream)
        private val callSpecifications = CallSpecificationScanner(stream, datatypeShape)
        private val standaloneCallables = StandaloneCallableExtractor(
            stream, bounds, datatypeShape, typeReferences, callableProperties, callSpecifications, bodyBoundaries
        )

        fun extract(): List<ProjectDeclaration> {
            val result = mutableListOf<ProjectDeclaration>()
            var cursor = 0
            while (cursor < tokens.size) {
                val create = nextValue(cursor, "CREATE") ?: break
                val parsed = parseCreate(create)
                if (parsed != null) {
                    result += parsed.declarations
                    cursor = parsed.nextIndex.coerceAtLeast(create + 1)
                } else {
                    cursor = create + 1
                }
            }
            return immutableList(result)
        }

        private fun parseCreate(create: Int): ParsedUnit? {
            var index = create + 1
            val orReplace = valueAt(index) == "OR" && valueAt(index + 1) == "REPLACE"
            if (orReplace) index += 2
            if (valueAt(index) == "EDITIONABLE" || valueAt(index) == "NONEDITIONABLE") index++
            return when (valueAt(index)) {
                "PACKAGE" -> parsePackage(create, index, orReplace)
                // The strict standalone path re-validates the whole prefix with token types.
                "PROCEDURE" -> parseStandaloneCallable(create, function = false)
                "FUNCTION" -> parseStandaloneCallable(create, function = true)
                "SEQUENCE" -> parseSequence(create, index)
                "TYPE" -> if (valueAt(index + 1) == "BODY") parseTypeBody(index) else parseStandaloneType(create, index)
                else -> null
            }
        }

        private fun unitLimit(create: Int): Int = bounds.unitLimit(create)

        private fun isPunctuator(index: Int, punctuator: PlSqlPunctuator): Boolean = stream.isPunctuator(index, punctuator)

        private fun tokenTypeAt(index: Int) = stream.tokenTypeAt(index)

        private fun isNameBefore(index: Int, limit: Int) = stream.isNameBefore(index, limit)

        private fun parseStandaloneCallable(create: Int, function: Boolean): ParsedUnit? = standaloneCallables.parseStandaloneCallable(create, function)

        private fun strictQualifiedName(start: Int, limit: Int): Pair<QualifiedName, Int>? = stream.strictQualifiedName(start, limit)

        private fun isOpener(index: Int) = stream.isOpener(index)

        private fun isCloser(index: Int) = stream.isCloser(index)

        private fun closerFor(opener: Int) = stream.closerFor(opener)

        private fun parseSequence(create: Int, sequenceIndex: Int): ParsedUnit? = schemaUnits.parseSequence(create, sequenceIndex)

        private fun parsePackage(create: Int, packageIndex: Int, orReplace: Boolean): ParsedUnit? {
            // The shared dispatch is value based; require real CREATE/PACKAGE tokens (and a prefix that
            // validates with token types) so quoted lookalikes never start a package unit.
            if (strictPackagePrefix(create) != (packageIndex to orReplace)) return null
            // `BODY` must be the keyword: `CREATE PACKAGE "BODY" AS ...` is a specification named "BODY".
            val body = isKeyword(packageIndex + 1, "BODY")
            val kindEnd = if (body) packageIndex + 2 else packageIndex + 1
            // IF NOT EXISTS follows PACKAGE [BODY] and cannot be combined with OR REPLACE (ORA-11541).
            val ifNotExists = if (body) {
                valueAt(kindEnd) == "IF" && valueAt(kindEnd + 1) == "NOT" && valueAt(kindEnd + 2) == "EXISTS"
            } else {
                isKeyword(kindEnd, "IF") && isKeyword(kindEnd + 1, "NOT") && isKeyword(kindEnd + 2, "EXISTS")
            }
            if (ifNotExists && orReplace) return null
            val nameIndex = if (ifNotExists) kindEnd + 3 else kindEnd
            if (!body) return parsePackageSpecification(create, nameIndex)
            val name = qualifiedName(nameIndex) ?: return null
            return parsePackageBody(name.first, name.second)
        }

        /** `CREATE [OR REPLACE] [EDITIONABLE|NONEDITIONABLE] PACKAGE` validated with token types. */
        private fun strictPackagePrefix(create: Int): Pair<Int, Boolean>? {
            if (!isKeyword(create, "CREATE")) return null
            var index = create + 1
            val orReplace = isKeyword(index, "OR") && isKeyword(index + 1, "REPLACE")
            if (orReplace) index += 2
            if (isKeyword(index, "EDITIONABLE") || isKeyword(index, "NONEDITIONABLE")) index++
            return if (isKeyword(index, "PACKAGE")) index to orReplace else null
        }

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
        private fun parsePackageSpecification(create: Int, nameIndex: Int): ParsedUnit? {
            val limit = unitLimit(create)
            val name = strictQualifiedName(nameIndex, limit) ?: return null
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
                if (isKeyword(cursor, "END")) {
                    val semicolon = packageTerminatorSemicolon(cursor, limit) ?: return null
                    val declarations = mutableListOf<ProjectDeclaration>()
                    declarations += PackageDeclaration(owner, DeclarationRole.SPECIFICATION, fileId, range(create, semicolon))
                    declarations += members
                    return ParsedUnit(declarations, semicolon + 1)
                }
                val declarationEnd = specificationDeclarationEnd(cursor, limit) ?: return null
                if (declarationEnd.terminated) {
                    // Values are tried first, as the grammar's DECLARE_SECTION does (VARIABLE_DECLARATION precedes
                    // subprograms, subtypes, cursors and types): anything that is not a complete, strictly valid
                    // variable falls through to the member parsers.
                    (parseSpecificationValue(cursor, declarationEnd.index, owner)
                        ?: parseSpecificationMember(cursor, owner)
                            // Agreement rule: the member parser (legacy scanner) must end exactly where the
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
            for (index in start until limit.coerceAtMost(tokens.size)) {
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return -1
                    expected.isNotEmpty() -> Unit
                    isKeyword(index, "WRAPPED") -> return WRAPPED_UNIT
                    isKeyword(index, "IS") || isKeyword(index, "AS") -> return index
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) || isKeyword(index, "END") -> return -1
                }
            }
            return -1
        }

        /** Index of the opaque payload of a wrapped unit (`WRAPPED` immediately followed by `WRAPPED_SOURCE`). */
        private fun wrappedPayloadEnd(start: Int, limit: Int): Int? {
            val wrapped = (start until limit.coerceAtMost(tokens.size)).firstOrNull { isKeyword(it, "WRAPPED") } ?: return null
            return (wrapped + 1).takeIf { it < limit && tokenTypeAt(it) === PlSqlTokenType.WRAPPED_SOURCE }
        }

        /** `;` of `END [name] ;` for the `END` keyword at [end], strictly below [limit]. */
        private fun packageTerminatorSemicolon(end: Int, limit: Int): Int? {
            var index = end + 1
            if (index < limit && isName(tokens[index])) index++
            return index.takeIf { it < limit && isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
        }

        private class DeclarationEnd(val index: Int, val terminated: Boolean)

        /**
         * End of the declaration starting at [start]: its first real `;` at typed delimiter depth 0
         * (`terminated`), or, after a missing `;`, the reserved `PROCEDURE` that starts the next
         * subprogram (not terminated). Null when delimiters are crossed/unbalanced or no `;` exists
         * before [limit]. `FUNCTION` is deliberately not a recovery boundary: it is not reserved and
         * the parser accepts `g NUMBER := function(1);`.
         */
        private fun specificationDeclarationEnd(start: Int, limit: Int): DeclarationEnd? {
            val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
            for (index in start until limit.coerceAtMost(tokens.size)) {
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return null
                    expected.isNotEmpty() -> Unit
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return DeclarationEnd(index, true)
                    index > start && isKeyword(index, "PROCEDURE") -> return DeclarationEnd(index, false)
                }
            }
            return null
        }

        /**
         * Existing member parsers, selected by structural lookahead on token types. `type NUMBER;`,
         * `"TYPE" NUMBER;` and a variable named `function` do not match any member shape.
         */
        private fun parseSpecificationMember(cursor: Int, owner: QualifiedName): ParsedDeclaration? = when {
            isKeyword(cursor, "TYPE") && isNameBefore(cursor + 1, tokens.size) && isKeyword(cursor + 2, "IS") ->
                parsePackageType(cursor, owner)
            isKeyword(cursor, "SUBTYPE") && isNameBefore(cursor + 1, tokens.size) && isKeyword(cursor + 2, "IS") ->
                parsePackageSubtype(cursor, owner)
            isKeyword(cursor, "PROCEDURE") && isNameBefore(cursor + 1, tokens.size) -> parseSubprogram(cursor, owner)
            isKeyword(cursor, "FUNCTION") && isNameBefore(cursor + 1, tokens.size) &&
                (isPunctuator(cursor + 2, PlSqlPunctuator.LPARENTHESIS) || isKeyword(cursor + 2, "RETURN")) ->
                parseSubprogram(cursor, owner)
            else -> null
        }

        private fun parseSpecificationValue(start: Int, semicolon: Int, owner: QualifiedName): ParsedDeclaration? = packageValues.parseSpecificationValue(start, semicolon, owner)

        private fun parsePackageBody(owner: QualifiedName, afterName: Int): ParsedUnit? {
            val header = firstAtTopLevel(afterName, setOf("IS", "AS")) ?: return null
            val parsed = parsePackageBodySubprograms(header + 1, owner)
            return ParsedUnit(parsed.declarations, parsed.end + 1)
        }

        private fun parsePackageBodySubprograms(start: Int, owner: QualifiedName): ParsedPackageBody {
            val result = mutableListOf<ProjectDeclaration>()
            var cursor = start
            val blocks = mutableListOf<BlockKind>()
            while (cursor < tokens.size) {
                if (blocks.isEmpty() && isSubprogramKeyword(cursor)) {
                    val parsed = parseBodySubprogram(cursor, tokens.size, owner)
                    if (parsed != null) {
                        parsed.declaration?.let(result::add)
                        cursor = parsed.nextIndex
                        continue
                    }
                }

                when {
                    isCaseStart(cursor) -> blocks += BlockKind.CASE
                    isKeyword(cursor, "BEGIN") -> {
                        if (blocks.isEmpty()) {
                            val end = executableBlockEnd(cursor, tokens.size) ?: tokens.lastIndex
                            return ParsedPackageBody(immutableList(result), end)
                        }
                        blocks += BlockKind.BEGIN
                    }
                    isKeyword(cursor, "END") -> when {
                        isKeyword(cursor + 1, "IF") || isKeyword(cursor + 1, "LOOP") -> Unit
                        blocks.lastOrNull() == BlockKind.CASE -> blocks.removeAt(blocks.lastIndex)
                        blocks.lastOrNull() == BlockKind.BEGIN -> blocks.removeAt(blocks.lastIndex)
                        blocks.isEmpty() -> return ParsedPackageBody(immutableList(result), semicolonAfter(cursor))
                    }
                }
                cursor++
            }
            return ParsedPackageBody(immutableList(result), tokens.lastIndex)
        }

        private fun parseBodySubprogram(index: Int, end: Int, owner: QualifiedName): ParsedBodySubprogram? {
            val function = valueAt(index) == "FUNCTION"
            val name = identifierAt(index + 1) ?: return null
            var cursor = name.second
            val parameters: List<ProjectParameter>
            if (valueAt(cursor) == "(") {
                val close = matching(cursor)
                if (close >= end) return null
                parameters = parseParameters(cursor + 1, close)
                cursor = close + 1
            } else {
                parameters = emptyList()
            }

            val header = firstBodyHeaderDelimiter(cursor, end) ?: return null
            if (header.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
                return ParsedBodySubprogram(null, header.index + 1)
            }

            val implementationEnd = implementationEnd(header.index + 1, end) ?: return null
            val declaration = if (function) {
                val returnIndex = firstAtTopLevel(cursor, setOf("RETURN"), header.index)
                    ?: return null
                val modifier = firstAtTopLevel(
                    returnIndex + 1,
                    FUNCTION_RETURN_TYPE_TERMINATORS,
                    header.index
                )
                val returnType = typeReference(returnIndex + 1, modifier ?: header.index)
                    ?: return null
                PackageFunctionDeclaration(
                    owner,
                    name.first,
                    parameters,
                    returnType,
                    fileId,
                    range(index, implementationEnd),
                    DeclarationRole.BODY
                )
            } else {
                PackageProcedureDeclaration(
                    owner,
                    name.first,
                    parameters,
                    fileId,
                    range(index, implementationEnd),
                    DeclarationRole.BODY
                )
            }
            return ParsedBodySubprogram(declaration, implementationEnd + 1)
        }

        private fun firstBodyHeaderDelimiter(start: Int, end: Int): BodyHeaderDelimiter? = bodyBoundaries.firstBodyHeaderDelimiter(start, end)

        private fun implementationEnd(start: Int, end: Int): Int? = bodyBoundaries.implementationEnd(start, end)


        private fun executableBlockEnd(start: Int, end: Int): Int? = bodyBoundaries.executableBlockEnd(start, end)

        private fun isSubprogramKeyword(index: Int): Boolean = bodyBoundaries.isSubprogramKeyword(index)

        private fun isCaseStart(index: Int): Boolean = bodyBoundaries.isCaseStart(index)

        private fun isKeyword(index: Int, keyword: String): Boolean = stream.isKeyword(index, keyword)

        private fun parsePackageType(index: Int, owner: QualifiedName): ParsedDeclaration? = memberParser.parsePackageType(index, owner)

        private fun parsePackageSubtype(index: Int, owner: QualifiedName): ParsedDeclaration? = memberParser.parsePackageSubtype(index, owner)

        private fun parseSubprogram(index: Int, owner: QualifiedName): ParsedDeclaration? = memberParser.parseSubprogram(index, owner)

        private fun parseParameters(start: Int, close: Int): List<ProjectParameter> = memberParser.parseParameters(start, close)

        private fun parseStandaloneType(create: Int, typeIndex: Int): ParsedUnit? = schemaUnits.parseStandaloneType(create, typeIndex)

        private fun parseTypeBody(typeIndex: Int): ParsedUnit = schemaUnits.parseTypeBody(typeIndex)

        private fun typeReference(start: Int, endExclusive: Int): TypeRef? = typeReferences.typeReference(start, endExclusive)

        private fun qualifiedName(start: Int): Pair<QualifiedName, Int>? = stream.qualifiedName(start)

        private fun identifierAt(start: Int): Pair<OracleIdentifier, Int>? = stream.identifierAt(start)

        private fun firstAtTopLevel(start: Int, wanted: Set<String>, endExclusive: Int = tokens.size): Int? = stream.firstAtTopLevel(start, wanted, endExclusive)

        private fun matching(open: Int): Int = stream.matching(open)

        private fun semicolonAfter(start: Int): Int = stream.semicolonAfter(start)

        private fun skipToSemicolon(start: Int) = semicolonAfter(start) + 1

        private fun nextValue(start: Int, wanted: String): Int? = stream.nextValue(start, wanted)

        private fun range(start: Int, end: Int): SourceRange = stream.range(start, end)

        private fun isName(token: Token): Boolean = stream.isName(token)

        private fun valueAt(index: Int) = stream.valueAt(index)
        private data class ParsedBodySubprogram(
            val declaration: ProjectDeclaration?,
            val nextIndex: Int
        )
        private data class ParsedPackageBody(
            val declarations: List<ProjectDeclaration>,
            val end: Int
        )
    }
}
