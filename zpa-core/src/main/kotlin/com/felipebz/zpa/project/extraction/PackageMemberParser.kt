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
import com.felipebz.zpa.project.*

internal val FUNCTION_RETURN_TYPE_TERMINATORS = setOf(
    "DETERMINISTIC",
    "PIPELINED",
    "PARALLEL_ENABLE",
    "RESULT_CACHE",
    "SQL_MACRO",
    "SHARD_ENABLE",
    "ACCESSIBLE",
    "AUTHID",
    "DEFAULT"
)

/**
 * Value-based, permissive parsers for package types, subtypes, subprogram headings and their parameters. The same
 * instance serves package specifications and package bodies.
 */
internal class PackageMemberParser(
    private val stream: TokenStream,
    private val typeReferences: TypeReferences,
    private val collectionElements: CollectionElementSyntax,
    private val datatypeShape: DatatypeShape
) {
    fun parsePackageType(index: Int, owner: QualifiedName): ParsedDeclaration? {
        val name = stream.identifierAt(index + 1) ?: return null
        val end = stream.semicolonAfter(index + 1)
        val collection = if (stream.isKeyword(index + 2, "IS")) collectionElements.parse(index + 2, end) else null
        val shape = typeReferences.typeShape(index, end) ?: ProjectTypeShape.COLLECTION.takeIf { collection != null }
        val recordFields = if (shape == ProjectTypeShape.RECORD) {
            parseRecordFields(name.second, end)
        } else {
            emptyList()
        }
        val declaration = PackageTypeDeclaration(
            owner, name.first, shape, recordFields, stream.fileId, stream.range(index, end),
            elementType = collection?.elementType?.takeIf { shape == ProjectTypeShape.COLLECTION }
        )
        return ParsedDeclaration(declaration, end + 1)
    }

    private fun parseRecordFields(start: Int, end: Int): List<ProjectRecordField> {
        val record = stream.firstAtTopLevel(start, setOf("RECORD"), end) ?: return emptyList()
        val open = record + 1
        if (stream.valueAt(open) != "(") return emptyList()
        val close = stream.matching(open)
        if (close > end) return emptyList()
        val result = mutableListOf<ProjectRecordField>()

        forEachDelimitedPart(open + 1, close) { fieldStart, fieldEnd ->
            parseRecordField(fieldStart, fieldEnd, result.size)?.let(result::add)
        }
        return immutableList(result)
    }
    private inline fun forEachDelimitedPart(
        start: Int,
        endExclusive: Int,
        consume: (Int, Int) -> Unit
    ) {
        var partStart = start
        var depth = 0
        for (index in start..endExclusive) {
            when (stream.valueAt(index)) {
                "(", "[" -> depth++
                ")", "]" -> depth--
            }
            if ((stream.valueAt(index) == "," && depth == 0) || index == endExclusive) {
                if (partStart < index) consume(partStart, index)
                partStart = index + 1
            }
        }
    }
    private fun parseRecordField(start: Int, endExclusive: Int, ordinal: Int): ProjectRecordField? {
        val name = stream.identifierAt(start) ?: return null
        val typeEnd = stream.firstAtTopLevel(
            name.second,
            setOf("NOT", "NULL", "DEFAULT", ":="),
            endExclusive
        ) ?: endExclusive
        val typeRef = typeReferences.declaredType(name.second, typeEnd) ?: return null
        return ProjectRecordField(name.first, ordinal, typeRef, stream.range(start, endExclusive - 1))
    }

    fun parsePackageSubtype(index: Int, owner: QualifiedName): ParsedDeclaration? {
        val name = stream.identifierAt(index + 1) ?: return null
        val isIndex = stream.firstAtTopLevel(name.second, setOf("IS")) ?: return null
        val end = stream.semicolonAfter(index + 1)
        // The base datatype stops at a top-level NOT NULL, NULL or RANGE keyword; delimiters that cannot be
        // matched keep the previous boundary (the first top-level RANGE, else the semicolon).
        val typeEnd = datatypeShape.datatypeEnd(isIndex + 1, end) {
            stream.isKeyword(it, "NOT") || stream.isKeyword(it, "NULL") || stream.isKeyword(it, "RANGE")
        } ?: stream.firstAtTopLevel(isIndex + 1, setOf("RANGE"), end) ?: end
        val type = typeReferences.declaredType(isIndex + 1, typeEnd) ?: return null
        val declaration = PackageSubtypeDeclaration(owner, name.first, type, stream.fileId, stream.range(index, end))
        return ParsedDeclaration(declaration, end + 1)
    }

    fun parseSubprogram(index: Int, owner: QualifiedName): ParsedDeclaration? {
        val function = stream.isKeyword(index, "FUNCTION")
        val name = stream.identifierAt(index + 1) ?: return null
        var cursor = name.second
        val parameters: List<ProjectParameter>
        if (stream.isPunctuator(cursor, PlSqlPunctuator.LPARENTHESIS)) {
            val close = stream.strictClose(cursor, stream.tokens.size) ?: return null
            parameters = parseParameters(cursor + 1, close) ?: return null
            cursor = close + 1
        } else {
            parameters = emptyList()
        }
        val end = stream.semicolonAfter(index)
        if (function) {
            val returnIndex = stream.firstKeywordAtTopLevel(cursor, setOf("RETURN"), end) ?: return null
            val modifier = stream.firstKeywordAtTopLevel(
                returnIndex + 1,
                FUNCTION_RETURN_TYPE_TERMINATORS,
                end
            )
            val deterministic = stream.firstKeywordAtTopLevel(returnIndex + 1, setOf("DETERMINISTIC"), end) != null
            val returnTypeEnd = modifier ?: end
            val returnType = typeReferences.declaredType(returnIndex + 1, returnTypeEnd) ?: return null
            return ParsedDeclaration(
                PackageFunctionDeclaration(
                    owner,
                    name.first,
                    parameters,
                    returnType,
                    stream.fileId,
                    stream.range(index, end),
                    deterministic = deterministic
                ),
                end + 1
            )
        }
        return ParsedDeclaration(
            PackageProcedureDeclaration(owner, name.first, parameters, stream.fileId, stream.range(index, end)),
            end + 1
        )
    }

    /**
     * `CURSOR name [ ( parameters ) ] RETURN rowtype ;` or `CURSOR name [ ( parameters ) ] [ RETURN rowtype ] IS query ;`
     * ending at the strict `;` at [semicolon]. Parameters must all be plain `IN` (no `OUT`, `IN OUT` or `NOCOPY`); the
     * return type must be a strictly valid datatype; the query is bounded by the `;` and not parsed. Anything else yields
     * null, never a partial fact.
     */
    fun parseCursor(index: Int, semicolon: Int, owner: QualifiedName): ParsedDeclaration? {
        val nameIndex = index + 1
        if (!stream.isKeyword(index, "CURSOR") || nameIndex >= semicolon || !stream.isName(stream.tokens[nameIndex]) ||
            stream.isPunctuator(nameIndex + 1, PlSqlPunctuator.DOT)
        ) return null
        var cursor = nameIndex + 1
        var parameters: List<ProjectParameter> = emptyList()
        if (stream.isPunctuator(cursor, PlSqlPunctuator.LPARENTHESIS)) {
            val close = stream.strictClose(cursor, semicolon) ?: return null
            if (close == cursor + 1) return null
            parameters = parseParameters(cursor + 1, close) ?: return null
            if (parameters.any { it.mode != ParameterMode.IN || it.nocopy }) return null
            cursor = close + 1
        }
        var returnType: TypeRef? = null
        if (stream.isKeyword(cursor, "RETURN")) {
            val returnEnd = stream.firstKeywordAtTopLevel(cursor + 1, setOf("IS"), semicolon) ?: semicolon
            if (!datatypeShape.strictDatatype(cursor + 1, returnEnd) || !datatypeShape.strictTypeArguments(cursor + 1, returnEnd)) return null
            returnType = typeReferences.typeReference(cursor + 1, returnEnd) ?: return null
            cursor = returnEnd
        }
        when {
            cursor == semicolon -> if (returnType == null) return null
            stream.isKeyword(cursor, "IS") -> if (cursor + 1 >= semicolon) return null
            else -> return null
        }
        val declaration = PackageCursorDeclaration(
            owner, stream.identifier(stream.tokens[nameIndex]), parameters, returnType, stream.fileId, stream.range(index, semicolon)
        )
        return ParsedDeclaration(declaration, semicolon + 1)
    }

    /** `name EXCEPTION ;` and nothing else, ending at the strict `;` at [semicolon]. */
    fun parseException(index: Int, semicolon: Int, owner: QualifiedName): ParsedDeclaration? {
        if (index + 2 != semicolon || !stream.isName(stream.tokens[index]) || !stream.isKeyword(index + 1, "EXCEPTION")) return null
        val declaration = PackageExceptionDeclaration(owner, stream.identifier(stream.tokens[index]), stream.fileId, stream.range(index, semicolon))
        return ParsedDeclaration(declaration, semicolon + 1)
    }

    fun parseParameters(start: Int, close: Int): List<ProjectParameter>? {
        if (start == close) return emptyList()
        val result = mutableListOf<ProjectParameter>()
        val expected = ArrayDeque<PlSqlPunctuator>()
        var partStart = start
        for (index in start until close) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return null
                expected.isEmpty() && stream.isPunctuator(index, PlSqlPunctuator.COMMA) -> {
                    if (partStart == index) return null
                    result += parseParameter(partStart, index, result.size + 1) ?: return null
                    partStart = index + 1
                }
            }
        }
        if (expected.isNotEmpty() || partStart == close) return null
        result += parseParameter(partStart, close, result.size + 1) ?: return null
        return result.toList()
    }

    private fun parseParameter(start: Int, endExclusive: Int, ordinal: Int): ProjectParameter? {
        val name = stream.identifierAt(start) ?: return null
        var cursor = name.second
        var mode = ParameterMode.IN
        if (stream.isKeyword(cursor, "IN")) {
            cursor++
            if (stream.isKeyword(cursor, "OUT")) {
                mode = ParameterMode.IN_OUT
                cursor++
            }
        } else if (stream.isKeyword(cursor, "OUT")) {
            mode = ParameterMode.OUT
            cursor++
        }
        val nocopy = stream.isKeyword(cursor, "NOCOPY")
        if (nocopy) cursor++
        val defaultIndex = stream.firstKeywordAtTopLevel(cursor, setOf("DEFAULT"), endExclusive)
        val assignmentIndex = stream.firstAtTopLevel(cursor, setOf(":="), endExclusive)
        val typeEnd = listOfNotNull(defaultIndex, assignmentIndex).minOrNull() ?: endExclusive
        val type = typeReferences.declaredType(cursor, typeEnd) ?: return null
        return ProjectParameter(
            name.first,
            ordinal,
            mode,
            nocopy,
            type,
            defaultIndex != null || assignmentIndex != null,
            stream.range(start, endExclusive - 1)
        )
    }
}
