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
import com.felipebz.zpa.project.TypeRef

/**
 * A structurally valid package collection head. [elementType] is the direct element datatype, present only when
 * the element and the rest of the declaration passed the strict checks.
 */
internal class CollectionSyntax(val elementType: TypeRef?)

/**
 * Recognizes the head and the direct element of a package collection declaration
 * `TYPE name IS (TABLE OF | (VARRAY | [VARYING] ARRAY) (bound) OF) datatype [NULL | NOT NULL]
 * [INDEX BY datatype [NULL | NOT NULL]] ;` on typed tokens. The bound and the index datatype are validated but
 * never retained. The element is a `TypeRef` only for a slice admitted by the strict datatype checks.
 */
internal class CollectionElementSyntax(
    private val stream: TokenStream,
    private val datatypeShape: DatatypeShape,
    private val typeReferences: TypeReferences
) {
    /**
     * [isIndex] is the `IS` keyword and [terminator] the declaration's terminating `;`. Null when the declaration
     * does not start with a valid collection head.
     */
    fun parse(isIndex: Int, terminator: Int): CollectionSyntax? {
        var cursor = isIndex + 1
        val table = stream.isKeyword(cursor, "TABLE") && stream.isKeyword(cursor + 1, "OF")
        if (table) {
            cursor += 2
        } else {
            val arrayHead = when {
                stream.isKeyword(cursor, "VARRAY") || stream.isKeyword(cursor, "ARRAY") -> cursor + 1
                stream.isKeyword(cursor, "VARYING") && stream.isKeyword(cursor + 1, "ARRAY") -> cursor + 2
                else -> return null
            }
            if (!stream.isPunctuator(arrayHead, PlSqlPunctuator.LPARENTHESIS)) return null
            val close = stream.strictClose(arrayHead, terminator) ?: return null
            if (!isBound(arrayHead + 1, close) || !stream.isKeyword(close + 1, "OF")) return null
            cursor = close + 2
        }
        return CollectionSyntax(element(cursor, terminator, table))
    }

    /** The array bound is an integer literal or a (qualified) constant name; its value is not interpreted. */
    private fun isBound(start: Int, endExclusive: Int): Boolean {
        if (start >= endExclusive) return false
        if (stream.tokenTypeAt(start) === PlSqlTokenType.INTEGER_LITERAL) return start + 1 == endExclusive
        return stream.strictQualifiedNameAnyLength(start, endExclusive) == endExclusive
    }

    private fun element(start: Int, terminator: Int, table: Boolean): TypeRef? {
        val end = datatypeShape.datatypeEnd(start, terminator) { stream.isKeyword(it, "NOT") || stream.isKeyword(it, "NULL") || (table && stream.isKeyword(it, "INDEX")) }
            ?: return null
        if (!isAdmittedDatatype(start, end)) return null
        var cursor = nullability(end) ?: return null
        if (table && stream.isKeyword(cursor, "INDEX")) {
            if (!stream.isKeyword(cursor + 1, "BY")) return null
            val indexStart = cursor + 2
            val indexEnd = datatypeShape.datatypeEnd(indexStart, terminator) { stream.isKeyword(it, "NOT") || stream.isKeyword(it, "NULL") }
                ?: return null
            if (!isAdmittedDatatype(indexStart, indexEnd)) return null
            cursor = nullability(indexEnd) ?: return null
        }
        if (cursor != terminator) return null
        return typeReferences.typeReference(start, end)
    }

    private fun isAdmittedDatatype(start: Int, endExclusive: Int): Boolean =
        datatypeShape.strictDatatype(start, endExclusive) && datatypeShape.strictTypeArguments(start, endExclusive)

    /** Index after an optional `NULL` or `NOT NULL`; null for a `NOT` that is not followed by `NULL`. */
    private fun nullability(index: Int): Int? = when {
        stream.isKeyword(index, "NOT") -> if (stream.isKeyword(index + 1, "NULL")) index + 2 else null
        stream.isKeyword(index, "NULL") -> index + 1
        else -> index
    }
}
