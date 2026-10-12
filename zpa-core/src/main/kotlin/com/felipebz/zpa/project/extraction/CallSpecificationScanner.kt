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
import com.felipebz.zpa.api.PlSqlTokenType

private val EXTERNAL_PARAMETER_PROPERTIES = listOf("LENGTH", "DURATION", "MAXLEN", "CHARSETID", "CHARSETFORM")

/** Boundary recognition of `LANGUAGE C`, `EXTERNAL` and `MLE` call specifications; it never produces facts. */
internal class CallSpecificationScanner(private val stream: TokenStream, private val datatypeShape: DatatypeShape) {
    /**
     * `(LANGUAGE C | EXTERNAL) [NAME id] LIBRARY id [NAME id] [AGENT IN (id {, id})]
     * [WITH CONTEXT] [PARAMETERS (param {, param})]`, returning the index of the `;`.
     * `C` is lexed as an identifier; a quoted `"C"` is not accepted. External parameters are
     * validated structurally against the grammar (see [externalParameterEnd]), not accepted as arbitrary words.
     */
    fun cSpecificationEnd(start: Int, limit: Int): Int? {
        var index = when {
            stream.isKeyword(start, "EXTERNAL") -> start + 1
            stream.isKeyword(start, "LANGUAGE") && stream.isUnquotedWord(start + 1, "C") -> start + 2
            else -> return null
        }
        if (stream.isKeyword(index, "NAME")) index = stream.nameEnd(index + 1, limit) ?: return null
        if (!stream.isKeyword(index, "LIBRARY")) return null
        index = stream.nameEnd(index + 1, limit) ?: return null
        if (stream.isKeyword(index, "NAME")) index = stream.nameEnd(index + 1, limit) ?: return null
        if (stream.isKeyword(index, "AGENT")) {
            if (!stream.isKeyword(index + 1, "IN")) return null
            index = stream.delimitedListEnd(index + 2, limit) { stream.nameEnd(it, limit) } ?: return null
        }
        if (stream.isKeyword(index, "WITH")) {
            if (!stream.isKeyword(index + 1, "CONTEXT")) return null
            index += 2
        }
        if (stream.isKeyword(index, "PARAMETERS")) index = stream.delimitedListEnd(index + 1, limit) { externalParameterEnd(it, limit) } ?: return null
        return index.takeIf { it < limit && stream.isPunctuator(it, PlSqlPunctuator.SEMICOLON) }
    }

    /**
     * One `EXTERNAL_PARAMETER`, following the grammar's ordered alternatives:
     * `CONTEXT` | `SELF (TDO | property)` | `(RETURN | name) [property] [BY REFERENCE] [datatype]`.
     * Returns the index after the parameter; the caller requires a real `,` or `)` there.
     * The optional datatype must pass [strictDatatype]; anything else rejects the unit.
     */
    private fun externalParameterEnd(start: Int, limit: Int): Int? {
        if (start >= limit) return null
        contextParameterEnd(start)?.let { return it }
        selfParameterEnd(start)?.let { return it }
        if (!stream.isKeyword(start, "RETURN") && !stream.isNameBefore(start, limit)) return null
        val afterProperty = externalParameterPropertyEnd(start + 1) ?: (start + 1)
        val index = byReferenceEnd(afterProperty) ?: return null
        val datatypeEnd = externalDatatypeEnd(index, limit) ?: return null
        if (datatypeEnd > index && !datatypeShape.strictDatatype(index, datatypeEnd)) return null
        return datatypeEnd
    }

    private fun endsExternalParameter(index: Int): Boolean =
        stream.isPunctuator(index, PlSqlPunctuator.COMMA) || stream.isPunctuator(index, PlSqlPunctuator.RPARENTHESIS)

    /**
     * CONTEXT is a complete parameter; if anything else follows it is an ordinary (unreserved)
     * name, as the grammar's third alternative allows (e.g. `CONTEXT x`).
     */
    private fun contextParameterEnd(start: Int): Int? =
        if (stream.isKeyword(start, "CONTEXT") && endsExternalParameter(start + 1)) start + 1 else null

    /** `SELF (TDO | property)` must end the parameter; otherwise SELF is an ordinary name. */
    private fun selfParameterEnd(start: Int): Int? {
        if (!stream.isKeyword(start, "SELF")) return null
        val selfEnd = if (stream.isKeyword(start + 1, "TDO")) start + 2 else externalParameterPropertyEnd(start + 1)
        return selfEnd?.takeIf { endsExternalParameter(it) }
    }

    /** `[BY REFERENCE]` at [index]: the index after it, [index] itself when absent, null for a `BY` without `REFERENCE`. */
    private fun byReferenceEnd(index: Int): Int? = when {
        !stream.isKeyword(index, "BY") -> index
        stream.isKeyword(index + 1, "REFERENCE") -> index + 2
        else -> null
    }

    /** `INDICATOR [STRUCT | TDO]` | `LENGTH` | `DURATION` | `MAXLEN` | `CHARSETID` | `CHARSETFORM`. */
    private fun externalParameterPropertyEnd(index: Int): Int? = when {
        stream.isKeyword(index, "INDICATOR") ->
            if (stream.isKeyword(index + 1, "STRUCT") || stream.isKeyword(index + 1, "TDO")) index + 2 else index + 1
        EXTERNAL_PARAMETER_PROPERTIES.any { stream.isKeyword(index, it) } -> index + 1
        else -> null
    }

    /** Index of the depth-0 `,` or `)` ending a datatype slice that starts at [start]. */
    private fun externalDatatypeEnd(start: Int, limit: Int): Int? {
        var index = start
        while (index < limit) {
            when {
                stream.isPunctuator(index, PlSqlPunctuator.COMMA) || stream.isPunctuator(index, PlSqlPunctuator.RPARENTHESIS) -> return index
                stream.isPunctuator(index, PlSqlPunctuator.LPARENTHESIS) -> index = (stream.strictClose(index, limit) ?: return null) + 1
                stream.isCloser(index) || stream.isOpener(index) || stream.isPunctuator(index, PlSqlPunctuator.SEMICOLON) -> return null
                else -> index++
            }
        }
        return null
    }

    /** `LANGUAGE name[.name] [PURE] <inline source>` or `MODULE name[.name] [ENV name[.name]] SIGNATURE 'str'`. */
    fun mleSpecificationEnd(start: Int, limit: Int): Int? {
        if (stream.isKeyword(start, "LANGUAGE")) {
            var index = stream.strictQualifiedName(start + 1, limit)?.second ?: return null
            if (stream.isKeyword(index, "PURE")) index++
            return if (stream.tokenTypeAt(index) === PlSqlTokenType.MLE_INLINE_SOURCE) index + 1 else null
        }
        if (!stream.isKeyword(start, "MODULE")) return null
        var index = stream.strictQualifiedName(start + 1, limit)?.second ?: return null
        if (stream.isKeyword(index, "ENV")) index = stream.strictQualifiedName(index + 1, limit)?.second ?: return null
        if (!stream.isKeyword(index, "SIGNATURE") || stream.tokenTypeAt(index + 1) !== PlSqlTokenType.STRING_LITERAL) return null
        return index + 2
    }
}
