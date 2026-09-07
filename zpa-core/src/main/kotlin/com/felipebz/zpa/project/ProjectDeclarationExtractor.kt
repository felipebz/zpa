package com.felipebz.zpa.project

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.flr.impl.Lexer
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets
import java.util.Locale

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
            if (valueAt(index) == "OR" && valueAt(index + 1) == "REPLACE") index += 2
            if (valueAt(index) == "EDITIONABLE" || valueAt(index) == "NONEDITIONABLE") index++
            return when (valueAt(index)) {
                "PACKAGE" -> parsePackage(create, index)
                "TYPE" -> if (valueAt(index + 1) == "BODY") parseTypeBody(create, index) else parseStandaloneType(create, index)
                else -> null
            }
        }

        private fun parsePackage(create: Int, packageIndex: Int): ParsedUnit? {
            val body = valueAt(packageIndex + 1) == "BODY"
            val nameIndex = if (body) packageIndex + 2 else packageIndex + 1
            val name = qualifiedName(nameIndex) ?: return null
            if (body) {
                val nextCreate = nextValue(name.second, "CREATE")
                // The body is recognized only to keep its private declarations out of the
                // public index. Its role is reserved in the model for later correlation.
                return ParsedUnit(emptyList(), nextCreate ?: tokens.size)
            }

            val header = firstAtTopLevel(name.second, setOf("IS", "AS")) ?: return null
            val end = packageEnd(header + 1)
            val owner = name.first
            val declarations = mutableListOf<ProjectDeclaration>()
            declarations += PackageDeclaration(owner, DeclarationRole.SPECIFICATION, fileId, range(create, end))
            var cursor = header + 1
            while (cursor < end && valueAt(cursor) != "END") {
                when (valueAt(cursor)) {
                    "TYPE" -> {
                        val parsed = parsePackageType(cursor, owner)
                        if (parsed != null) declarations += parsed.declaration
                        cursor = parsed?.nextIndex ?: skipToSemicolon(cursor)
                    }
                    "SUBTYPE" -> {
                        val parsed = parsePackageSubtype(cursor, owner)
                        if (parsed != null) declarations += parsed.declaration
                        cursor = parsed?.nextIndex ?: skipToSemicolon(cursor)
                    }
                    "PROCEDURE", "FUNCTION" -> {
                        val parsed = parseSubprogram(cursor, owner)
                        if (parsed != null) declarations += parsed.declaration
                        cursor = parsed?.nextIndex ?: skipToSemicolon(cursor)
                    }
                    else -> cursor = skipToSemicolon(cursor)
                }
            }
            return ParsedUnit(declarations, end + 1)
        }

        private fun parsePackageType(index: Int, owner: QualifiedName): ParsedDeclaration? {
            val name = identifierAt(index + 1) ?: return null
            val end = semicolonAfter(index + 1)
            val shape = typeShape(index, end)
            val declaration = PackageTypeDeclaration(owner, name.first, shape, fileId, range(index, end))
            return ParsedDeclaration(declaration, end + 1)
        }

        private fun parsePackageSubtype(index: Int, owner: QualifiedName): ParsedDeclaration? {
            val name = identifierAt(index + 1) ?: return null
            val isIndex = firstAtTopLevel(name.second, setOf("IS")) ?: return null
            val end = semicolonAfter(index + 1)
            val typeEnd = firstAtTopLevel(isIndex + 1, setOf("RANGE"), end) ?: end
            val type = typeReference(isIndex + 1, typeEnd) ?: return null
            val declaration = PackageSubtypeDeclaration(owner, name.first, type, fileId, range(index, end))
            return ParsedDeclaration(declaration, end + 1)
        }

        private fun parseSubprogram(index: Int, owner: QualifiedName): ParsedDeclaration? {
            val function = valueAt(index) == "FUNCTION"
            val name = identifierAt(index + 1) ?: return null
            var cursor = name.second
            val parameters: List<ProjectParameter>
            if (valueAt(cursor) == "(") {
                val close = matching(cursor)
                parameters = parseParameters(cursor + 1, close)
                cursor = close + 1
            } else {
                parameters = emptyList()
            }
            val end = semicolonAfter(index)
            if (function) {
                val returnIndex = firstAtTopLevel(cursor, setOf("RETURN"), end) ?: return null
                val modifier = firstAtTopLevel(
                    returnIndex + 1,
                    setOf("DETERMINISTIC", "PIPELINED", "PARALLEL_ENABLE", "RESULT_CACHE"),
                    end
                )
                val returnType = typeReference(returnIndex + 1, modifier ?: end) ?: return null
                return ParsedDeclaration(
                    PackageFunctionDeclaration(owner, name.first, parameters, returnType, fileId, range(index, end)),
                    end + 1
                )
            }
            return ParsedDeclaration(
                PackageProcedureDeclaration(owner, name.first, parameters, fileId, range(index, end)),
                end + 1
            )
        }

        private fun parseParameters(start: Int, close: Int): List<ProjectParameter> {
            val result = mutableListOf<ProjectParameter>()
            var partStart = start
            var depth = 0
            for (index in start..close) {
                when (valueAt(index)) {
                    "(", "[" -> depth++
                    ")", "]" -> depth--
                }
                if ((valueAt(index) == "," && depth == 0) || index == close) {
                    if (partStart < index) parseParameter(partStart, index, result.size + 1)?.let { result += it }
                    partStart = index + 1
                }
            }
            return result.toList()
        }

        private fun parseParameter(start: Int, endExclusive: Int, ordinal: Int): ProjectParameter? {
            val name = identifierAt(start) ?: return null
            var cursor = name.second
            var mode = ParameterMode.IN
            if (valueAt(cursor) == "IN") {
                cursor++
                if (valueAt(cursor) == "OUT") {
                    mode = ParameterMode.IN_OUT
                    cursor++
                }
            } else if (valueAt(cursor) == "OUT") {
                mode = ParameterMode.OUT
                cursor++
            }
            val nocopy = valueAt(cursor) == "NOCOPY"
            if (nocopy) cursor++
            val defaultIndex = firstAtTopLevel(cursor, setOf("DEFAULT"), endExclusive)
            val assignmentIndex = firstAtTopLevel(cursor, setOf(":="), endExclusive)
            val typeEnd = listOfNotNull(defaultIndex, assignmentIndex).minOrNull() ?: endExclusive
            val type = typeReference(cursor, typeEnd) ?: return null
            return ProjectParameter(
                name.first,
                ordinal,
                mode,
                nocopy,
                type,
                defaultIndex != null || assignmentIndex != null,
                range(start, endExclusive - 1)
            )
        }

        private fun parseStandaloneType(create: Int, typeIndex: Int): ParsedUnit? {
            val name = qualifiedName(typeIndex + 1) ?: return null
            val end = semicolonAfter(typeIndex + 1)
            val declaration = StandaloneTypeDeclaration(name.first, typeShape(typeIndex, end), fileId, range(create, end))
            return ParsedUnit(listOf(declaration), end + 1)
        }

        private fun parseTypeBody(create: Int, typeIndex: Int): ParsedUnit {
            val nextCreate = nextValue(typeIndex + 2, "CREATE")
            return ParsedUnit(emptyList(), nextCreate ?: tokens.size)
        }

        private fun typeShape(start: Int, end: Int): ProjectTypeShape? {
            val values = tokens.subList(start, (end + 1).coerceAtMost(tokens.size)).map { value(it) }
            return when {
                "RECORD" in values -> ProjectTypeShape.RECORD
                "OBJECT" in values -> ProjectTypeShape.OBJECT
                "VARRAY" in values || "TABLE" in values -> ProjectTypeShape.COLLECTION
                "REF" in values && "CURSOR" in values -> ProjectTypeShape.REF_CURSOR
                else -> null
            }
        }

        private fun typeReference(start: Int, endExclusive: Int): TypeRef? {
            if (start >= endExclusive || start >= tokens.size) return null
            val end = endExclusive.coerceAtMost(tokens.size)
            val selected = tokens.subList(start, end)
            val percent = selected.indexOfFirst { value(it) == "%" }
            val nameTokens = if (percent >= 0) selected.subList(0, percent) else selected
            val name = qualifiedSegments(nameTokens) ?: return null
            val sourceRange = range(start, end - 1)
            if (percent >= 0) {
                val anchor = when (valueAt(start + percent + 1)) {
                    "TYPE" -> TypeAnchor.TYPE
                    "ROWTYPE" -> TypeAnchor.ROWTYPE
                    else -> null
                }
                if (anchor != null) return AnchoredTypeRef(name, anchor, sourceRange)
            }
            return NamedTypeRef(name, sourceRange)
        }

        private fun qualifiedName(start: Int): Pair<QualifiedName, Int>? {
            val first = identifierAt(start) ?: return null
            val segments = mutableListOf(first.first)
            var cursor = first.second
            while (valueAt(cursor) == ".") {
                val next = identifierAt(cursor + 1) ?: break
                segments += next.first
                cursor = next.second
            }
            return QualifiedName(segments) to cursor
        }

        private fun qualifiedSegments(tokens: List<Token>): QualifiedName? {
            val segments = mutableListOf<OracleIdentifier>()
            var index = 0
            if (tokens.isEmpty() || !isName(tokens[0])) return null
            while (index < tokens.size && isName(tokens[index])) {
                segments += identifier(tokens[index])
                index++
                if (index < tokens.size && tokens[index].value == "." && isNameAt(tokens, index + 1)) {
                    index++
                } else {
                    break
                }
            }
            return segments.takeIf { it.isNotEmpty() }?.let(::QualifiedName)
        }

        private fun isNameAt(tokens: List<Token>, index: Int) = index in tokens.indices && isName(tokens[index])

        private fun identifierAt(start: Int): Pair<OracleIdentifier, Int>? {
            if (start !in tokens.indices || !isName(tokens[start])) return null
            return identifier(tokens[start]) to start + 1
        }

        private fun identifier(token: Token) = OracleIdentifier.fromToken(token.originalValue, token.value)

        private fun packageEnd(start: Int): Int {
            var depth = 0
            for (index in start until tokens.size) {
                when (valueAt(index)) {
                    "(" , "[" -> depth++
                    ")", "]" -> depth--
                    "END" -> if (depth == 0 && isPackageTerminator(index)) {
                        val semi = semicolonAfter(index)
                        return semi
                    }
                }
            }
            return tokens.lastIndex
        }

        private fun isPackageTerminator(index: Int): Boolean {
            val next = index + 1
            return valueAt(next) == ";" || (next in tokens.indices && isName(tokens[next]) && valueAt(next + 1) == ";")
        }

        private fun firstAtTopLevel(start: Int, wanted: Set<String>, endExclusive: Int = tokens.size): Int? {
            var depth = 0
            for (index in start until endExclusive.coerceAtMost(tokens.size)) {
                when (valueAt(index)) {
                    "(", "[" -> depth++
                    ")", "]" -> depth--
                }
                if (depth == 0 && valueAt(index) in wanted) return index
            }
            return null
        }

        private fun matching(open: Int): Int {
            var depth = 0
            for (index in open until tokens.size) {
                when (valueAt(index)) {
                    "(" -> depth++
                    ")" -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
            return tokens.lastIndex
        }

        private fun semicolonAfter(start: Int): Int {
            var depth = 0
            for (index in start until tokens.size) {
                when (valueAt(index)) {
                    "(", "[" -> depth++
                    ")", "]" -> depth--
                    ";" -> if (depth == 0) return index
                }
            }
            return tokens.lastIndex.coerceAtLeast(start)
        }

        private fun skipToSemicolon(start: Int) = semicolonAfter(start) + 1

        private fun nextValue(start: Int, wanted: String): Int? =
            (start until tokens.size).firstOrNull { valueAt(it) == wanted }

        private fun range(start: Int, end: Int): SourceRange {
            val first = tokens[start.coerceIn(0, tokens.lastIndex)]
            val last = tokens[end.coerceIn(0, tokens.lastIndex)]
            return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
        }

        private fun isName(token: Token): Boolean =
            token.type == GenericTokenType.IDENTIFIER ||
                (token.type is PlSqlKeyword && !(token.type as PlSqlKeyword).isReserved)

        private fun valueAt(index: Int) = if (index in tokens.indices) tokens[index].value.uppercase(Locale.ROOT) else ""
        private fun value(token: Token) = token.value.uppercase(Locale.ROOT)

        private data class ParsedUnit(val declarations: List<ProjectDeclaration>, val nextIndex: Int)
        private data class ParsedDeclaration(val declaration: ProjectDeclaration, val nextIndex: Int)
    }
}
