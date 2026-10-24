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
import com.felipebz.zpa.project.*

/**
 * Per-extraction unit discovery and dispatch: finds candidate `CREATE` units by value, selects the extractor, and
 * advances through the token list. It owns the (eagerly built, thread-confined) component graph of one extraction.
 */
internal class UnitDispatcher(fileId: FileId, tokens: List<Token>, discardedText: DiscardedSourceText) {
    private val stream = TokenStream(fileId, tokens)
    private val bounds = UnitBounds(stream)
    private val datatypeShape = DatatypeShape(stream)
    private val typeReferences = TypeReferences(stream)
    private val collectionElements = CollectionElementSyntax(stream, datatypeShape, typeReferences)
    private val schemaUnits = SchemaUnitExtractor(stream, typeReferences, bounds, collectionElements)
    private val bodyBoundaries = BodyBoundaryScanner(stream)
    private val initializerShape = InitializerShape(stream)
    private val packageValues = PackageValueRecognizer(stream, datatypeShape, typeReferences, initializerShape)
    private val memberParser = PackageMemberParser(stream, typeReferences, collectionElements, datatypeShape)
    private val callableProperties = CallableProperties(stream)
    private val callSpecifications = CallSpecificationScanner(stream, datatypeShape)
    private val packageBodies = PackageBodyExtractor(stream, bodyBoundaries, memberParser, typeReferences)
    private val packageSpecifications = PackageSpecificationExtractor(stream, bounds, packageValues, memberParser, discardedText)
    private val standaloneCallables = StandaloneCallableExtractor(
        stream, bounds, datatypeShape, typeReferences, callableProperties, callSpecifications, bodyBoundaries
    )

    fun extract(): ProjectFileExtraction {
        val result = mutableListOf<ProjectDeclaration>()
        var cursor = 0
        while (cursor < stream.tokens.size) {
            val create = stream.nextValue(cursor, "CREATE") ?: break
            val parsed = parseCreate(create)
            if (parsed != null) {
                result += parsed.declarations
                cursor = parsed.nextIndex.coerceAtLeast(create + 1)
            } else {
                cursor = create + 1
            }
        }
        val altered = alteredTypeAttributes()
        return ProjectFileExtraction(immutableList(result), standaloneCallables.omittedNames, altered.keys, altered.filterValues { it.isNotEmpty() })
    }

    private fun alteredTypeAttributes(): Map<QualifiedName, Set<OracleIdentifier>> {
        val altered = HashMap<QualifiedName, MutableSet<OracleIdentifier>>()
        var cursor = 0
        while (cursor < stream.tokens.size) {
            val alter = stream.nextValue(cursor, "ALTER") ?: break
            cursor = alter + 1
            if (!stream.isKeyword(alter, "ALTER") || stream.valueAt(alter + 1) != "TYPE") continue
            val name = stream.qualifiedName(alter + 2) ?: continue
            val end = bounds.statementEnd(name.second)
            val attributes = (name.second..end).filter { stream.valueAt(it) == "ATTRIBUTE" }
            if (attributes.isEmpty()) continue
            val dropped = altered.getOrPut(name.first) { HashSet() }
            for (attribute in attributes) {
                if (stream.valueAt(attribute - 1) != "DROP") continue
                if (stream.valueAt(attribute + 1) == "(") {
                    val close = stream.matching(attribute + 1).coerceAtMost(end)
                    var index = attribute + 2
                    while (index < close) {
                        stream.identifierAt(index)?.let { dropped += it.first }
                        index = (index + 1 until close).firstOrNull { stream.valueAt(it) == "," }?.plus(1) ?: close
                    }
                } else {
                    stream.identifierAt(attribute + 1)?.let { dropped += it.first }
                }
            }
        }
        return altered
    }

    private fun parseCreate(create: Int): ParsedUnit? {
        var index = create + 1
        val orReplace = stream.valueAt(index) == "OR" && stream.valueAt(index + 1) == "REPLACE"
        if (orReplace) index += 2
        if (stream.valueAt(index) == "EDITIONABLE" || stream.valueAt(index) == "NONEDITIONABLE") index++
        return when (stream.valueAt(index)) {
            "PACKAGE" -> parsePackage(create, index, orReplace)
            // The strict standalone path re-validates the whole prefix with token types.
            "PROCEDURE" -> standaloneCallables.parseStandaloneCallable(create, function = false)
            "FUNCTION" -> standaloneCallables.parseStandaloneCallable(create, function = true)
            "SEQUENCE" -> schemaUnits.parseSequence(create, index)
            "TYPE" -> if (stream.valueAt(index + 1) == "BODY") schemaUnits.parseTypeBody(index) else schemaUnits.parseStandaloneType(create, index)
            else -> null
        }
    }

    private fun parsePackage(create: Int, packageIndex: Int, orReplace: Boolean): ParsedUnit? {
        // The shared dispatch is value based; require real CREATE/PACKAGE tokens (and a prefix that
        // validates with token types) so quoted lookalikes never start a package unit.
        if (strictPackagePrefix(create) != (packageIndex to orReplace)) return null
        // `BODY` must be the keyword: `CREATE PACKAGE "BODY" AS ...` is a specification named "BODY".
        val body = stream.isKeyword(packageIndex + 1, "BODY")
        val kindEnd = if (body) packageIndex + 2 else packageIndex + 1
        // IF NOT EXISTS follows PACKAGE [BODY] and cannot be combined with OR REPLACE (ORA-11541).
        val ifNotExists = if (body) {
            stream.valueAt(kindEnd) == "IF" && stream.valueAt(kindEnd + 1) == "NOT" && stream.valueAt(kindEnd + 2) == "EXISTS"
        } else {
            stream.isKeyword(kindEnd, "IF") && stream.isKeyword(kindEnd + 1, "NOT") && stream.isKeyword(kindEnd + 2, "EXISTS")
        }
        if (ifNotExists && orReplace) return null
        val nameIndex = if (ifNotExists) kindEnd + 3 else kindEnd
        if (!body) return packageSpecifications.parsePackageSpecification(create, nameIndex)
        val name = stream.qualifiedName(nameIndex) ?: return null
        return packageBodies.parsePackageBody(name.first, name.second)
    }

    /** `CREATE [OR REPLACE] [EDITIONABLE|NONEDITIONABLE] PACKAGE` validated with token types. */
    private fun strictPackagePrefix(create: Int): Pair<Int, Boolean>? {
        if (!stream.isKeyword(create, "CREATE")) return null
        var index = create + 1
        val orReplace = stream.isKeyword(index, "OR") && stream.isKeyword(index + 1, "REPLACE")
        if (orReplace) index += 2
        if (stream.isKeyword(index, "EDITIONABLE") || stream.isKeyword(index, "NONEDITIONABLE")) index++
        return if (stream.isKeyword(index, "PACKAGE")) index to orReplace else null
    }
}
