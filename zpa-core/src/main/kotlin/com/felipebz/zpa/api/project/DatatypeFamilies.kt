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
import java.util.*

/** Outcome of passing an actual of one family to a formal of another. Only [INCOMPATIBLE] may ever eliminate. */
internal enum class DatatypeCompatibility {
    /** A single candidate with this pairing compiles (it says nothing about overload preference). */
    COMPATIBLE,

    /** No implicit conversion exists: a candidate with this pairing is never chosen. */
    INCOMPATIBLE,

    /** The outcome depends on the PLSQL_IMPLICIT_CONVERSION_BOOL setting of the calling unit. */
    ENVIRONMENT_DEPENDENT,

    /** Not decidable from family identity (interval kinds, nominal composite identity) or not classified. */
    UNKNOWN
}

/**
 * Exact built-in name classification and the measured actual-to-formal compatibility relation. Unknown or ambiguous
 * names stay unclassified (null); nothing is inferred from spelling similarity, categories or expressions.
 */
@OptIn(ZpaExperimentalApi::class)
internal object DatatypeFamilies {
    private val names: Map<String, ProjectDatatypeFamily> = buildMap {
        fun put(family: ProjectDatatypeFamily, vararg names: String) = names.forEach { put(it, family) }
        put(
            ProjectDatatypeFamily.NUMERIC, "NUMBER", "INTEGER", "INT", "SMALLINT", "DEC", "DECIMAL", "NUMERIC", "FLOAT", "REAL",
            // DOUBLE is the retained leading word of DOUBLE PRECISION.
            "DOUBLE", "PLS_INTEGER", "BINARY_INTEGER", "NATURAL", "NATURALN", "POSITIVE", "POSITIVEN", "SIGNTYPE",
            "SIMPLE_INTEGER", "BINARY_FLOAT", "BINARY_DOUBLE"
        )
        put(ProjectDatatypeFamily.CHARACTER, "VARCHAR2", "VARCHAR", "CHAR", "CHARACTER", "STRING", "NCHAR", "NVARCHAR2")
        put(ProjectDatatypeFamily.CLOB, "CLOB", "NCLOB")
        put(ProjectDatatypeFamily.BLOB, "BLOB")
        put(ProjectDatatypeFamily.RAW, "RAW")
        // TIMESTAMP covers WITH [LOCAL] TIME ZONE: every variant behaves as DATE.
        put(ProjectDatatypeFamily.DATETIME, "DATE", "TIMESTAMP")
        // INTERVAL is the retained leading word of both DAY TO SECOND and YEAR TO MONTH.
        put(ProjectDatatypeFamily.INTERVAL, "INTERVAL")
        put(ProjectDatatypeFamily.BOOLEAN, "BOOLEAN")
        put(ProjectDatatypeFamily.JSON, "JSON")
        put(ProjectDatatypeFamily.ROWID, "ROWID", "UROWID")
        // Deliberately absent: LONG (LONG and LONG RAW share the leading word), BFILE, REF CURSOR, owned types.
    }

    /** The only multi-word built-in spellings recognized (precision and length groups removed, single spaces). */
    private val multiWord: Map<String, ProjectDatatypeFamily> = mapOf(
        "TIMESTAMP WITH TIME ZONE" to ProjectDatatypeFamily.DATETIME,
        "TIMESTAMP WITH LOCAL TIME ZONE" to ProjectDatatypeFamily.DATETIME,
        "INTERVAL DAY TO SECOND" to ProjectDatatypeFamily.INTERVAL,
        "INTERVAL YEAR TO MONTH" to ProjectDatatypeFamily.INTERVAL,
        "DOUBLE PRECISION" to ProjectDatatypeFamily.NUMERIC
    )

    /**
     * Family of a built-in type name written in source: exactly one unquoted segment naming a supported built-in.
     * A quoted or qualified name is a user-defined type, whatever its spelling.
     */
    fun ofName(name: QualifiedName): ProjectDatatypeFamily? {
        val segment = name.segments.singleOrNull() ?: return null
        if (segment.quoted) return null
        return names[segment.lookupName]
    }

    /**
     * Family of a built-in type spelled as text (a dictionary DATA_TYPE or a caller-supplied name): length, precision
     * and scale groups are ignored, a single word goes through [ofName], and only the known TIMESTAMP, INTERVAL and
     * DOUBLE PRECISION phrases are accepted among multi-word spellings. Quotes, dots or anything else stay unclassified.
     */
    fun ofTypeName(text: String): ProjectDatatypeFamily? {
        if (text.contains('"') || text.contains('.')) return null
        val stripped = stripGroups(text) ?: return null
        val words = stripped.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        if (words.size > 1) return multiWord[words.joinToString(" ").uppercase(Locale.ROOT)]
        val word = words.single()
        if (!word.all { it.isLetterOrDigit() || it == '_' || it == '$' || it == '#' }) return null
        return ofName(QualifiedName(OracleIdentifier.fromSource(word)))
    }

    /** Family of a dictionary datatype: owner-less, known spellings only (see [ofTypeName]). */
    fun ofExternal(datatype: ProjectExternalDatatype): ProjectDatatypeFamily? {
        if (datatype.typeOwner.isPresent) return null
        return ofTypeName(datatype.name)
    }

    /** Removes balanced, non-nested `( … )` groups whose content is digits, commas, spaces and length semantics. */
    private fun stripGroups(text: String): String? {
        val out = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val c = text[index]
            when (c) {
                '(' -> {
                    val close = text.indexOf(')', index)
                    if (close < 0) return null
                    val content = text.substring(index + 1, close).uppercase(Locale.ROOT)
                    if (!GROUP_CONTENT.matches(content)) return null
                    out.append(' ')
                    index = close + 1
                }
                ')' -> return null
                else -> {
                    out.append(c)
                    index++
                }
            }
        }
        return out.toString()
    }

    private val WHITESPACE = Regex("\\s+")
    private val GROUP_CONTENT = Regex("\\s*(\\d+|\\*)\\s*(,\\s*-?\\d+\\s*)?(BYTE|CHAR)?\\s*")

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
     *
     * A built-in name is classified as the predefined type; callers holding the declaring package must still check
     * that no package member shadows it ([builtInName] gives the name to check).
     */
    fun ofDescription(description: ProjectExternalTypeDescription, startName: ProjectQualifiedName?, startReference: ProjectTypeReference?): ProjectDatatypeFamily? {
        if (isRowtypeReference(startReference)) return ProjectDatatypeFamily.COMPOSITE
        val source = description.source
        return when (description.status) {
            ProjectExternalTypeDescription.Status.NOT_EXTERNAL -> when (source.status) {
                ProjectTypeDescription.Status.BUILT_IN -> builtInName(description, startName)?.let { ofName(it.name) }
                ProjectTypeDescription.Status.DECLARED_TYPE -> {
                    val shape = (source.declaration.get() as? ProjectShapedTypeView)?.shape?.orElse(null)
                    if (shape == ProjectTypeShape.RECORD || shape == ProjectTypeShape.COLLECTION || shape == ProjectTypeShape.OBJECT) {
                        ProjectDatatypeFamily.COMPOSITE
                    } else null
                }
                else -> null
            }
            ProjectExternalTypeDescription.Status.RESOLVED -> when (description.anchor.orElse(null)) {
                ProjectTypeReference.Anchor.ROWTYPE -> ProjectDatatypeFamily.COMPOSITE
                ProjectTypeReference.Anchor.TYPE -> description.lookup.flatMap { it.column }.flatMap { it.datatype }.orElse(null)?.let { ofExternal(it) }
                null -> null
            }
            else -> null
        }
    }

    /** True when [reference] is a retained reference written with `%ROWTYPE`. */
    fun isRowtypeReference(reference: ProjectTypeReference?): Boolean =
        reference != null && reference.kind == ProjectTypeReference.Kind.ANCHORED &&
            reference.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE

    /** The built-in name a conclusive source walk stopped at, or null when the walk did not end at a built-in. */
    fun builtInName(description: ProjectExternalTypeDescription, startName: ProjectQualifiedName?): ProjectQualifiedName? {
        if (description.status != ProjectExternalTypeDescription.Status.NOT_EXTERNAL) return null
        val source = description.source
        if (source.status != ProjectTypeDescription.Status.BUILT_IN) return null
        return source.terminalReference.map { it.name }.orElse(null) ?: startName
    }

    /** The package whose scope resolved the built-in name: the owner of the last followed step, else [requestOwner]. */
    fun builtInScope(description: ProjectExternalTypeDescription, requestOwner: QualifiedName?): QualifiedName? =
        when (val last = description.source.steps.lastOrNull()) {
            is ProjectPackageValueView -> last.owner.name
            is ProjectSubtypeView -> last.owner.name
            null -> requestOwner
            else -> null
        }

    /**
     * True when [description] identifies a type: a source walk ending at a built-in or a declared type, or a resolved
     * external reading. Its family may still be unsupported (LONG, REF CURSOR, owned types).
     */
    fun isConclusive(description: ProjectExternalTypeDescription): Boolean = when (description.status) {
        ProjectExternalTypeDescription.Status.RESOLVED -> true
        ProjectExternalTypeDescription.Status.NOT_EXTERNAL -> description.source.status == ProjectTypeDescription.Status.BUILT_IN ||
            description.source.status == ProjectTypeDescription.Status.DECLARED_TYPE
        else -> false
    }

    private const val C = 'C'
    private const val X = 'X'
    private const val E = 'E'

    /**
     * Rows: actual family; columns: formal family, both in [ProjectDatatypeFamily] order. `C` compiles, `X` is rejected
     * (PLS-00306) for every measured member pair and parameter mode, `E` depends on PLSQL_IMPLICIT_CONVERSION_BOOL,
     * `U` is not decidable: interval day/second versus year/month is not retained, and composite identity is nominal
     * (a `%ROWTYPE` formal even accepts a same-shaped record).
     */
    private val matrix = arrayOf(
        //          NUM CHR CLB BLB RAW DTM INT BOO JSN RID CMP
        /* NUM */ "CCXXXXXEXXX",
        /* CHR */ "CCCXCCCEXCX",
        /* CLB */ "XCCXXXXXXXX",
        /* BLB */ "XXXCCXXXXXX",
        /* RAW */ "XCXCCXXXXXX",
        /* DTM */ "XCXXXCXXXXX",
        /* INT */ "XCXXXXUXXXX",
        /* BOO */ "EEXXXXXCXXX",
        /* JSN */ "XXXXXXXXCXX",
        /* RID */ "XCXXXXXXXCX",
        /* CMP */ "XXXXXXXXXXU"
    )

    init {
        val size = ProjectDatatypeFamily.entries.size
        check(matrix.size == size && matrix.all { it.length == size }) { "Malformed compatibility matrix" }
    }

    /** Compatibility of an actual of family [actual] with a formal of family [formal]; unclassified sides are UNKNOWN. */
    fun compatibility(actual: ProjectDatatypeFamily?, formal: ProjectDatatypeFamily?): DatatypeCompatibility {
        if (actual == null || formal == null) return DatatypeCompatibility.UNKNOWN
        return when (matrix[actual.ordinal][formal.ordinal]) {
            C -> DatatypeCompatibility.COMPATIBLE
            X -> DatatypeCompatibility.INCOMPATIBLE
            E -> DatatypeCompatibility.ENVIRONMENT_DEPENDENT
            else -> DatatypeCompatibility.UNKNOWN
        }
    }
}
