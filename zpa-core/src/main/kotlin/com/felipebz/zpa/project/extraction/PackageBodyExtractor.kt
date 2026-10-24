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

/**
 * Value-based extraction of the subprograms of a package body. Like a specification, the walk stops at the unit limit
 * (the next real `CREATE` or lone-line `/`): a body without its `;` or its final `END` never takes the next unit's text.
 */
internal class PackageBodyExtractor(
    private val stream: TokenStream,
    private val bodyBoundaries: BodyBoundaryScanner,
    private val memberParser: PackageMemberParser,
    private val typeReferences: TypeReferences,
    private val bounds: UnitBounds,
) {
    fun parsePackageBody(create: Int, owner: QualifiedName, afterName: Int): ParsedUnit? {
        val limit = bounds.unitLimit(create)
        val header = stream.firstAtTopLevel(afterName, setOf("IS", "AS")) ?: return null
        if (header >= limit) return null
        val parsed = parsePackageBodySubprograms(header + 1, owner, limit)
        return ParsedUnit(parsed.declarations, parsed.end + 1)
    }

    private fun parsePackageBodySubprograms(start: Int, owner: QualifiedName, limit: Int): ParsedPackageBody {
        val result = mutableListOf<ProjectDeclaration>()
        var cursor = start
        val blocks = mutableListOf<BlockKind>()
        while (cursor < limit) {
            val parsed = subprogramAt(cursor, blocks, owner, limit)
            if (parsed != null) {
                parsed.declaration?.let(result::add)
                cursor = parsed.nextIndex
                continue
            }

            val opened = bodyBoundaries.statementStart(cursor)
            when {
                opened != null -> blocks += opened
                stream.isKeyword(cursor, "BEGIN") -> {
                    if (blocks.isEmpty()) {
                        val end = bodyBoundaries.executableBlockEnd(cursor, limit)?.coerceAtMost(limit - 1) ?: (limit - 1)
                        return ParsedPackageBody(immutableList(result), end)
                    }
                    blocks += BlockKind.BEGIN
                }
                stream.isKeyword(cursor, "END") ->
                    if (bodyBoundaries.closeBlock(cursor, blocks) == BlockClose.UNBALANCED) {
                        return ParsedPackageBody(immutableList(result), minOf(stream.semicolonAfter(cursor), limit - 1))
                    }
            }
            cursor++
        }
        return ParsedPackageBody(immutableList(result), limit - 1)
    }

    /** The subprogram that starts at [cursor], only outside any block. */
    private fun subprogramAt(cursor: Int, blocks: List<BlockKind>, owner: QualifiedName, limit: Int): ParsedBodySubprogram? =
        if (blocks.isEmpty() && bodyBoundaries.isSubprogramKeyword(cursor)) parseBodySubprogram(cursor, limit, owner) else null

    private fun parseBodySubprogram(index: Int, end: Int, owner: QualifiedName): ParsedBodySubprogram? {
        val function = stream.isKeyword(index, "FUNCTION")
        val name = stream.identifierAt(index + 1) ?: return null
        var cursor = name.second
        val parameters: List<ProjectParameter>
        if (stream.isPunctuator(cursor, PlSqlPunctuator.LPARENTHESIS)) {
            val close = stream.strictClose(cursor, end) ?: return null
            val parsedParameters = memberParser.parseParameters(cursor + 1, close)
            if (parsedParameters == null) {
                val skippedHeader = bodyBoundaries.firstBodyHeaderDelimiter(close + 1, end) ?: return null
                if (skippedHeader.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
                    return ParsedBodySubprogram(null, skippedHeader.index + 1)
                }
                val skippedEnd = bodyBoundaries.implementationEnd(skippedHeader.index + 1, end) ?: return null
                return ParsedBodySubprogram(null, skippedEnd + 1)
            }
            parameters = parsedParameters
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
            // A missing or rejected return type skips this subprogram, never the members after it.
            val returnIndex = stream.firstKeywordAtTopLevel(cursor, setOf("RETURN"), header.index)
                ?: return ParsedBodySubprogram(null, implementationEnd + 1)
            val modifier = stream.firstKeywordAtTopLevel(
                returnIndex + 1,
                FUNCTION_RETURN_TYPE_TERMINATORS,
                header.index
            )
            val returnTypeEnd = modifier ?: header.index
            val returnType = typeReferences.declaredType(returnIndex + 1, returnTypeEnd)
                ?: return ParsedBodySubprogram(null, implementationEnd + 1)
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
