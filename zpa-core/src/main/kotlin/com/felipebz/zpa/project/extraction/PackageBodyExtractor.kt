//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.*

/**
 * Value-based extraction of the subprograms of a package body, and of the names the body declares at its own level.
 * Like a specification, the walk stops at the unit limit (the next real `CREATE` or lone-line `/`): a body without its
 * `;` or its final `END` never takes the next unit's text.
 */
internal class PackageBodyExtractor(
    private val stream: TokenStream,
    private val bodyBoundaries: BodyBoundaryScanner,
    private val memberParser: PackageMemberParser,
    private val typeReferences: TypeReferences,
    private val bounds: UnitBounds,
    private val discardedText: DiscardedSourceText
) {
    private val nameRecorder = MemberNameRecorder(stream)

    fun parsePackageBody(create: Int, owner: QualifiedName, afterName: Int): ParsedUnit? {
        val limit = bounds.unitLimit(create)
        val header = stream.firstAtTopLevel(afterName, setOf("IS", "AS")) ?: return null
        if (header >= limit) return null
        val parsed = parsePackageBodySubprograms(header, owner, limit)
        return ParsedUnit(parsed.declarations, parsed.end + 1, PackageBodyMemberNames(owner, stream.range(create, parsed.end), parsed.names))
    }

    private fun parsePackageBodySubprograms(header: Int, owner: QualifiedName, limit: Int): ParsedPackageBody {
        val result = mutableListOf<ProjectDeclaration>()
        val level = LevelNames(header)
        var cursor = header + 1
        val blocks = mutableListOf<BlockKind>()
        while (cursor < limit) {
            if (blocks.isEmpty() && bodyBoundaries.isSubprogramKeyword(cursor)) {
                val parsed = parseBodySubprogram(cursor, limit, owner)
                if (parsed != null) {
                    parsed.declaration?.let(result::add)
                    level.subprogram(cursor, parsed.nextIndex - 1)
                    cursor = parsed.nextIndex
                    continue
                }
                // The walk now reads the subprogram's own text token by token: its names are not the body's.
                level.lose()
            }

            val atLevel = blocks.isEmpty()
            val opened = bodyBoundaries.statementStart(cursor)
            when {
                opened != null -> blocks += opened
                stream.isKeyword(cursor, "BEGIN") -> {
                    if (blocks.isEmpty()) {
                        val end = bodyBoundaries.executableBlockEnd(cursor, limit)?.coerceAtMost(limit - 1) ?: (limit - 1)
                        return ParsedPackageBody(immutableList(result), end, level.finish(cursor))
                    }
                    blocks += BlockKind.BEGIN
                }
                stream.isKeyword(cursor, "END") ->
                    if (bodyBoundaries.closeBlock(cursor, blocks) == BlockClose.UNBALANCED) {
                        return ParsedPackageBody(immutableList(result), minOf(stream.semicolonAfter(cursor), limit - 1), level.finish(cursor))
                    }
            }
            if (atLevel) level.token(cursor, blocks.isEmpty() && stream.isPunctuator(cursor, PlSqlPunctuator.SEMICOLON))
            cursor++
        }
        level.unterminated()
        return ParsedPackageBody(immutableList(result), limit - 1, level.finish(limit - 1))
    }

    // Discarded text is scanned only outside the subprograms: their own text declares no name of the body's level.
    private inner class LevelNames(private var segmentStart: Int) {
        private val names = mutableListOf<PackageMemberName>()
        private val gaps = mutableSetOf<MemberNameGap>()
        private val scans = mutableListOf<DiscardedSourceText.Scan>()
        private var declarationStart = -1
        private var lost = false

        fun token(index: Int, ends: Boolean) {
            if (lost) return
            if (declarationStart < 0) declarationStart = index
            if (ends) {
                nameRecorder.record(declarationStart, index, true, names, gaps)
                declarationStart = -1
            }
        }

        fun subprogram(start: Int, last: Int) {
            if (lost) return
            brokenDeclaration(start)
            scans += discardedText.scan(stream.tokens, segmentStart, start)
            segmentStart = last
            val kind = if (stream.isKeyword(start, "FUNCTION")) PackageMemberKind.FUNCTION else PackageMemberKind.PROCEDURE
            stream.identifierAt(start + 1)?.let { names += PackageMemberName(it.first, kind, stream.range(start, last)) }
                ?: run { gaps += MemberNameGap.UNIDENTIFIED_DECLARATION }
        }

        fun lose() {
            gaps += MemberNameGap.UNIDENTIFIED_DECLARATION
            lost = true
        }

        fun unterminated() {
            gaps += MemberNameGap.UNTERMINATED_DECLARATION
        }

        fun finish(end: Int): PackageMemberNameCoverage {
            if (!lost) brokenDeclaration(end)
            scans += discardedText.scan(stream.tokens, segmentStart, end)
            scans.forEach { scan -> scan.kinds.forEach { gaps += nameRecorder.discardedGap(it) } }
            val conditional = scans.mapNotNull { it.conditionalTokens }
            val range = if (conditional.isEmpty()) null else stream.range(conditional.minOf { it.first }, conditional.maxOf { it.last })
            val withConditional = scans.filter { CallableCoverageGap.CONDITIONAL_COMPILATION in it.kinds }
            val mentions = if (withConditional.isEmpty() || withConditional.any { it.conditionalMentions == null }) null
            else withConditional.flatMapTo(HashSet()) { it.conditionalMentions!! }
            return PackageMemberNameCoverage(names, gaps, range, mentions)
        }

        private fun brokenDeclaration(next: Int) {
            if (declarationStart < 0) return
            nameRecorder.record(declarationStart, next, false, names, gaps)
            declarationStart = -1
        }
    }

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
        val end: Int,
        val names: PackageMemberNameCoverage
    )
}
