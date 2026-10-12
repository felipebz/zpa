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

/**
 * Value-based extraction of the subprograms of a package body. Like the code it was moved from, the walk is
 * bounded by the end of the token list, not by the unit boundaries.
 */
internal class PackageBodyExtractor(
    private val stream: TokenStream,
    private val bodyBoundaries: BodyBoundaryScanner,
    private val memberParser: PackageMemberParser,
    private val typeReferences: TypeReferences
) {
    fun parsePackageBody(owner: QualifiedName, afterName: Int): ParsedUnit? {
        val header = stream.firstAtTopLevel(afterName, setOf("IS", "AS")) ?: return null
        val parsed = parsePackageBodySubprograms(header + 1, owner)
        return ParsedUnit(parsed.declarations, parsed.end + 1)
    }

    private fun parsePackageBodySubprograms(start: Int, owner: QualifiedName): ParsedPackageBody {
        val result = mutableListOf<ProjectDeclaration>()
        var cursor = start
        val blocks = mutableListOf<BlockKind>()
        while (cursor < stream.tokens.size) {
            val parsed = subprogramAt(cursor, blocks, owner)
            if (parsed != null) {
                parsed.declaration?.let(result::add)
                cursor = parsed.nextIndex
                continue
            }

            when {
                bodyBoundaries.isCaseStart(cursor) -> blocks += BlockKind.CASE
                stream.isKeyword(cursor, "BEGIN") -> {
                    if (blocks.isEmpty()) {
                        val end = bodyBoundaries.executableBlockEnd(cursor, stream.tokens.size) ?: stream.tokens.lastIndex
                        return ParsedPackageBody(immutableList(result), end)
                    }
                    blocks += BlockKind.BEGIN
                }
                stream.isKeyword(cursor, "END") ->
                    if (bodyBoundaries.closeBlock(cursor, blocks) == BlockClose.UNBALANCED) {
                        return ParsedPackageBody(immutableList(result), stream.semicolonAfter(cursor))
                    }
            }
            cursor++
        }
        return ParsedPackageBody(immutableList(result), stream.tokens.lastIndex)
    }

    /** The subprogram that starts at [cursor], only outside any block. */
    private fun subprogramAt(cursor: Int, blocks: List<BlockKind>, owner: QualifiedName): ParsedBodySubprogram? =
        if (blocks.isEmpty() && bodyBoundaries.isSubprogramKeyword(cursor)) parseBodySubprogram(cursor, stream.tokens.size, owner) else null

    private fun parseBodySubprogram(index: Int, end: Int, owner: QualifiedName): ParsedBodySubprogram? {
        val function = stream.valueAt(index) == "FUNCTION"
        val name = stream.identifierAt(index + 1) ?: return null
        var cursor = name.second
        val parameters: List<ProjectParameter>
        if (stream.valueAt(cursor) == "(") {
            val close = stream.matching(cursor)
            if (close >= end) return null
            parameters = memberParser.parseParameters(cursor + 1, close)
            cursor = close + 1
        } else {
            parameters = emptyList()
        }

        val header = bodyBoundaries.firstBodyHeaderDelimiter(cursor, end) ?: return null
        if (header.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
            return ParsedBodySubprogram(null, header.index + 1)
        }

        val implementationEnd = bodyBoundaries.implementationEnd(header.index + 1, end) ?: return null
        val declaration = if (function) {
            val returnIndex = stream.firstAtTopLevel(cursor, setOf("RETURN"), header.index)
                ?: return null
            val modifier = stream.firstAtTopLevel(
                returnIndex + 1,
                FUNCTION_RETURN_TYPE_TERMINATORS,
                header.index
            )
            val returnType = typeReferences.typeReference(returnIndex + 1, modifier ?: header.index)
                ?: return null
            PackageFunctionDeclaration(
                owner,
                name.first,
                parameters,
                returnType,
                stream.fileId,
                stream.range(index, implementationEnd),
                DeclarationRole.BODY
            )
        } else {
            PackageProcedureDeclaration(
                owner,
                name.first,
                parameters,
                stream.fileId,
                stream.range(index, implementationEnd),
                DeclarationRole.BODY
            )
        }
        return ParsedBodySubprogram(declaration, implementationEnd + 1)
    }

    private data class ParsedBodySubprogram(
        val declaration: ProjectDeclaration?,
        val nextIndex: Int
    )
    private data class ParsedPackageBody(
        val declarations: List<ProjectDeclaration>,
        val end: Int
    )
}
