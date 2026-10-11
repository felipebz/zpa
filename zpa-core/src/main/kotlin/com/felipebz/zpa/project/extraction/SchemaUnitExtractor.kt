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
import com.felipebz.zpa.project.ProjectTypeShape
import com.felipebz.zpa.project.SequenceDeclaration
import com.felipebz.zpa.project.StandaloneTypeDeclaration

/** `CREATE SEQUENCE` and `CREATE [OR REPLACE] TYPE [BODY]` units, recognized with the value-based token operations. */
internal class SchemaUnitExtractor(
    private val stream: TokenStream,
    private val typeReferences: TypeReferences,
    private val bounds: UnitBounds,
    private val collectionElements: CollectionElementSyntax
) {
    fun parseSequence(create: Int, sequenceIndex: Int): ParsedUnit? {
        // CREATE SEQUENCE has no OR REPLACE or EDITIONABLE prefix in the supported
        // grammar. Keep extraction aligned with that grammar even though parseCreate
        // handles those prefixes for other schema objects.
        if (sequenceIndex != create + 1) return null

        val ifNotExists = stream.valueAt(sequenceIndex + 1) == "IF" &&
            stream.valueAt(sequenceIndex + 2) == "NOT" && stream.valueAt(sequenceIndex + 3) == "EXISTS"
        val nameIndex = if (ifNotExists) sequenceIndex + 4 else sequenceIndex + 1
        val parsedName = stream.qualifiedName(nameIndex) ?: return null
        if (parsedName.first.segments.size !in 1..2) return null

        val end = stream.semicolonAfter(parsedName.second)
        return ParsedUnit(
            listOf(SequenceDeclaration(parsedName.first, stream.fileId, stream.range(create, end))),
            end + 1
        )
    }

    fun parseStandaloneType(create: Int, typeIndex: Int): ParsedUnit? {
        val name = stream.qualifiedName(typeIndex + 1) ?: return null
        val end = stream.semicolonAfter(typeIndex + 1)
        val collection = standaloneCollection(create, name.second, end)
        val shape = typeReferences.typeShape(typeIndex, end) ?: ProjectTypeShape.COLLECTION.takeIf { collection != null }
        val declaration = StandaloneTypeDeclaration(
            name.first, shape, stream.fileId, stream.range(create, end),
            elementType = collection?.elementType?.takeIf { shape == ProjectTypeShape.COLLECTION }
        )
        return ParsedUnit(listOf(declaration), end + 1)
    }

    /**
     * Collection syntax of `CREATE TYPE name (IS | AS) ...`, with `IS`/`AS` directly after the name (header options
     * such as `FORCE` are not supported). The element is retained only when the declaration's existing end [end]
     * coincides with its reliable end inside the unit: the first typed top-level `;` before the unit boundary, or
     * the boundary itself when there is no `;`. A declaration that runs into the next unit keeps its fact without
     * an element.
     */
    private fun standaloneCollection(create: Int, afterName: Int, end: Int): CollectionSyntax? {
        if (!stream.isKeyword(afterName, "IS") && !stream.isKeyword(afterName, "AS")) return null
        val limit = bounds.unitLimit(create)
        return collectionElements.parseStandalone(afterName, limit, reliableEnd(afterName, limit)?.takeIf { it == end })
    }

    /** First `;` at typed delimiter depth 0 in `[start, limit)`, else [limit]; null when delimiters are crossed. */
    private fun reliableEnd(start: Int, limit: Int): Int? {
        val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
        for (index in start until limit.coerceAtMost(stream.tokens.size)) {
            when {
                stream.isOpener(index) -> expected.addLast(stream.closerFor(index))
                stream.isCloser(index) -> if (expected.removeLastOrNull() !== stream.tokens[index].type) return null
                expected.isEmpty() && stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return index
            }
        }
        return if (expected.isEmpty()) limit else null
    }

    fun parseTypeBody(typeIndex: Int): ParsedUnit {
        val nextCreate = stream.nextValue(typeIndex + 2, "CREATE")
        return ParsedUnit(emptyList(), nextCreate ?: stream.tokens.size)
    }
}
