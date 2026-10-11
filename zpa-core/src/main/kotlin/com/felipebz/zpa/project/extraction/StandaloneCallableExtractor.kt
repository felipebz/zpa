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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.project.*

/** Standalone-only return-type terminators (the package set lacks AGGREGATE, ORDER and CLUSTER). */
private val STANDALONE_RETURN_TYPE_TERMINATORS = listOf(
    "DETERMINISTIC", "PIPELINED", "SQL_MACRO", "PARALLEL_ENABLE", "ORDER", "CLUSTER", "SHARD_ENABLE",
    "RESULT_CACHE", "AUTHID", "DEFAULT", "ACCESSIBLE", "AGGREGATE", "IS", "AS"
)

private val CALL_SPECIFICATION_WORDS = setOf("LANGUAGE", "EXTERNAL", "MLE")

/**
 * Strict extraction of `CREATE [OR REPLACE] PROCEDURE|FUNCTION` units. It never uses the value-based helpers for
 * structural decisions; any doubt yields null (no fact, nothing consumed).
 */
internal class StandaloneCallableExtractor(
    private val stream: TokenStream,
    private val bounds: UnitBounds,
    private val datatypeShape: DatatypeShape,
    private val typeReferences: TypeReferences,
    private val callableProperties: CallableProperties,
    private val callSpecifications: CallSpecificationScanner,
    private val bodyBoundaries: BodyBoundaryScanner
) {
    fun parseStandaloneCallable(create: Int, function: Boolean): ParsedUnit? {
        if (!stream.isKeyword(create, "CREATE")) return null
        val limit = bounds.unitLimit(create)
        var index = create + 1
        val orReplace = stream.isKeyword(index, "OR") && stream.isKeyword(index + 1, "REPLACE")
        if (orReplace) index += 2
        if (stream.isKeyword(index, "EDITIONABLE") || stream.isKeyword(index, "NONEDITIONABLE")) index++
        if (!stream.isKeyword(index, if (function) "FUNCTION" else "PROCEDURE")) return null
        index++
        if (stream.isKeyword(index, "IF")) {
            // IF NOT EXISTS cannot be combined with OR REPLACE (ORA-11541).
            if (orReplace || !stream.isKeyword(index + 1, "NOT") || !stream.isKeyword(index + 2, "EXISTS")) return null
            index += 3
        }
        val name = stream.strictQualifiedName(index, limit) ?: return null
        index = skipUnitOptions(name.second, limit) ?: return null
        if (stream.isKeyword(index, "WRAPPED")) {
            // The lexer makes the whole payload one opaque WRAPPED_SOURCE token. No signature
            // can be recovered, so the unit is skipped without a fact.
            return if (index + 1 < limit && stream.tokenTypeAt(index + 1) === PlSqlTokenType.WRAPPED_SOURCE) {
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
            index = callableProperties.skipPropertiesStrict(index, limit, function = false)?.next ?: return null
            if (!stream.isKeyword(index, "IS") && !stream.isKeyword(index, "AS")) return null
            val end = strictImplementationEnd(index + 1, limit) ?: return null
            return ParsedUnit(
                listOf(StandaloneProcedureDeclaration(name.first, parameters, stream.fileId, stream.range(create, end))),
                end + 1
            )
        }

        // Function: RETURN datatype, properties, then an implementation or a terminal `;`.
        if (!stream.isKeyword(index, "RETURN")) return null
        val typeStart = index + 1
        val typeEnd = returnTypeEnd(typeStart, limit) ?: return null
        if (!datatypeShape.strictDatatype(typeStart, typeEnd)) return null
        val returnType = typeReferences.typeReference(typeStart, typeEnd) ?: return null
        val properties = callableProperties.skipPropertiesStrict(typeEnd, limit, function = true) ?: return null
        index = properties.next
        val end = when {
            properties.terminal -> index
            stream.isKeyword(index, "IS") || stream.isKeyword(index, "AS") -> strictImplementationEnd(index + 1, limit) ?: return null
            stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> index // body-less declaration
            else -> return null
        }
        if (end >= limit || !stream.isPunctuator(end, PlSqlPunctuator.SEMICOLON)) return null
        return ParsedUnit(
            listOf(
                StandaloneFunctionDeclaration(
                    name.first, parameters, returnType, stream.fileId, stream.range(create, end),
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
                stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> index = (stream.strictClose(index, limit) ?: return null) + 1
                stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) ||
                    STANDALONE_RETURN_TYPE_TERMINATORS.any { stream.isKeyword(index, it) } -> return index.takeIf { it > start }
                stream.isOpener(index) || stream.isCloser(index) -> return null
                else -> index++
            }
        }
        return null
    }

    /** `[TIMESTAMP 'literal'] [SHARING = METADATA|NONE]`; returns the next index or null. */
    private fun skipUnitOptions(start: Int, limit: Int): Int? {
        var index = start
        if (stream.isKeyword(index, "TIMESTAMP")) {
            if (stream.tokenTypeAt(index + 1) !== PlSqlTokenType.STRING_LITERAL) return null
            index += 2
        } else if (stream.tokenTypeAt(index) === PlSqlTokenType.TIMESTAMP_LITERAL) {
            index++
        }
        if (stream.isKeyword(index, "SHARING")) {
            if (!stream.isPunctuator(index + 1, PlSqlPunctuator.EQUALS)) return null
            if (!stream.isKeyword(index + 2, "METADATA") && !stream.isKeyword(index + 2, "NONE")) return null
            index += 3
        }
        return index.takeIf { it < limit }
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
        if (!stream.isPunctuator(start, PlSqlPunctuator.LPARENTHESIS)) return StrictParameters.Absent
        val close = stream.strictClose(start, limit) ?: return StrictParameters.Failed
        val parts = stream.splitStrict(start, close) ?: return StrictParameters.Failed
        val parameters = parts.mapIndexed { position, part ->
            parseParameterStrict(part.first, part.last + 1, position + 1) ?: return StrictParameters.Failed
        }
        return StrictParameters.Parsed(immutableList(parameters), close + 1)
    }

    /** `name [IN] (OUT [NOCOPY] datatype | datatype [(DEFAULT|:=) expression])`, fully consumed. */
    private fun parseParameterStrict(start: Int, endExclusive: Int, ordinal: Int): ProjectParameter? {
        if (!stream.isNameBefore(start, endExclusive)) return null
        var cursor = start + 1
        var mode = ParameterMode.IN
        if (stream.isKeyword(cursor, "IN")) cursor++
        var nocopy = false
        if (stream.isKeyword(cursor, "OUT")) {
            mode = if (mode == ParameterMode.IN && stream.isKeyword(cursor - 1, "IN")) ParameterMode.IN_OUT else ParameterMode.OUT
            cursor++
            if (stream.isKeyword(cursor, "NOCOPY")) {
                nocopy = true
                cursor++
            }
        } else if (stream.isKeyword(cursor, "NOCOPY")) {
            return null
        }
        var defaultIndex: Int? = null
        var depth = 0
        for (index in cursor until endExclusive) {
            when {
                stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> depth++
                stream.isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) -> depth--
                depth == 0 && (stream.isKeyword(index, "DEFAULT") || stream.isPunctuator(index, PlSqlPunctuator.ASSIGNMENT)) -> {
                    defaultIndex = index
                    break
                }
            }
        }
        if (defaultIndex != null && (mode != ParameterMode.IN || defaultIndex + 1 >= endExclusive)) return null
        val typeEnd = defaultIndex ?: endExclusive
        if (!datatypeShape.strictDatatype(cursor, typeEnd)) return null
        val type = typeReferences.typeReference(cursor, typeEnd) ?: return null
        return ProjectParameter(name = stream.identifier(stream.tokens[start]), ordinal = ordinal, mode = mode, nocopy = nocopy,
            typeRef = type, defaultPresent = defaultIndex != null, sourceRange = stream.range(start, endExclusive - 1))
    }

    /** End (a real `;` below [limit]) of the body or call specification starting at [start]. */
    private fun strictImplementationEnd(start: Int, limit: Int): Int? {
        if (start >= limit) return null
        val end = when {
            stream.isKeyword(start, "LANGUAGE") && stream.isKeyword(start + 1, "JAVA") ->
                if (stream.isKeyword(start + 2, "NAME") && stream.tokenTypeAt(start + 3) === PlSqlTokenType.STRING_LITERAL) start + 4 else return null
            stream.isKeyword(start, "LANGUAGE") || stream.isKeyword(start, "EXTERNAL") -> callSpecifications.cSpecificationEnd(start, limit) ?: return null
            stream.isKeyword(start, "MLE") -> callSpecifications.mleSpecificationEnd(start + 1, limit) ?: return null
            // A quoted identifier spelled like a call-specification keyword is not one.
            stream.tokens[start].type == GenericTokenType.IDENTIFIER && stream.valueAt(start) in CALL_SPECIFICATION_WORDS -> return null
            else -> bodyBoundaries.implementationEnd(start, limit) ?: return null
        }
        return end.takeIf { it < limit && stream.isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
    }
}
