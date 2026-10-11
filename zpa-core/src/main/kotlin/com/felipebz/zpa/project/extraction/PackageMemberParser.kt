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
    private val collectionElements: CollectionElementSyntax
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

    private fun parseRecordField(start: Int, endExclusive: Int, ordinal: Int): ProjectRecordField? {
        val name = stream.identifierAt(start) ?: return null
        val typeEnd = stream.firstAtTopLevel(
            name.second,
            setOf("NOT", "NULL", "DEFAULT", ":="),
            endExclusive
        ) ?: endExclusive
        val typeRef = typeReferences.typeReference(name.second, typeEnd) ?: return null
        return ProjectRecordField(name.first, ordinal, typeRef, stream.range(start, endExclusive - 1))
    }

    fun parsePackageSubtype(index: Int, owner: QualifiedName): ParsedDeclaration? {
        val name = stream.identifierAt(index + 1) ?: return null
        val isIndex = stream.firstAtTopLevel(name.second, setOf("IS")) ?: return null
        val end = stream.semicolonAfter(index + 1)
        val typeEnd = stream.firstAtTopLevel(isIndex + 1, setOf("RANGE"), end) ?: end
        val type = typeReferences.typeReference(isIndex + 1, typeEnd) ?: return null
        val declaration = PackageSubtypeDeclaration(owner, name.first, type, stream.fileId, stream.range(index, end))
        return ParsedDeclaration(declaration, end + 1)
    }

    fun parseSubprogram(index: Int, owner: QualifiedName): ParsedDeclaration? {
        val function = stream.valueAt(index) == "FUNCTION"
        val name = stream.identifierAt(index + 1) ?: return null
        var cursor = name.second
        val parameters: List<ProjectParameter>
        if (stream.valueAt(cursor) == "(") {
            val close = stream.matching(cursor)
            parameters = parseParameters(cursor + 1, close)
            cursor = close + 1
        } else {
            parameters = emptyList()
        }
        val end = stream.semicolonAfter(index)
        if (function) {
            val returnIndex = stream.firstAtTopLevel(cursor, setOf("RETURN"), end) ?: return null
            val modifier = stream.firstAtTopLevel(
                returnIndex + 1,
                FUNCTION_RETURN_TYPE_TERMINATORS,
                end
            )
            val deterministic = stream.firstAtTopLevel(returnIndex + 1, setOf("DETERMINISTIC"), end) != null
            val returnType = typeReferences.typeReference(returnIndex + 1, modifier ?: end) ?: return null
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

    fun parseParameters(start: Int, close: Int): List<ProjectParameter> {
        val result = mutableListOf<ProjectParameter>()
        forEachDelimitedPart(start, close) { partStart, partEnd ->
            parseParameter(partStart, partEnd, result.size + 1)?.let(result::add)
        }
        return result.toList()
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

    private fun parseParameter(start: Int, endExclusive: Int, ordinal: Int): ProjectParameter? {
        val name = stream.identifierAt(start) ?: return null
        var cursor = name.second
        var mode = ParameterMode.IN
        if (stream.valueAt(cursor) == "IN") {
            cursor++
            if (stream.valueAt(cursor) == "OUT") {
                mode = ParameterMode.IN_OUT
                cursor++
            }
        } else if (stream.valueAt(cursor) == "OUT") {
            mode = ParameterMode.OUT
            cursor++
        }
        val nocopy = stream.valueAt(cursor) == "NOCOPY"
        if (nocopy) cursor++
        val defaultIndex = stream.firstAtTopLevel(cursor, setOf("DEFAULT"), endExclusive)
        val assignmentIndex = stream.firstAtTopLevel(cursor, setOf(":="), endExclusive)
        val typeEnd = listOfNotNull(defaultIndex, assignmentIndex).minOrNull() ?: endExclusive
        val type = typeReferences.typeReference(cursor, typeEnd) ?: return null
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
