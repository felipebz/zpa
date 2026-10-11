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

import com.felipebz.zpa.project.SequenceDeclaration
import com.felipebz.zpa.project.StandaloneTypeDeclaration

/** `CREATE SEQUENCE` and `CREATE [OR REPLACE] TYPE [BODY]` units, recognized with the value-based token operations. */
internal class SchemaUnitExtractor(private val stream: TokenStream, private val typeReferences: TypeReferences) {
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
        val declaration = StandaloneTypeDeclaration(name.first, typeReferences.typeShape(typeIndex, end), stream.fileId, stream.range(create, end))
        return ParsedUnit(listOf(declaration), end + 1)
    }

    fun parseTypeBody(typeIndex: Int): ParsedUnit {
        val nextCreate = stream.nextValue(typeIndex + 2, "CREATE")
        return ParsedUnit(emptyList(), nextCreate ?: stream.tokens.size)
    }
}
