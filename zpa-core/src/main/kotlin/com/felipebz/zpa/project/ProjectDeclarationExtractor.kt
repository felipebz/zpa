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
package com.felipebz.zpa.project

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets
import java.util.*

/** Words that may follow a datatype name in the strict standalone datatype check. */
private val DATATYPE_CONTINUATIONS = setOf(
    "CHAR", "BYTE", "VARYING", "PRECISION", "RAW", "WITH", "LOCAL", "TIME", "ZONE", "TO",
    "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "CHARACTER", "NATIONAL"
)

private val EXTERNAL_PARAMETER_PROPERTIES = listOf("LENGTH", "DURATION", "MAXLEN", "CHARSETID", "CHARSETFORM")

/** Standalone-only return-type terminators (the package set lacks AGGREGATE, ORDER and CLUSTER). */
private val STANDALONE_RETURN_TYPE_TERMINATORS = listOf(
    "DETERMINISTIC", "PIPELINED", "SQL_MACRO", "PARALLEL_ENABLE", "ORDER", "CLUSTER", "SHARD_ENABLE",
    "RESULT_CACHE", "AUTHID", "DEFAULT", "ACCESSIBLE", "AGGREGATE", "IS", "AS"
)

/** Sentinel returned by [packageHeaderEnd] for a wrapped unit. */
private const val WRAPPED_UNIT = -2

private val UNIT_KINDS = listOf("FUNCTION", "PROCEDURE", "PACKAGE", "TRIGGER", "TYPE")

private val CALL_SPECIFICATION_WORDS = setOf("LANGUAGE", "EXTERNAL", "MLE")

private val FUNCTION_RETURN_TYPE_TERMINATORS = setOf(
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
            val orReplace = valueAt(index) == "OR" && valueAt(index + 1) == "REPLACE"
            if (orReplace) index += 2
            if (valueAt(index) == "EDITIONABLE" || valueAt(index) == "NONEDITIONABLE") index++
            return when (valueAt(index)) {
                "PACKAGE" -> parsePackage(create, index, orReplace)
                // The strict standalone path re-validates the whole prefix with token types.
                "PROCEDURE" -> parseStandaloneCallable(create, function = false)
                "FUNCTION" -> parseStandaloneCallable(create, function = true)
                "SEQUENCE" -> parseSequence(create, index)
                "TYPE" -> if (valueAt(index + 1) == "BODY") parseTypeBody(index) else parseStandaloneType(create, index)
                else -> null
            }
        }

        // ---- Strict standalone callable path. It never uses the value-based helpers for
        // structural decisions: quoted identifiers expose unquoted values that look like keywords
        // or punctuators, so keywords are checked with isKeyword and punctuators with isPunctuator.
        // Any doubt yields null (no fact, nothing consumed): incomplete coverage beats a wrong fact.

        /** Sorted indices of unit boundaries: real CREATE keywords, lone-line `/` and EOF. Built once. */
        private val unitBoundaries: IntArray by lazy {
            tokens.indices.filter {
                isKeyword(it, "CREATE") || isLoneSlash(it) || tokens[it].type == GenericTokenType.EOF
            }.toIntArray()
        }

        private fun isLoneSlash(index: Int): Boolean {
            if (!isPunctuator(index, PlSqlPunctuator.DIVISION)) return false
            val token = tokens[index]
            val previousOnEarlierLine = index == 0 || tokens[index - 1].endLine < token.line
            val next = tokens.getOrNull(index + 1)
            val nextOnLaterLine = next == null || next.type == GenericTokenType.EOF || next.line > token.endLine
            return previousOnEarlierLine && nextOnLaterLine
        }

        /** First boundary strictly after [create]; scans of that unit never reach it. */
        private fun unitLimit(create: Int): Int {
            var position = Arrays.binarySearch(unitBoundaries, create + 1)
            if (position < 0) position = -position - 1
            return if (position < unitBoundaries.size) unitBoundaries[position] else tokens.size
        }

        private fun isPunctuator(index: Int, punctuator: PlSqlPunctuator): Boolean =
            index in tokens.indices && tokens[index].type === punctuator

        private fun tokenTypeAt(index: Int) = if (index in tokens.indices) tokens[index].type else null

        private fun isNameBefore(index: Int, limit: Int) = index < limit && index in tokens.indices && isName(tokens[index])

        private fun parseStandaloneCallable(create: Int, function: Boolean): ParsedUnit? {
            if (!isKeyword(create, "CREATE")) return null
            val limit = unitLimit(create)
            var index = create + 1
            val orReplace = isKeyword(index, "OR") && isKeyword(index + 1, "REPLACE")
            if (orReplace) index += 2
            if (isKeyword(index, "EDITIONABLE") || isKeyword(index, "NONEDITIONABLE")) index++
            if (!isKeyword(index, if (function) "FUNCTION" else "PROCEDURE")) return null
            index++
            if (isKeyword(index, "IF")) {
                // IF NOT EXISTS cannot be combined with OR REPLACE (ORA-11541).
                if (orReplace || !isKeyword(index + 1, "NOT") || !isKeyword(index + 2, "EXISTS")) return null
                index += 3
            }
            val name = strictQualifiedName(index, limit) ?: return null
            index = skipUnitOptions(name.second, limit) ?: return null
            if (isKeyword(index, "WRAPPED")) {
                // The lexer makes the whole payload one opaque WRAPPED_SOURCE token. No signature
                // can be recovered, so the unit is skipped without a fact.
                return if (index + 1 < limit && tokenTypeAt(index + 1) === PlSqlTokenType.WRAPPED_SOURCE) {
                    ParsedUnit(emptyList(), index + 2)
                } else {
                    null
                }
            }
            val parameters = when (val parsed = parseParametersStrict(index, limit)) {
                StrictParameters.Absent -> emptyList()
                StrictParameters.Failed -> return null
                is StrictParameters.Parsed -> {
                    index = parsed.next
                    parsed.parameters
                }
            }
            if (!function) {
                index = skipPropertiesStrict(index, limit, function = false)?.next ?: return null
                if (!isKeyword(index, "IS") && !isKeyword(index, "AS")) return null
                val end = strictImplementationEnd(index + 1, limit) ?: return null
                return ParsedUnit(
                    listOf(StandaloneProcedureDeclaration(name.first, parameters, fileId, range(create, end))),
                    end + 1
                )
            }

            // Function: RETURN datatype, properties, then an implementation or a terminal `;`.
            if (!isKeyword(index, "RETURN")) return null
            val typeStart = index + 1
            val typeEnd = returnTypeEnd(typeStart, limit) ?: return null
            if (!strictDatatype(typeStart, typeEnd)) return null
            val returnType = typeReference(typeStart, typeEnd) ?: return null
            val properties = skipPropertiesStrict(typeEnd, limit, function = true) ?: return null
            index = properties.next
            val end = when {
                properties.terminal -> index
                isKeyword(index, "IS") || isKeyword(index, "AS") -> strictImplementationEnd(index + 1, limit) ?: return null
                isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> index // body-less declaration
                else -> return null
            }
            if (end >= limit || !isPunctuator(end, PlSqlPunctuator.SEMICOLON)) return null
            return ParsedUnit(
                listOf(
                    StandaloneFunctionDeclaration(
                        name.first, parameters, returnType, fileId, range(create, end),
                        deterministic = properties.deterministic
                    )
                ),
                end + 1
            )
        }

        /**
         * End (exclusive) of the return datatype: the first depth-0 token that is a real keyword in
         * [STANDALONE_RETURN_TYPE_TERMINATORS] or a real `;`. Balanced parentheses are skipped; any other
         * delimiter, a missing terminator before [limit] or an empty slice yields null.
         */
        private fun returnTypeEnd(start: Int, limit: Int): Int? {
            var index = start
            while (index < limit) {
                when {
                    isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> index = (strictClose(index, limit) ?: return null) + 1
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) ||
                        STANDALONE_RETURN_TYPE_TERMINATORS.any { isKeyword(index, it) } -> return index.takeIf { it > start }
                    isOpener(index) || isCloser(index) -> return null
                    else -> index++
                }
            }
            return null
        }

        /** One or two name segments joined by a real `.`; a third segment is a failure. */
        private fun strictQualifiedName(start: Int, limit: Int): Pair<QualifiedName, Int>? {
            if (!isNameBefore(start, limit)) return null
            val segments = mutableListOf(identifier(tokens[start]))
            var index = start + 1
            if (isPunctuator(index, PlSqlPunctuator.DOT)) {
                if (!isNameBefore(index + 1, limit)) return null
                segments += identifier(tokens[index + 1])
                index += 2
                if (isPunctuator(index, PlSqlPunctuator.DOT)) return null
            }
            return QualifiedName(segments) to index
        }

        /** `[TIMESTAMP 'literal'] [SHARING = METADATA|NONE]`; returns the next index or null. */
        private fun skipUnitOptions(start: Int, limit: Int): Int? {
            var index = start
            if (isKeyword(index, "TIMESTAMP")) {
                if (tokenTypeAt(index + 1) !== PlSqlTokenType.STRING_LITERAL) return null
                index += 2
            } else if (tokenTypeAt(index) === PlSqlTokenType.TIMESTAMP_LITERAL) {
                index++
            }
            if (isKeyword(index, "SHARING")) {
                if (!isPunctuator(index + 1, PlSqlPunctuator.EQUALS)) return null
                if (!isKeyword(index + 2, "METADATA") && !isKeyword(index + 2, "NONE")) return null
                index += 3
            }
            return index.takeIf { it < limit }
        }

        private fun isOpener(index: Int) =
            isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) || isPunctuator(index, PlSqlPunctuator.LBRACKET)

        private fun isCloser(index: Int) =
            isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) || isPunctuator(index, PlSqlPunctuator.RBRACKET)

        private fun closerFor(opener: Int) =
            if (isPunctuator(opener, PlSqlPunctuator.LPARENTHESIS)) PlSqlPunctuator.RPARENTHESIS else PlSqlPunctuator.RBRACKET

        /**
         * Index of the `)` matching the `(` at [open], or null when nesting is unbalanced or crossed
         * (`(` must close with `)`, `[` with `]`) before [limit], or a `;`/CREATE appears inside.
         * Never falls back to the end of the file.
         */
        private fun strictClose(open: Int, limit: Int): Int? {
            if (!isPunctuator(open, PlSqlPunctuator.LPARENTHESIS)) return null
            val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
            for (index in open until limit.coerceAtMost(tokens.size)) {
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> {
                        if (expected.removeLastOrNull() !== tokens[index].type) return null
                        if (expected.isEmpty()) return index
                    }
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) || isKeyword(index, "CREATE") -> return null
                }
            }
            return null
        }

        /**
         * Depth-0 comma-separated parts of `(open, close)`; null for `()`, any empty part, or any
         * crossed or unmatched delimiter (nesting is re-validated, not assumed from [close]).
         */
        private fun splitStrict(open: Int, close: Int): List<IntRange>? {
            if (close == open + 1 || strictClose(open, close + 1) != close) return null
            val parts = mutableListOf<IntRange>()
            val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
            var partStart = open + 1
            for (index in open + 1..close) {
                if (index == close || (expected.isEmpty() && isPunctuator(index, PlSqlPunctuator.COMMA))) {
                    if (partStart >= index) return null
                    parts += partStart until index
                    partStart = index + 1
                    continue
                }
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return null
                }
            }
            return parts.takeIf { expected.isEmpty() }
        }

        private sealed interface StrictParameters {
            /** No parameter list: a valid empty signature. */
            object Absent : StrictParameters
            /** Every part parsed; ordinals are 1..n in declaration order. */
            class Parsed(val parameters: List<ProjectParameter>, val next: Int) : StrictParameters
            /** Any part or the list structure is invalid: the whole unit must be rejected. */
            object Failed : StrictParameters
        }

        private fun parseParametersStrict(start: Int, limit: Int): StrictParameters {
            if (!isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return StrictParameters.Absent
            val close = strictClose(start, limit) ?: return StrictParameters.Failed
            val parts = splitStrict(start, close) ?: return StrictParameters.Failed
            val parameters = parts.mapIndexed { position, part ->
                parseParameterStrict(part.first, part.last + 1, position + 1) ?: return StrictParameters.Failed
            }
            return StrictParameters.Parsed(immutableList(parameters), close + 1)
        }

        /** `name [IN] (OUT [NOCOPY] datatype | datatype [(DEFAULT|:=) expression])`, fully consumed. */
        private fun parseParameterStrict(start: Int, endExclusive: Int, ordinal: Int): ProjectParameter? {
            if (!isNameBefore(start, endExclusive)) return null
            var cursor = start + 1
            var mode = ParameterMode.IN
            if (isKeyword(cursor, "IN")) cursor++
            var nocopy = false
            if (isKeyword(cursor, "OUT")) {
                mode = if (mode == ParameterMode.IN && isKeyword(cursor - 1, "IN")) ParameterMode.IN_OUT else ParameterMode.OUT
                cursor++
                if (isKeyword(cursor, "NOCOPY")) {
                    nocopy = true
                    cursor++
                }
            } else if (isKeyword(cursor, "NOCOPY")) {
                return null
            }
            var defaultIndex: Int? = null
            var depth = 0
            for (index in cursor until endExclusive) {
                when {
                    isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> depth++
                    isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) -> depth--
                    depth == 0 && (isKeyword(index, "DEFAULT") || isPunctuator(index, PlSqlPunctuator.ASSIGNMENT)) -> {
                        defaultIndex = index
                        break
                    }
                }
            }
            if (defaultIndex != null && (mode != ParameterMode.IN || defaultIndex + 1 >= endExclusive)) return null
            val typeEnd = defaultIndex ?: endExclusive
            if (!strictDatatype(cursor, typeEnd)) return null
            val type = typeReference(cursor, typeEnd) ?: return null
            return ProjectParameter(name = identifier(tokens[start]), ordinal = ordinal, mode = mode, nocopy = nocopy,
                typeRef = type, defaultPresent = defaultIndex != null, sourceRange = range(start, endExclusive - 1))
        }

        /**
         * Conservative datatype shape: `name[.name…] [%TYPE|%ROWTYPE]`, `REF name[.name]`, or a
         * name followed only by balanced groups and words from [DATATYPE_CONTINUATIONS]. Anything
         * else (an extra free identifier, punctuation, unknown words) is rejected: no fact.
         */
        private fun strictDatatype(start: Int, endExclusive: Int): Boolean {
            if (start >= endExclusive) return false
            var index = start
            val ref = isKeyword(index, "REF")
            if (ref) index++
            if (!isNameBefore(index, endExclusive)) return false
            index++
            while (isPunctuator(index, PlSqlPunctuator.DOT) && isNameBefore(index + 1, endExclusive)) index += 2
            if (index == endExclusive) return true
            if (ref) return false
            if (isPunctuator(index, PlSqlPunctuator.MOD)) {
                return index + 2 == endExclusive && (isKeyword(index + 1, "TYPE") || isKeyword(index + 1, "ROWTYPE"))
            }
            while (index < endExclusive) {
                index = when {
                    isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> {
                        val close = strictClose(index, endExclusive) ?: return false
                        close + 1
                    }
                    tokens[index].type is PlSqlKeyword && valueAt(index) in DATATYPE_CONTINUATIONS -> index + 1
                    else -> return false
                }
            }
            return true
        }

        private class PropertyScan(val next: Int, val deterministic: Boolean, val terminal: Boolean)

        /**
         * Skips subprogram properties (grammar `subprogramProperty`, any order). Each clause's
         * mandatory structure is verified; anything unknown or malformed yields null. For a
         * [function], `PIPELINED ... USING ref ;` and `AGGREGATE USING ref ;` are terminal forms
         * (`next` is then the real `;`); a procedure cannot end this way, so they are rejected.
         */
        private fun skipPropertiesStrict(start: Int, limit: Int, function: Boolean): PropertyScan? {
            var index = start
            var deterministic = false
            while (index < limit) {
                index = when {
                    isKeyword(index, "DETERMINISTIC") -> {
                        deterministic = true
                        index + 1
                    }
                    isKeyword(index, "SHARD_ENABLE") -> index + 1
                    isKeyword(index, "PIPELINED") -> {
                        var next = index + 1
                        if ((isKeyword(next, "ROW") || isKeyword(next, "TABLE")) && isKeyword(next + 1, "POLYMORPHIC")) next += 2
                        if (isKeyword(next, "USING")) {
                            if (!function) return null
                            return terminalUsing(next, limit, deterministic)
                        }
                        next
                    }
                    isKeyword(index, "AGGREGATE") -> {
                        if (!function || !isKeyword(index + 1, "USING")) return null
                        return terminalUsing(index + 1, limit, deterministic)
                    }
                    isKeyword(index, "SQL_MACRO") -> sqlMacroEnd(index + 1, limit) ?: return null
                    isKeyword(index, "PARALLEL_ENABLE") -> parallelEnableEnd(index + 1, limit) ?: return null
                    isKeyword(index, "ORDER") || isKeyword(index, "CLUSTER") -> streamingEnd(index, limit) ?: return null
                    isKeyword(index, "RESULT_CACHE") ->
                        if (isKeyword(index + 1, "RELIES_ON")) {
                            delimitedListEnd(index + 2, limit) { strictQualifiedNameAnyLength(it, limit) } ?: return null
                        } else {
                            index + 1
                        }
                    isKeyword(index, "AUTHID") ->
                        if (isKeyword(index + 1, "CURRENT_USER") || isKeyword(index + 1, "DEFINER")) index + 2 else return null
                    isKeyword(index, "DEFAULT") ->
                        if (isKeyword(index + 1, "COLLATION") && isKeyword(index + 2, "USING_NLS_COMP")) index + 3 else return null
                    isKeyword(index, "ACCESSIBLE") ->
                        if (isKeyword(index + 1, "BY")) delimitedListEnd(index + 2, limit, ::accessorEnd) ?: return null else return null
                    else -> return PropertyScan(index, deterministic, false)
                }
            }
            return null
        }

        /** `USING object_reference ;` at [using]; the reference is a conservative name path. */
        private fun terminalUsing(using: Int, limit: Int, deterministic: Boolean): PropertyScan? {
            val end = strictQualifiedNameAnyLength(using + 1, limit) ?: return null
            return if (end < limit && isPunctuator(end, PlSqlPunctuator.SEMICOLON)) PropertyScan(end, deterministic, true) else null
        }

        /**
         * `( item {, item} )` starting at [open], where [item] returns the index after one item or
         * null. Requires at least one item, real commas and a real closing parenthesis.
         */
        private inline fun delimitedListEnd(open: Int, limit: Int, item: (Int) -> Int?): Int? {
            if (!isPunctuator(open, PlSqlPunctuator.LPARENTHESIS)) return null
            var index = open + 1
            while (true) {
                index = item(index) ?: return null
                if (index >= limit) return null
                if (isPunctuator(index, PlSqlPunctuator.RPARENTHESIS)) return index + 1
                if (!isPunctuator(index, PlSqlPunctuator.COMMA)) return null
                index++
            }
        }

        private fun nameEnd(index: Int, limit: Int): Int? = if (isNameBefore(index, limit)) index + 1 else null

        /** `name {. name}` of any length (object references in RELIES_ON). */
        private fun strictQualifiedNameAnyLength(start: Int, limit: Int): Int? {
            var index = nameEnd(start, limit) ?: return null
            while (isPunctuator(index, PlSqlPunctuator.DOT)) index = nameEnd(index + 1, limit) ?: return null
            return index
        }

        /** Accessor of ACCESSIBLE BY: `[FUNCTION|PROCEDURE|PACKAGE|TRIGGER|TYPE] unit_name`. */
        private fun accessorEnd(start: Int): Int? {
            val limit = tokens.size
            val index = if (UNIT_KINDS.any { isKeyword(start, it) }) start + 1 else start
            return strictQualifiedName(index, limit)?.second
        }

        /** `[( [TYPE =>] (TABLE|SCALAR) )]`. */
        private fun sqlMacroEnd(start: Int, limit: Int): Int? {
            if (!isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return start
            var index = start + 1
            if (isKeyword(index, "TYPE")) {
                if (!isPunctuator(index + 1, PlSqlPunctuator.ASSOCIATION)) return null
                index += 2
            }
            if (!isKeyword(index, "TABLE") && !isKeyword(index, "SCALAR")) return null
            return if (index + 1 < limit && isPunctuator(index + 1, PlSqlPunctuator.RPARENTHESIS)) index + 2 else null
        }

        /**
         * `[( PARTITION name BY (ANY | (HASH|RANGE|VALUE) (name {, name})) [streaming] )]`.
         */
        private fun parallelEnableEnd(start: Int, limit: Int): Int? {
            if (!isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return start
            if (!isKeyword(start + 1, "PARTITION") || nameEnd(start + 2, limit) == null || !isKeyword(start + 3, "BY")) return null
            var index = start + 4
            index = when {
                isKeyword(index, "ANY") -> index + 1
                isKeyword(index, "HASH") || isKeyword(index, "RANGE") || isKeyword(index, "VALUE") ->
                    delimitedListEnd(index + 1, limit) { nameEnd(it, limit) } ?: return null
                else -> return null
            }
            if (isKeyword(index, "ORDER") || isKeyword(index, "CLUSTER")) index = streamingEnd(index, limit) ?: return null
            return if (index < limit && isPunctuator(index, PlSqlPunctuator.RPARENTHESIS)) index + 1 else null
        }

        /** `(ORDER|CLUSTER) name BY ( name {, name} )`. */
        private fun streamingEnd(start: Int, limit: Int): Int? {
            if (nameEnd(start + 1, limit) == null || !isKeyword(start + 2, "BY")) return null
            return delimitedListEnd(start + 3, limit) { nameEnd(it, limit) }
        }

        /** End (a real `;` below [limit]) of the body or call specification starting at [start]. */
        private fun strictImplementationEnd(start: Int, limit: Int): Int? {
            if (start >= limit) return null
            val end = when {
                isKeyword(start, "LANGUAGE") && isKeyword(start + 1, "JAVA") ->
                    if (isKeyword(start + 2, "NAME") && tokenTypeAt(start + 3) === PlSqlTokenType.STRING_LITERAL) start + 4 else return null
                isKeyword(start, "LANGUAGE") || isKeyword(start, "EXTERNAL") -> cSpecificationEnd(start, limit) ?: return null
                isKeyword(start, "MLE") -> mleSpecificationEnd(start + 1, limit) ?: return null
                // A quoted identifier spelled like a call-specification keyword is not one.
                tokens[start].type == GenericTokenType.IDENTIFIER && valueAt(start) in CALL_SPECIFICATION_WORDS -> return null
                else -> implementationEnd(start, limit) ?: return null
            }
            return end.takeIf { it < limit && isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
        }

        /**
         * `(LANGUAGE C | EXTERNAL) [NAME id] LIBRARY id [NAME id] [AGENT IN (id {, id})]
         * [WITH CONTEXT] [PARAMETERS (param {, param})]`, returning the index of the `;`.
         * `C` is lexed as an identifier; a quoted `"C"` is not accepted. External parameters are
         * validated structurally against the grammar (see [externalParameterEnd]), not accepted as arbitrary words.
         */
        private fun cSpecificationEnd(start: Int, limit: Int): Int? {
            var index = when {
                isKeyword(start, "EXTERNAL") -> start + 1
                isKeyword(start, "LANGUAGE") && isUnquotedWord(start + 1, "C") -> start + 2
                else -> return null
            }
            if (isKeyword(index, "NAME")) index = nameEnd(index + 1, limit) ?: return null
            if (!isKeyword(index, "LIBRARY")) return null
            index = nameEnd(index + 1, limit) ?: return null
            if (isKeyword(index, "NAME")) index = nameEnd(index + 1, limit) ?: return null
            if (isKeyword(index, "AGENT")) {
                if (!isKeyword(index + 1, "IN")) return null
                index = delimitedListEnd(index + 2, limit) { nameEnd(it, limit) } ?: return null
            }
            if (isKeyword(index, "WITH")) {
                if (!isKeyword(index + 1, "CONTEXT")) return null
                index += 2
            }
            if (isKeyword(index, "PARAMETERS")) index = delimitedListEnd(index + 1, limit) { externalParameterEnd(it, limit) } ?: return null
            return index.takeIf { it < limit && isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
        }

        /**
         * One `EXTERNAL_PARAMETER`, following the grammar's ordered alternatives:
         * `CONTEXT` | `SELF (TDO | property)` | `(RETURN | name) [property] [BY REFERENCE] [datatype]`.
         * Returns the index after the parameter; the caller requires a real `,` or `)` there.
         * The optional datatype must pass [strictDatatype]; anything else rejects the unit.
         */
        private fun externalParameterEnd(start: Int, limit: Int): Int? {
            if (start >= limit) return null
            // CONTEXT is a complete parameter; if anything else follows it is an ordinary (unreserved)
            // name, as the grammar's third alternative allows (e.g. `CONTEXT x`).
            if (isKeyword(start, "CONTEXT") && (isPunctuator(start + 1, PlSqlPunctuator.COMMA) ||
                    isPunctuator(start + 1, PlSqlPunctuator.RPARENTHESIS))) return start + 1
            if (isKeyword(start, "SELF")) {
                // `SELF (TDO | property)` must end the parameter; otherwise SELF is an ordinary name.
                val selfEnd = if (isKeyword(start + 1, "TDO")) start + 2 else externalParameterPropertyEnd(start + 1)
                if (selfEnd != null && (isPunctuator(selfEnd, PlSqlPunctuator.COMMA) ||
                        isPunctuator(selfEnd, PlSqlPunctuator.RPARENTHESIS))) return selfEnd
            }
            if (!isKeyword(start, "RETURN") && !isNameBefore(start, limit)) return null
            var index = start + 1
            index = externalParameterPropertyEnd(index) ?: index
            if (isKeyword(index, "BY")) {
                if (!isKeyword(index + 1, "REFERENCE")) return null
                index += 2
            }
            val datatypeEnd = externalDatatypeEnd(index, limit) ?: return null
            if (datatypeEnd > index && !strictDatatype(index, datatypeEnd)) return null
            return datatypeEnd
        }

        /** `INDICATOR [STRUCT | TDO]` | `LENGTH` | `DURATION` | `MAXLEN` | `CHARSETID` | `CHARSETFORM`. */
        private fun externalParameterPropertyEnd(index: Int): Int? = when {
            isKeyword(index, "INDICATOR") ->
                if (isKeyword(index + 1, "STRUCT") || isKeyword(index + 1, "TDO")) index + 2 else index + 1
            EXTERNAL_PARAMETER_PROPERTIES.any { isKeyword(index, it) } -> index + 1
            else -> null
        }

        /** Index of the depth-0 `,` or `)` ending a datatype slice that starts at [start]. */
        private fun externalDatatypeEnd(start: Int, limit: Int): Int? {
            var index = start
            while (index < limit) {
                when {
                    isPunctuator(index, PlSqlPunctuator.COMMA) || isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) -> return index
                    isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> index = (strictClose(index, limit) ?: return null) + 1
                    isCloser(index) || isOpener(index) || isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return null
                    else -> index++
                }
            }
            return null
        }

        private fun isUnquotedWord(index: Int, word: String): Boolean =
            index in tokens.indices && tokens[index].type == GenericTokenType.IDENTIFIER &&
                tokens[index].originalValue.equals(word, ignoreCase = true)

        /** `LANGUAGE name[.name] [PURE] <inline source>` or `MODULE name[.name] [ENV name[.name]] SIGNATURE 'str'`. */
        private fun mleSpecificationEnd(start: Int, limit: Int): Int? {
            if (isKeyword(start, "LANGUAGE")) {
                var index = strictQualifiedName(start + 1, limit)?.second ?: return null
                if (isKeyword(index, "PURE")) index++
                return if (tokenTypeAt(index) === PlSqlTokenType.MLE_INLINE_SOURCE) index + 1 else null
            }
            if (!isKeyword(start, "MODULE")) return null
            var index = strictQualifiedName(start + 1, limit)?.second ?: return null
            if (isKeyword(index, "ENV")) index = strictQualifiedName(index + 1, limit)?.second ?: return null
            if (!isKeyword(index, "SIGNATURE") || tokenTypeAt(index + 1) !== PlSqlTokenType.STRING_LITERAL) return null
            return index + 2
        }

        private fun parseSequence(create: Int, sequenceIndex: Int): ParsedUnit? {
            // CREATE SEQUENCE has no OR REPLACE or EDITIONABLE prefix in the supported
            // grammar. Keep extraction aligned with that grammar even though parseCreate
            // handles those prefixes for other schema objects.
            if (sequenceIndex != create + 1) return null

            val ifNotExists = valueAt(sequenceIndex + 1) == "IF" &&
                valueAt(sequenceIndex + 2) == "NOT" && valueAt(sequenceIndex + 3) == "EXISTS"
            val nameIndex = if (ifNotExists) sequenceIndex + 4 else sequenceIndex + 1
            val parsedName = qualifiedName(nameIndex) ?: return null
            if (parsedName.first.segments.size !in 1..2) return null

            val end = semicolonAfter(parsedName.second)
            return ParsedUnit(
                listOf(SequenceDeclaration(parsedName.first, fileId, range(create, end))),
                end + 1
            )
        }

        private fun parsePackage(create: Int, packageIndex: Int, orReplace: Boolean): ParsedUnit? {
            // The shared dispatch is value based; require real CREATE/PACKAGE tokens (and a prefix that
            // validates with token types) so quoted lookalikes never start a package unit.
            if (strictPackagePrefix(create) != (packageIndex to orReplace)) return null
            // `BODY` must be the keyword: `CREATE PACKAGE "BODY" AS ...` is a specification named "BODY".
            val body = isKeyword(packageIndex + 1, "BODY")
            val kindEnd = if (body) packageIndex + 2 else packageIndex + 1
            // IF NOT EXISTS follows PACKAGE [BODY] and cannot be combined with OR REPLACE (ORA-11541).
            val ifNotExists = if (body) {
                valueAt(kindEnd) == "IF" && valueAt(kindEnd + 1) == "NOT" && valueAt(kindEnd + 2) == "EXISTS"
            } else {
                isKeyword(kindEnd, "IF") && isKeyword(kindEnd + 1, "NOT") && isKeyword(kindEnd + 2, "EXISTS")
            }
            if (ifNotExists && orReplace) return null
            val nameIndex = if (ifNotExists) kindEnd + 3 else kindEnd
            if (!body) return parsePackageSpecification(create, nameIndex)
            val name = qualifiedName(nameIndex) ?: return null
            return parsePackageBody(name.first, name.second)
        }

        /** `CREATE [OR REPLACE] [EDITIONABLE|NONEDITIONABLE] PACKAGE` validated with token types. */
        private fun strictPackagePrefix(create: Int): Pair<Int, Boolean>? {
            if (!isKeyword(create, "CREATE")) return null
            var index = create + 1
            val orReplace = isKeyword(index, "OR") && isKeyword(index + 1, "REPLACE")
            if (orReplace) index += 2
            if (isKeyword(index, "EDITIONABLE") || isKeyword(index, "NONEDITIONABLE")) index++
            return if (isKeyword(index, "PACKAGE")) index to orReplace else null
        }

        /**
         * Package specification, bounded by the unit limit and validated with token types.
         *
         * The end of the specification is found by *walking declarations*, not by searching for an
         * `END`: every declaration ends at its first real `;`, and a real `END` keyword (reserved, so
         * never an identifier) is the terminator only where a declaration would start. A `CASE ... END`
         * inside an initializer, a quoted `"END"` or a variable named like a keyword therefore cannot
         * end the package. A specification that cannot be walked to its own `END [name] ;` before the
         * unit limit yields no fact at all (never a range to EOF, never the next unit's members).
         */
        private fun parsePackageSpecification(create: Int, nameIndex: Int): ParsedUnit? {
            val limit = unitLimit(create)
            val name = strictQualifiedName(nameIndex, limit) ?: return null
            val owner = name.first
            val header = packageHeaderEnd(name.second, limit)
            if (header == WRAPPED_UNIT) {
                // One opaque WRAPPED_SOURCE token: nothing can be recovered, and nothing after it is consumed.
                val wrapped = wrappedPayloadEnd(name.second, limit) ?: return null
                return ParsedUnit(emptyList(), wrapped + 1)
            }
            if (header < 0) return null

            val members = mutableListOf<ProjectDeclaration>()
            var cursor = header + 1
            while (true) {
                if (cursor >= limit) return null
                if (isKeyword(cursor, "END")) {
                    val semicolon = packageTerminatorSemicolon(cursor, limit) ?: return null
                    val declarations = mutableListOf<ProjectDeclaration>()
                    declarations += PackageDeclaration(owner, DeclarationRole.SPECIFICATION, fileId, range(create, semicolon))
                    declarations += members
                    return ParsedUnit(declarations, semicolon + 1)
                }
                val declarationEnd = specificationDeclarationEnd(cursor, limit) ?: return null
                if (declarationEnd.terminated) {
                    parseSpecificationMember(cursor, owner)
                        // Agreement rule: the member parser (legacy scanner) must end exactly where the
                        // strict scanner ends the declaration; any disagreement drops the declaration.
                        ?.takeIf { it.nextIndex == declarationEnd.index + 1 }
                        ?.let { members += it.declaration }
                    cursor = declarationEnd.index + 1
                } else {
                    // Stopped at a reserved PROCEDURE after a missing `;`: the broken declaration yields no
                    // fact and the following subprogram is still visited.
                    cursor = declarationEnd.index
                }
            }
        }

        /**
         * Index of the `IS`/`AS` keyword ending the header, [WRAPPED_UNIT] for a wrapped unit, or -1.
         * Typed delimiters only, bounded by [limit]; a `;` or `END` first means there is no header.
         */
        private fun packageHeaderEnd(start: Int, limit: Int): Int {
            val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
            for (index in start until limit.coerceAtMost(tokens.size)) {
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return -1
                    expected.isNotEmpty() -> Unit
                    isKeyword(index, "WRAPPED") -> return WRAPPED_UNIT
                    isKeyword(index, "IS") || isKeyword(index, "AS") -> return index
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) || isKeyword(index, "END") -> return -1
                }
            }
            return -1
        }

        /** Index of the opaque payload of a wrapped unit (`WRAPPED` immediately followed by `WRAPPED_SOURCE`). */
        private fun wrappedPayloadEnd(start: Int, limit: Int): Int? {
            val wrapped = (start until limit.coerceAtMost(tokens.size)).firstOrNull { isKeyword(it, "WRAPPED") } ?: return null
            return (wrapped + 1).takeIf { it < limit && tokenTypeAt(it) === PlSqlTokenType.WRAPPED_SOURCE }
        }

        /** `;` of `END [name] ;` for the `END` keyword at [end], strictly below [limit]. */
        private fun packageTerminatorSemicolon(end: Int, limit: Int): Int? {
            var index = end + 1
            if (index < limit && isName(tokens[index])) index++
            return index.takeIf { it < limit && isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
        }

        private class DeclarationEnd(val index: Int, val terminated: Boolean)

        /**
         * End of the declaration starting at [start]: its first real `;` at typed delimiter depth 0
         * (`terminated`), or, after a missing `;`, the reserved `PROCEDURE` that starts the next
         * subprogram (not terminated). Null when delimiters are crossed/unbalanced or no `;` exists
         * before [limit]. `FUNCTION` is deliberately not a recovery boundary: it is not reserved and
         * the parser accepts `g NUMBER := function(1);`.
         */
        private fun specificationDeclarationEnd(start: Int, limit: Int): DeclarationEnd? {
            val expected = kotlin.collections.ArrayDeque<PlSqlPunctuator>()
            for (index in start until limit.coerceAtMost(tokens.size)) {
                when {
                    isOpener(index) -> expected.addLast(closerFor(index))
                    isCloser(index) -> if (expected.removeLastOrNull() !== tokens[index].type) return null
                    expected.isNotEmpty() -> Unit
                    isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return DeclarationEnd(index, true)
                    index > start && isKeyword(index, "PROCEDURE") -> return DeclarationEnd(index, false)
                }
            }
            return null
        }

        /**
         * Existing member parsers, selected by structural lookahead on token types. `type NUMBER;`,
         * `"TYPE" NUMBER;` and a variable named `function` do not match any member shape.
         */
        private fun parseSpecificationMember(cursor: Int, owner: QualifiedName): ParsedDeclaration? = when {
            isKeyword(cursor, "TYPE") && isNameBefore(cursor + 1, tokens.size) && isKeyword(cursor + 2, "IS") ->
                parsePackageType(cursor, owner)
            isKeyword(cursor, "SUBTYPE") && isNameBefore(cursor + 1, tokens.size) && isKeyword(cursor + 2, "IS") ->
                parsePackageSubtype(cursor, owner)
            isKeyword(cursor, "PROCEDURE") && isNameBefore(cursor + 1, tokens.size) -> parseSubprogram(cursor, owner)
            isKeyword(cursor, "FUNCTION") && isNameBefore(cursor + 1, tokens.size) &&
                (isPunctuator(cursor + 2, PlSqlPunctuator.LPARENTHESIS) || isKeyword(cursor + 2, "RETURN")) ->
                parseSubprogram(cursor, owner)
            else -> null
        }

        private fun parsePackageBody(owner: QualifiedName, afterName: Int): ParsedUnit? {
            val header = firstAtTopLevel(afterName, setOf("IS", "AS")) ?: return null
            val parsed = parsePackageBodySubprograms(header + 1, owner)
            return ParsedUnit(parsed.declarations, parsed.end + 1)
        }

        private fun parsePackageBodySubprograms(start: Int, owner: QualifiedName): ParsedPackageBody {
            val result = mutableListOf<ProjectDeclaration>()
            var cursor = start
            val blocks = mutableListOf<BlockKind>()
            while (cursor < tokens.size) {
                if (blocks.isEmpty() && isSubprogramKeyword(cursor)) {
                    val parsed = parseBodySubprogram(cursor, tokens.size, owner)
                    if (parsed != null) {
                        parsed.declaration?.let(result::add)
                        cursor = parsed.nextIndex
                        continue
                    }
                }

                when {
                    isCaseStart(cursor) -> blocks += BlockKind.CASE
                    isKeyword(cursor, "BEGIN") -> {
                        if (blocks.isEmpty()) {
                            val end = executableBlockEnd(cursor, tokens.size) ?: tokens.lastIndex
                            return ParsedPackageBody(immutableList(result), end)
                        }
                        blocks += BlockKind.BEGIN
                    }
                    isKeyword(cursor, "END") -> when {
                        isKeyword(cursor + 1, "IF") || isKeyword(cursor + 1, "LOOP") -> Unit
                        blocks.lastOrNull() == BlockKind.CASE -> blocks.removeAt(blocks.lastIndex)
                        blocks.lastOrNull() == BlockKind.BEGIN -> blocks.removeAt(blocks.lastIndex)
                        blocks.isEmpty() -> return ParsedPackageBody(immutableList(result), semicolonAfter(cursor))
                    }
                }
                cursor++
            }
            return ParsedPackageBody(immutableList(result), tokens.lastIndex)
        }

        private fun parseBodySubprogram(index: Int, end: Int, owner: QualifiedName): ParsedBodySubprogram? {
            val function = valueAt(index) == "FUNCTION"
            val name = identifierAt(index + 1) ?: return null
            var cursor = name.second
            val parameters: List<ProjectParameter>
            if (valueAt(cursor) == "(") {
                val close = matching(cursor)
                if (close >= end) return null
                parameters = parseParameters(cursor + 1, close)
                cursor = close + 1
            } else {
                parameters = emptyList()
            }

            val header = firstBodyHeaderDelimiter(cursor, end) ?: return null
            if (header.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
                return ParsedBodySubprogram(null, header.index + 1)
            }

            val implementationEnd = implementationEnd(header.index + 1, end) ?: return null
            val declaration = if (function) {
                val returnIndex = firstAtTopLevel(cursor, setOf("RETURN"), header.index)
                    ?: return null
                val modifier = firstAtTopLevel(
                    returnIndex + 1,
                    FUNCTION_RETURN_TYPE_TERMINATORS,
                    header.index
                )
                val returnType = typeReference(returnIndex + 1, modifier ?: header.index)
                    ?: return null
                PackageFunctionDeclaration(
                    owner,
                    name.first,
                    parameters,
                    returnType,
                    fileId,
                    range(index, implementationEnd),
                    DeclarationRole.BODY
                )
            } else {
                PackageProcedureDeclaration(
                    owner,
                    name.first,
                    parameters,
                    fileId,
                    range(index, implementationEnd),
                    DeclarationRole.BODY
                )
            }
            return ParsedBodySubprogram(declaration, implementationEnd + 1)
        }

        private fun firstBodyHeaderDelimiter(start: Int, end: Int): BodyHeaderDelimiter? {
            var depth = 0
            for (index in start until end.coerceAtMost(tokens.size)) {
                when (valueAt(index)) {
                    "(", "[" -> depth++
                    ")", "]" -> depth--
                }
                if (depth == 0) {
                    when (valueAt(index)) {
                        "IS", "AS" -> return BodyHeaderDelimiter(BodyHeaderDelimiterKind.IMPLEMENTATION, index)
                        ";" -> return BodyHeaderDelimiter(BodyHeaderDelimiterKind.FORWARD_DECLARATION, index)
                    }
                }
            }
            return null
        }

        private fun implementationEnd(start: Int, end: Int): Int? {
            if (valueAt(start) == "LANGUAGE" || valueAt(start) == "EXTERNAL") {
                return semicolonAfter(start).coerceAtMost(end)
            }
            val blocks = mutableListOf<BlockKind>()
            var bodyStarted = false
            var index = start
            while (index < end.coerceAtMost(tokens.size)) {
                if (!bodyStarted && isSubprogramKeyword(index)) {
                    val nestedEnd = bodySubprogramEnd(index, end)
                    if (nestedEnd != null) {
                        index = nestedEnd + 1
                        continue
                    }
                }
                when {
                    isKeyword(index, "BEGIN") -> {
                        bodyStarted = true
                        blocks += BlockKind.BEGIN
                    }
                    isCaseStart(index) -> blocks += BlockKind.CASE
                    isKeyword(index, "END") -> when {
                        isKeyword(index + 1, "IF") || isKeyword(index + 1, "LOOP") -> Unit
                        blocks.lastOrNull() == BlockKind.CASE -> blocks.removeAt(blocks.lastIndex)
                        blocks.lastOrNull() == BlockKind.BEGIN -> {
                            blocks.removeAt(blocks.lastIndex)
                            if (bodyStarted && blocks.isEmpty()) return semicolonAfter(index).coerceAtMost(end)
                        }
                        else -> return null
                    }
                }
                index++
            }
            return null
        }

        private fun bodySubprogramEnd(index: Int, end: Int): Int? {
            val name = identifierAt(index + 1) ?: return null
            var cursor = name.second
            if (valueAt(cursor) == "(") {
                val close = matching(cursor)
                if (close >= end) return null
                cursor = close + 1
            }
            val header = firstBodyHeaderDelimiter(cursor, end) ?: return null
            return if (header.kind == BodyHeaderDelimiterKind.FORWARD_DECLARATION) {
                header.index
            } else {
                implementationEnd(header.index + 1, end)
            }
        }

        private fun executableBlockEnd(start: Int, end: Int): Int? {
            val blocks = mutableListOf<BlockKind>()
            var index = start
            while (index < end.coerceAtMost(tokens.size)) {
                when {
                    isKeyword(index, "BEGIN") -> blocks += BlockKind.BEGIN
                    isCaseStart(index) -> blocks += BlockKind.CASE
                    isKeyword(index, "END") -> when {
                        isKeyword(index + 1, "IF") || isKeyword(index + 1, "LOOP") -> Unit
                        blocks.lastOrNull() == BlockKind.CASE -> blocks.removeAt(blocks.lastIndex)
                        blocks.lastOrNull() == BlockKind.BEGIN -> {
                            blocks.removeAt(blocks.lastIndex)
                            if (blocks.isEmpty()) return semicolonAfter(index).coerceAtMost(end)
                        }
                        else -> return null
                    }
                }
                index++
            }
            return null
        }

        private fun isSubprogramKeyword(index: Int): Boolean =
            isKeyword(index, "PROCEDURE") || isKeyword(index, "FUNCTION")

        private fun isCaseStart(index: Int): Boolean =
            isKeyword(index, "CASE") && !isKeyword(index - 1, "END")

        private fun isKeyword(index: Int, keyword: String): Boolean =
            index in tokens.indices && tokens[index].type is PlSqlKeyword && valueAt(index) == keyword

        private fun parsePackageType(index: Int, owner: QualifiedName): ParsedDeclaration? {
            val name = identifierAt(index + 1) ?: return null
            val end = semicolonAfter(index + 1)
            val shape = typeShape(index, end)
            val recordFields = if (shape == ProjectTypeShape.RECORD) {
                parseRecordFields(name.second, end)
            } else {
                emptyList()
            }
            val declaration = PackageTypeDeclaration(owner, name.first, shape, recordFields, fileId, range(index, end))
            return ParsedDeclaration(declaration, end + 1)
        }

        private fun parseRecordFields(start: Int, end: Int): List<ProjectRecordField> {
            val record = firstAtTopLevel(start, setOf("RECORD"), end) ?: return emptyList()
            val open = record + 1
            if (valueAt(open) != "(") return emptyList()
            val close = matching(open)
            if (close > end) return emptyList()

            val result = mutableListOf<ProjectRecordField>()
            forEachDelimitedPart(open + 1, close) { fieldStart, fieldEnd ->
                parseRecordField(fieldStart, fieldEnd, result.size)?.let(result::add)
            }
            return immutableList(result)
        }

        private fun parseRecordField(start: Int, endExclusive: Int, ordinal: Int): ProjectRecordField? {
            val name = identifierAt(start) ?: return null
            val typeEnd = firstAtTopLevel(
                name.second,
                setOf("NOT", "NULL", "DEFAULT", ":="),
                endExclusive
            ) ?: endExclusive
            val typeRef = typeReference(name.second, typeEnd) ?: return null
            return ProjectRecordField(name.first, ordinal, typeRef, range(start, endExclusive - 1))
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
                    FUNCTION_RETURN_TYPE_TERMINATORS,
                    end
                )
                val deterministic = firstAtTopLevel(returnIndex + 1, setOf("DETERMINISTIC"), end) != null
                val returnType = typeReference(returnIndex + 1, modifier ?: end) ?: return null
                return ParsedDeclaration(
                    PackageFunctionDeclaration(
                        owner,
                        name.first,
                        parameters,
                        returnType,
                        fileId,
                        range(index, end),
                        deterministic = deterministic
                    ),
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
                when (valueAt(index)) {
                    "(", "[" -> depth++
                    ")", "]" -> depth--
                }
                if ((valueAt(index) == "," && depth == 0) || index == endExclusive) {
                    if (partStart < index) consume(partStart, index)
                    partStart = index + 1
                }
            }
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

        private fun parseTypeBody(typeIndex: Int): ParsedUnit {
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
            val ref = valueAt(start) == "REF"
            val nameStart = if (ref) 1 else 0
            val nameTokens = if (percent >= 0) {
                selected.subList(nameStart, percent)
            } else {
                selected.subList(nameStart, selected.size)
            }
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
            if (ref) return RefTypeRef(name, sourceRange)
            val headerTypeIdentity = normalizedHeaderTypeIdentity(selected)
            return if (headerTypeIdentity == null) {
                NamedTypeRef(name, sourceRange)
            } else {
                NamedTypeRef.withHeaderTypeIdentity(name, sourceRange, headerTypeIdentity)
            }
        }

        /**
         * Keeps the complete datatype token sequence for callable-heading correlation.
         * Token boundaries are length encoded so punctuation and adjacent words cannot
         * collapse into the same identity. Lexer trivia is not part of this sequence.
         */
        private fun normalizedHeaderTypeIdentity(selected: List<Token>): String? {
            if (isPlainQualifiedName(selected)) return null
            return selected.joinToString(separator = "") { token ->
                val part = normalizedHeaderToken(token)
                "${part.length}:$part;"
            }
        }

        private fun isPlainQualifiedName(selected: List<Token>): Boolean {
            var index = 0
            if (!isNameAt(selected, index)) return false
            while (true) {
                index++
                if (valueAt(selected, index) != "." || !isNameAt(selected, index + 1)) {
                    return index == selected.size
                }
                index += 2
            }
        }

        private fun normalizedHeaderToken(token: Token): String = when (val type = token.type) {
            GenericTokenType.IDENTIFIER -> "identifier:${QualifiedName(identifier(token)).lookupKey()}"
            is PlSqlKeyword -> if (type.isReserved) {
                "keyword:${value(token)}"
            } else {
                "identifier:${QualifiedName(identifier(token)).lookupKey()}"
            }
            else -> "token:${token.originalValue}"
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

        private fun valueAt(tokens: List<Token>, index: Int): String =
            if (index in tokens.indices) value(tokens[index]) else ""

        private fun identifierAt(start: Int): Pair<OracleIdentifier, Int>? {
            if (start !in tokens.indices || !isName(tokens[start])) return null
            return identifier(tokens[start]) to start + 1
        }

        private fun identifier(token: Token) = OracleIdentifier.fromToken(token.originalValue, token.value)

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
        private data class ParsedBodySubprogram(
            val declaration: ProjectDeclaration?,
            val nextIndex: Int
        )
        private data class ParsedPackageBody(
            val declarations: List<ProjectDeclaration>,
            val end: Int
        )
        private data class BodyHeaderDelimiter(
            val kind: BodyHeaderDelimiterKind,
            val index: Int
        )
        private enum class BodyHeaderDelimiterKind {
            IMPLEMENTATION,
            FORWARD_DECLARATION
        }
        private enum class BlockKind {
            BEGIN,
            CASE
        }
    }
}
