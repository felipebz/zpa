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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange

/** The syntactic position of a reference, which decides the declaration kinds it may bind. */
internal enum class LocalReferenceContext {
    /** A plain name read or assigned in PL/SQL: an expression, an assignment target, an `INTO` target. */
    VALUE,

    /** The name of a call statement, with or without arguments (`p;`, `p(1);`). */
    CALL_STATEMENT,

    /** The name of `name(args)` in an expression: a function call, an element access or a type constructor. */
    CALL_EXPRESSION,

    /**
     * The first segment of a qualified name (`a.b`, `a.b(…)`, `c%ROWCOUNT`, a qualified type name). Only that segment
     * is looked up; [LocalReference.remainingSegments] counts the rest.
     */
    QUALIFIER,

    /** The single-segment name of a written datatype (a named type or `REF` type). */
    TYPE,

    /** The single-segment name anchored by `%TYPE`. */
    ANCHOR_TYPE,

    /** The single-segment name anchored by `%ROWTYPE`. */
    ANCHOR_ROWTYPE,

    /** The exception of `RAISE e`, of a `WHEN e` handler, or of `PRAGMA EXCEPTION_INIT(e, …)`. */
    EXCEPTION,

    /** A plain name inside a SQL statement or query, other than the PL/SQL targets of an `INTO` clause. */
    SQL_VALUE
}

/**
 * One name lookup written in a file, as an AST-detached fact: no node, token or source text is reachable from it.
 *
 * [region] is the innermost region containing the reference; for a written datatype it is the type use's
 * [LocalTypeUse.lookupRegion] and [range] is the type use's range. The lookup position is the start of [range].
 */
internal class LocalReference(
    val name: OracleIdentifier,
    val range: SourceRange,
    val context: LocalReferenceContext,
    val region: LocalRegionId,
    /** For a [LocalReferenceContext.QUALIFIER], the number of segments after this one; 0 otherwise. */
    val remainingSegments: Int,
    /** Written in the default expression of a subprogram or cursor parameter. */
    val inParameterDefault: Boolean
) {
    override fun toString() =
        "$context $name@${range.startLine}:${range.startColumn} in $region" +
            (if (remainingSegments > 0) " +$remainingSegments" else "") + (if (inParameterDefault) " (default)" else "")

    companion object {
        /** Document order of the start, then of the end, then context for references at one position. */
        val ORDER: Comparator<LocalReference> = compareBy<LocalReference>(
            { it.range.startLine }, { it.range.startColumn }, { it.range.endLine }, { it.range.endColumn }
        ).thenBy { it.context }
    }
}
