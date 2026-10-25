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
     * The first segment of a qualified name (`a.b`, `a.b(…)`, `c%ROWCOUNT`, a qualified type name). [LocalReference.segments]
     * holds the rest, and [LocalReference.lastSegmentContext] the position of the whole name.
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
    SQL_VALUE,

    /** The name of `name(args)` inside SQL, single-row `VALUES` included: a SQL built-in wins over a local function. */
    SQL_CALL,

    /**
     * A plain name in the `VALUES` of a single-table `INSERT`: no column scope, so a local wins, but a local function is
     * still used in SQL (PLS-00231).
     */
    INSERT_VALUE,

    /** The label of `GOTO`: a statement label under the branching restrictions. */
    GOTO_TARGET,

    /** The label of `EXIT` or `CONTINUE`: the label of an enclosing loop. */
    LOOP_TARGET,

    /** The cursor of `OPEN`, `FETCH` or `CLOSE`: an explicit cursor or a cursor variable. */
    CURSOR_OPERAND,

    /** The cursor of `OPEN … FOR`: a cursor variable, never an explicit cursor. */
    OPEN_FOR_OPERAND,

    /**
     * The single name of a `FOR r IN c` or `FOR r IN c(…)` control: an explicit cursor, or a value iterated once (a
     * variable or a function). Unlike [CURSOR_OPERAND], a variable here need not be a cursor variable.
     */
    FOR_CONTROL
}

/** A segment written after the first one: a member (`.name`) or an attribute (`%name`), with its source [range]. */
internal class LocalSegment(val name: OracleIdentifier, val isAttribute: Boolean, val range: SourceRange) {
    override fun toString() = (if (isAttribute) "%" else ".") + name
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
    /** For a [LocalReferenceContext.QUALIFIER], the segments written after this one, in order; empty otherwise. */
    val segments: List<LocalSegment>,
    /** The position of the whole name: [context] for a single segment, the qualified name's own position otherwise. */
    val lastSegmentContext: LocalReferenceContext,
    /** Written in the default expression of a subprogram or cursor parameter. */
    val inParameterDefault: Boolean,
    /** The innermost exception handler holding the reference, for the branching rule of `GOTO`. */
    val handler: SourceRange?,
    /** For a name inside SQL, the relations that can qualify it. */
    val sql: LocalSqlScope? = null,
    /**
     * The range of the first segment's own token: [range], except for a written datatype, whose [range] is the type
     * use's. Null when the datatype's name is not written as `name(.name)*` (a multi-word built-in type).
     */
    val nameRange: SourceRange? = range
) {
    val remainingSegments: Int get() = segments.size

    override fun toString() =
        "$context $name${segments.joinToString("")}@${range.startLine}:${range.startColumn} in $region" +
            (if (segments.isNotEmpty()) " as $lastSegmentContext" else "") + (if (inParameterDefault) " (default)" else "")

    companion object {
        /** Document order of the start, then of the end, then context for references at one position. */
        val ORDER: Comparator<LocalReference> = compareBy<LocalReference>(
            { it.range.startLine }, { it.range.startColumn }, { it.range.endLine }, { it.range.endColumn }
        ).thenBy { it.context }
    }
}
