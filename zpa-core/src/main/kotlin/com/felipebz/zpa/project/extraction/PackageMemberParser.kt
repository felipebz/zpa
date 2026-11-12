//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
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

private val SELECT_LIST_TERMINATORS = setOf("FROM", "UNION", "INTERSECT", "MINUS", "EXCEPT", "ORDER")

/** Keywords written before the operand they apply to (`prior l.qty`): the name after one is that operand, not an alias. */
private val PREFIX_OPERATORS = setOf("PRIOR", "CONNECT_BY_ROOT", "NOT", "NEW")

/** The fields of an interval literal's qualifier (`interval '1' day`): after a literal, one of them ends the literal. */
private val INTERVAL_FIELDS = setOf("YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND")

private val LITERAL_TYPES = setOf(
    PlSqlTokenType.STRING_LITERAL,
    PlSqlTokenType.INTEGER_LITERAL,
    PlSqlTokenType.NUMBER_LITERAL,
    PlSqlTokenType.DATE_LITERAL,
    PlSqlTokenType.TIMESTAMP_LITERAL,
    PlSqlTokenType.TIME_LITERAL
)

/**
 * Value-based, permissive parsers for package types, subtypes, subprogram headings and their parameters. The same
 * instance serves package specifications and package bodies.
 */
internal class PackageMemberParser(
    private val stream: TokenStream,
    private val typeReferences: TypeReferences,
    private val collectionElements: CollectionElementSyntax,
    private val datatypeShape: DatatypeShape,
    private val callSpecifications: CallSpecificationScanner
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
            elementType = collection?.elementType?.takeIf { shape == ProjectTypeShape.COLLECTION },
            collectionKind = collection?.kind?.takeIf { shape == ProjectTypeShape.COLLECTION }
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
            // A call specification (`IS`/`AS` then `LANGUAGE`, `EXTERNAL` or `MLE`) ends the heading; it must be a whole one.
            val callSpecification = stream.firstKeywordAtTopLevel(returnIndex + 1, setOf("AS", "IS"), end)
            if (callSpecification != null && callSpecifications.specificationEnd(callSpecification + 1, end + 1) != end) return null
            val returnTypeEnd = listOfNotNull(modifier, callSpecification).minOrNull() ?: end
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
     * return type must be a strictly valid datatype; the query is bounded by the `;` and only its select list is read, a
     * query whose select list cannot be read giving one item of no name covering the query. Anything else yields null,
     * never a partial fact.
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
        val selectList = when {
            cursor == semicolon -> if (returnType == null) return null else null
            stream.isKeyword(cursor, "IS") -> {
                if (cursor + 1 >= semicolon) return null
                selectList(cursor + 1, semicolon) ?: SelectList(listOf(SelectItem(null, stream.range(cursor + 1, semicolon - 1))))
            }
            else -> return null
        }
        val declaration = PackageCursorDeclaration(
            owner, stream.identifier(stream.tokens[nameIndex]), parameters, returnType, selectList, stream.fileId,
            stream.range(index, semicolon)
        )
        return ParsedDeclaration(declaration, semicolon + 1)
    }

    /**
     * The select list of the query in [start, endExclusive): of its main query block after a `WITH` clause, of its first
     * branch in a compound query (Oracle names the fields from it), inside any enclosing parentheses. Null when the list
     * cannot be read: no `SELECT` where expected, an empty item or unbalanced delimiters.
     */
    private fun selectList(start: Int, endExclusive: Int): SelectList? {
        var index = start
        while (index < endExclusive && stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS)) index++
        if (stream.isKeyword(index, "WITH")) index = stream.firstKeywordAtTopLevel(index + 1, setOf("SELECT"), endExclusive) ?: return null
        if (index >= endExclusive || !stream.isKeyword(index, "SELECT")) return null
        index++
        if (stream.isKeyword(index, "DISTINCT") || stream.isKeyword(index, "UNIQUE") || stream.isKeyword(index, "ALL")) index++
        val items = mutableListOf<SelectItem>()
        val expected = ArrayDeque<PlSqlPunctuator>()
        var itemStart = index
        while (true) {
            val ends = index >= endExclusive ||
                expected.isEmpty() && (stream.isCloser(index) || SELECT_LIST_TERMINATORS.any { stream.isKeyword(index, it) } && !stream.isKeyword(index - 1, "MULTISET"))
            if (ends || expected.isEmpty() && stream.isPunctuator(index, PlSqlPunctuator.COMMA)) {
                items += selectItem(itemStart, index) ?: return null
                if (ends) break
                itemStart = index + 1
            } else if (stream.isOpener(index)) {
                expected.addLast(stream.closerFor(index))
            } else if (stream.isCloser(index) && expected.removeLastOrNull() !== stream.tokens[index].type) {
                return null
            }
            index++
        }
        return if (expected.isEmpty()) SelectList(items) else null
    }

    /**
     * The field name of a select item, only when the tokens leave no doubt: a column written `name(.name)*` gives its
     * last name; an expression followed by `AS name` gives that name; an expression that surely ends (a literal, `)`,
     * `END`, a value name) followed by a name gives that name, unless the name may still belong to the expression (an
     * interval qualifier after a literal: `interval '1' day`). Anything else is an item of no known name: a keyword
     * written after the expression may belong to it (`l.created at local`, `l.name collate binary_ci`,
     * `l.v multiset union l.w`), and Oracle then names the item by its text.
     */
    private fun selectItem(start: Int, endExclusive: Int): SelectItem? {
        if (start >= endExclusive) return null
        val last = endExclusive - 1
        val whole = stream.range(start, last)
        if (stream.isPunctuator(last, PlSqlPunctuator.MULTIPLICATION) && (start == last || stream.isPunctuator(last - 1, PlSqlPunctuator.DOT))) {
            return SelectItem(null, whole)
        }
        if (isColumn(start, last)) return named(last)
        val before = last - 1
        if (start < before && stream.isKeyword(before, "AS") && stream.isName(stream.tokens[last])) return named(last)
        val qualifier = stream.tokens[before].type in LITERAL_TYPES && stream.tokens[last].type != GenericTokenType.IDENTIFIER && stream.valueAt(last) in INTERVAL_FIELDS
        if (start <= before && stream.isName(stream.tokens[last]) && !qualifier && endsExpression(start, before)) return named(last)
        return SelectItem(null, whole)
    }

    private fun isColumn(start: Int, last: Int) = (last - start) % 2 == 0 && (start..last).all {
        if ((it - start) % 2 == 1) stream.isPunctuator(it, PlSqlPunctuator.DOT) else stream.isName(stream.tokens[it])
    }

    /**
     * Whether the expression in [start, before] surely ends at [before]: a literal, `)`, `END`, a word that is no keyword,
     * a name written after a `.`, or a whole item of one keyword that is a value (`rownum`, `sysdate`). A keyword written
     * after other tokens (`at`, `collate`, `zone`) may take more of the item.
     */
    private fun endsExpression(start: Int, before: Int): Boolean {
        val token = stream.tokens[before]
        return when {
            token.type in LITERAL_TYPES || stream.isPunctuator(before, PlSqlPunctuator.RPARENTHESIS) || stream.isKeyword(before, "END") -> true
            token.type == GenericTokenType.IDENTIFIER -> true
            !stream.isName(token) -> false
            before > start && stream.isPunctuator(before - 1, PlSqlPunctuator.DOT) -> true
            else -> before == start && stream.valueAt(before) !in PREFIX_OPERATORS
        }
    }

    private fun named(index: Int) = SelectItem(stream.identifier(stream.tokens[index]), stream.range(index, index))

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
