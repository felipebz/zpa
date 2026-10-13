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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName

/**
 * Datatype families whose members behave identically for PL/SQL parameter passing (verified on Oracle 26ai by
 * single-candidate compilation of every member pair). Finer than [ProjectBuiltInTypeCategory], which mixes CLOB with
 * BLOB, DATE with INTERVAL and VARCHAR2 with RAW/ROWID/LONG.
 */
internal enum class DatatypeFamily {
    NUMERIC, CHARACTER, CLOB, BLOB, RAW, DATETIME, INTERVAL, BOOLEAN, JSON, ROWID, COMPOSITE
}

/** Outcome of passing an actual of one family to a formal of another. Only [INCOMPATIBLE] may ever eliminate. */
internal enum class DatatypeCompatibility {
    /** A single candidate with this pairing compiles (it says nothing about overload preference). */
    COMPATIBLE,

    /** No implicit conversion exists: a candidate with this pairing is never chosen. */
    INCOMPATIBLE,

    /** Not measured, environment-dependent, or not decidable from family identity. */
    UNKNOWN
}

/**
 * Exact built-in name classification and the measured actual-to-formal compatibility relation. Unknown or ambiguous
 * names stay unclassified (null); nothing is inferred from spelling similarity, categories or expressions.
 */
@OptIn(ZpaExperimentalApi::class)
internal object DatatypeFamilies {
    private val names: Map<String, DatatypeFamily> = buildMap {
        fun put(family: DatatypeFamily, vararg names: String) = names.forEach { put(it, family) }
        put(
            DatatypeFamily.NUMERIC, "NUMBER", "INTEGER", "INT", "SMALLINT", "DEC", "DECIMAL", "NUMERIC", "FLOAT", "REAL",
            // DOUBLE is the retained leading word of DOUBLE PRECISION.
            "DOUBLE", "PLS_INTEGER", "BINARY_INTEGER", "NATURAL", "NATURALN", "POSITIVE", "POSITIVEN", "SIGNTYPE",
            "SIMPLE_INTEGER", "BINARY_FLOAT", "BINARY_DOUBLE"
        )
        put(DatatypeFamily.CHARACTER, "VARCHAR2", "VARCHAR", "CHAR", "CHARACTER", "STRING", "NCHAR", "NVARCHAR2")
        put(DatatypeFamily.CLOB, "CLOB", "NCLOB")
        put(DatatypeFamily.BLOB, "BLOB")
        put(DatatypeFamily.RAW, "RAW")
        // TIMESTAMP covers WITH [LOCAL] TIME ZONE: every variant behaves as DATE.
        put(DatatypeFamily.DATETIME, "DATE", "TIMESTAMP")
        // INTERVAL is the retained leading word of both DAY TO SECOND and YEAR TO MONTH.
        put(DatatypeFamily.INTERVAL, "INTERVAL")
        put(DatatypeFamily.BOOLEAN, "BOOLEAN")
        put(DatatypeFamily.JSON, "JSON")
        put(DatatypeFamily.ROWID, "ROWID", "UROWID")
        // Deliberately absent: LONG (LONG and LONG RAW share the leading word), BFILE, REF CURSOR, owned types.
    }

    /**
     * Family of a built-in type name written in source: exactly one unquoted segment naming a supported built-in.
     * A quoted or qualified name is a user-defined type, whatever its spelling.
     */
    fun ofName(name: QualifiedName): DatatypeFamily? {
        val segment = name.segments.singleOrNull() ?: return null
        if (segment.quoted) return null
        return names[segment.lookupName]
    }

    /** Family of a dictionary datatype: owner-less, the base word before any length/precision, known variants only. */
    fun ofExternal(datatype: ProjectExternalDatatype): DatatypeFamily? {
        if (datatype.typeOwner.isPresent) return null
        val base = datatype.name.substringBefore('(').trim()
        if (base.any { it.isWhitespace() }) {
            val first = base.substringBefore(' ')
            // TIMESTAMP(6) WITH [LOCAL] TIME ZONE and INTERVAL DAY(2) TO SECOND(6) / YEAR(2) TO MONTH; LONG RAW etc. stay unknown.
            return when (first) {
                "TIMESTAMP" -> DatatypeFamily.DATETIME
                "INTERVAL" -> DatatypeFamily.INTERVAL
                else -> null
            }
        }
        if (base.isEmpty() || base.contains('"')) return null
        return ofName(QualifiedName(OracleIdentifier.fromSource(base)))
    }

    /**
     * Family of a type described through the source/external composition, for the request [startReference] (a retained
     * reference) or [startName] (a name query). Only conclusive statuses classify:
     * - `NOT_EXTERNAL` with a source built-in (terminal reference, else the request name), or with a source RECORD,
     *   COLLECTION or OBJECT declaration;
     * - `RESOLVED` column `%TYPE` with an owner-less datatype, or `RESOLVED` `%ROWTYPE`.
     *
     * A retained reference written with `%ROWTYPE` is COMPOSITE whatever its resolution: any declaration that compiles
     * with it holds a record (table, view or cursor row), and one that does not compile cannot receive a call. The shape
     * names no relation or field, so it can only separate composite from scalar, never one composite from another.
     */
    fun ofDescription(description: ProjectExternalTypeDescription, startName: ProjectQualifiedName?, startReference: ProjectTypeReference?): DatatypeFamily? {
        if (startReference != null && startReference.kind == ProjectTypeReference.Kind.ANCHORED &&
            startReference.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE
        ) return DatatypeFamily.COMPOSITE
        val source = description.source
        return when (description.status) {
            ProjectExternalTypeDescription.Status.NOT_EXTERNAL -> when (source.status) {
                ProjectTypeDescription.Status.BUILT_IN -> {
                    val name = source.terminalReference.map { it.name }.orElse(null) ?: startName
                    name?.let { ofName(it.name) }
                }
                ProjectTypeDescription.Status.DECLARED_TYPE -> {
                    val shape = (source.declaration.get() as? ProjectShapedTypeView)?.shape?.orElse(null)
                    if (shape == ProjectTypeShape.RECORD || shape == ProjectTypeShape.COLLECTION || shape == ProjectTypeShape.OBJECT) {
                        DatatypeFamily.COMPOSITE
                    } else null
                }
                else -> null
            }
            ProjectExternalTypeDescription.Status.RESOLVED -> when (description.anchor.orElse(null)) {
                ProjectTypeReference.Anchor.ROWTYPE -> DatatypeFamily.COMPOSITE
                ProjectTypeReference.Anchor.TYPE -> description.lookup.flatMap { it.column }.flatMap { it.datatype }.orElse(null)?.let { ofExternal(it) }
                null -> null
            }
            else -> null
        }
    }

    private val families = DatatypeFamily.entries
    private const val C = 'C'
    private const val X = 'X'

    /**
     * Rows: actual family; columns: formal family, both in [DatatypeFamily] order. `C` compiles, `X` is rejected
     * (PLS-00306) for every measured member pair and parameter mode, `?` is UNKNOWN: BOOLEAN with NUMERIC/CHARACTER
     * depends on PLSQL_IMPLICIT_CONVERSION_BOOL, INTERVAL day/second versus year/month is not retained, and composite
     * identity is nominal (a `%ROWTYPE` formal even accepts a same-shaped record).
     */
    private val matrix = arrayOf(
        //          NUM CHR CLB BLB RAW DTM INT BOO JSN RID CMP
        /* NUM */ "CCXXXXX?XXX",
        /* CHR */ "CCCXCCC?XCX",
        /* CLB */ "XCCXXXXXXXX",
        /* BLB */ "XXXCCXXXXXX",
        /* RAW */ "XCXCCXXXXXX",
        /* DTM */ "XCXXXCXXXXX",
        /* INT */ "XCXXXX?XXXX",
        /* BOO */ "??XXXXXCXXX",
        /* JSN */ "XXXXXXXXCXX",
        /* RID */ "XCXXXXXXXCX",
        /* CMP */ "XXXXXXXXXX?"
    )

    init {
        check(matrix.size == families.size && matrix.all { it.length == families.size }) { "Malformed compatibility matrix" }
    }

    /** Compatibility of an actual of family [actual] with a formal of family [formal]; unclassified sides are UNKNOWN. */
    fun compatibility(actual: DatatypeFamily?, formal: DatatypeFamily?): DatatypeCompatibility {
        if (actual == null || formal == null) return DatatypeCompatibility.UNKNOWN
        return when (matrix[actual.ordinal][formal.ordinal]) {
            C -> DatatypeCompatibility.COMPATIBLE
            X -> DatatypeCompatibility.INCOMPATIBLE
            else -> DatatypeCompatibility.UNKNOWN
        }
    }
}
