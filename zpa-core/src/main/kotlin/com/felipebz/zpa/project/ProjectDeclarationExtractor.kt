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
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.extraction.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets

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
        private val packageBodies = PackageBodyExtractor(stream, bodyBoundaries, memberParser, typeReferences)
        private val packageSpecifications = PackageSpecificationExtractor(stream, bounds, packageValues, memberParser)
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

        private fun parseStandaloneCallable(create: Int, function: Boolean): ParsedUnit? = standaloneCallables.parseStandaloneCallable(create, function)

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

        private fun parsePackageSpecification(create: Int, nameIndex: Int): ParsedUnit? = packageSpecifications.parsePackageSpecification(create, nameIndex)

        private fun parsePackageBody(owner: QualifiedName, afterName: Int): ParsedUnit? = packageBodies.parsePackageBody(owner, afterName)


        private fun isKeyword(index: Int, keyword: String): Boolean = stream.isKeyword(index, keyword)

        private fun parseStandaloneType(create: Int, typeIndex: Int): ParsedUnit? = schemaUnits.parseStandaloneType(create, typeIndex)

        private fun parseTypeBody(typeIndex: Int): ParsedUnit = schemaUnits.parseTypeBody(typeIndex)

        private fun qualifiedName(start: Int): Pair<QualifiedName, Int>? = stream.qualifiedName(start)

        private fun semicolonAfter(start: Int): Int = stream.semicolonAfter(start)

        private fun skipToSemicolon(start: Int) = semicolonAfter(start) + 1

        private fun nextValue(start: Int, wanted: String): Int? = stream.nextValue(start, wanted)

        private fun valueAt(index: Int) = stream.valueAt(index)
    }
}
