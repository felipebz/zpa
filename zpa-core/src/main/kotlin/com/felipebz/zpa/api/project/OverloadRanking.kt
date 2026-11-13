//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.QualifiedName

/**
 * The exact built-in type a value or a formal has, as far as Oracle's choice among compatible overloads tells types
 * apart. Predefined subtypes rank as the type they are declared from in STANDARD or DBMS_STANDARD (INTEGER as NUMBER,
 * NATURAL as PLS_INTEGER, CHAR and NVARCHAR2 as VARCHAR2, NCLOB as CLOB); Oracle cannot tell a subtype from its base
 * when choosing (PLS-00307 between them).
 */
internal enum class OverloadRank(val group: Group) {
    NUMBER(Group.NUMERIC),
    PLS_INTEGER(Group.NUMERIC),
    BINARY_FLOAT(Group.NUMERIC),
    BINARY_DOUBLE(Group.NUMERIC),
    VARCHAR2(Group.CHARACTER),

    /** RAW is declared in STANDARD as a subtype of VARCHAR2: Oracle cannot tell the two apart. */
    RAW(Group.CHARACTER),
    CLOB(Group.CLOB),
    BLOB(Group.BLOB),
    DATE(Group.DATE),
    TIMESTAMP(Group.TIMESTAMP),
    TIMESTAMP_TZ(Group.TIMESTAMP_TZ),
    TIMESTAMP_LTZ(Group.TIMESTAMP_LTZ),
    INTERVAL_DS(Group.INTERVAL_DS),
    INTERVAL_YM(Group.INTERVAL_YM),
    BOOLEAN(Group.BOOLEAN);

    /** Types that one another's values prefer over every type of another group. */
    enum class Group { NUMERIC, CHARACTER, CLOB, BLOB, DATE, TIMESTAMP, TIMESTAMP_TZ, TIMESTAMP_LTZ, INTERVAL_DS, INTERVAL_YM, BOOLEAN }

    companion object {
        private val names: Map<String, OverloadRank> = buildMap {
            fun put(rank: OverloadRank, vararg names: String) = names.forEach { put(it, rank) }
            put(NUMBER, "NUMBER", "INTEGER", "INT", "SMALLINT", "DEC", "DECIMAL", "NUMERIC", "FLOAT", "REAL", "DOUBLE")
            put(PLS_INTEGER, "PLS_INTEGER", "BINARY_INTEGER", "NATURAL", "NATURALN", "POSITIVE", "POSITIVEN", "SIGNTYPE", "SIMPLE_INTEGER")
            put(BINARY_FLOAT, "BINARY_FLOAT", "SIMPLE_FLOAT")
            put(BINARY_DOUBLE, "BINARY_DOUBLE", "SIMPLE_DOUBLE")
            put(
                VARCHAR2, "VARCHAR2", "VARCHAR", "STRING", "CHAR", "CHARACTER", "NCHAR", "NVARCHAR2",
                "DBMS_ID", "DBMS_QUOTED_ID", "DBMS_ID_30", "DBMS_QUOTED_ID_30", "DBMS_ID_128", "DBMS_QUOTED_ID_128"
            )
            put(RAW, "RAW")
            put(CLOB, "CLOB", "NCLOB")
            put(BLOB, "BLOB")
            put(DATE, "DATE")
            put(TIMESTAMP, "TIMESTAMP_UNCONSTRAINED")
            put(TIMESTAMP_TZ, "TIMESTAMP_TZ_UNCONSTRAINED")
            put(TIMESTAMP_LTZ, "TIMESTAMP_LTZ_UNCONSTRAINED")
            put(INTERVAL_DS, "DSINTERVAL_UNCONSTRAINED")
            put(INTERVAL_YM, "YMINTERVAL_UNCONSTRAINED")
            put(BOOLEAN, "BOOLEAN", "BOOL")
        }

        private val phrases: Map<String, OverloadRank> = mapOf(
            "TIMESTAMP" to TIMESTAMP,
            "TIMESTAMP WITH TIME ZONE" to TIMESTAMP_TZ,
            "TIMESTAMP WITH LOCAL TIME ZONE" to TIMESTAMP_LTZ,
            "INTERVAL DAY TO SECOND" to INTERVAL_DS,
            "INTERVAL YEAR TO MONTH" to INTERVAL_YM,
            "DOUBLE PRECISION" to NUMBER
        )

        /**
         * The rank of a built-in type written as [name], with the normalized tokens of the whole written type in
         * [writtenIdentity] when it is more than a plain name (a multi-word type, a precision): a TIMESTAMP or INTERVAL
         * name needs them to tell its variants apart.
         */
        fun of(name: QualifiedName, writtenIdentity: String?): OverloadRank? {
            val segment = name.segments.singleOrNull()?.takeUnless { it.quoted } ?: return null
            return when (segment.lookupName) {
                "TIMESTAMP" -> when {
                    writtenIdentity == null -> TIMESTAMP
                    writtenIdentity.contains(":LOCAL;") -> TIMESTAMP_LTZ
                    writtenIdentity.contains(":ZONE;") -> TIMESTAMP_TZ
                    else -> TIMESTAMP
                }
                "INTERVAL" -> when {
                    writtenIdentity == null -> null
                    writtenIdentity.contains(":YEAR;") -> INTERVAL_YM
                    writtenIdentity.contains(":DAY;") -> INTERVAL_DS
                    else -> null
                }
                else -> names[segment.lookupName]
            }
        }

        /** The rank of a built-in type spelled as text, as [ProjectArgumentType.builtIn] receives it; null when not known. */
        fun ofText(words: List<String>): OverloadRank? {
            if (words.size > 1) return phrases[words.joinToString(" ")]
            val word = words.singleOrNull() ?: return null
            // A lone INTERVAL names no type; a lone TIMESTAMP may be the first word of a longer one.
            return if (word == "TIMESTAMP" || word == "INTERVAL") null else names[word]
        }

        /**
         * The rank of a data dictionary `DATA_TYPE` with its parenthesized groups removed, as [words]: the dictionary
         * writes `TIMESTAMP(6)` for a timestamp without time zone, so a lone TIMESTAMP is that type there.
         */
        fun ofDictionaryText(words: List<String>): OverloadRank? =
            if (words.size == 1 && words[0] == "TIMESTAMP") phrases["TIMESTAMP"] else ofText(words)

        /** Within a group, the formals a value of [actual] prefers, best first; ties share a position. */
        private fun preference(actual: OverloadRank): List<Set<OverloadRank>> = when (actual) {
            PLS_INTEGER -> listOf(setOf(PLS_INTEGER), setOf(NUMBER), setOf(BINARY_FLOAT), setOf(BINARY_DOUBLE))
            NUMBER -> listOf(setOf(NUMBER), setOf(BINARY_FLOAT), setOf(BINARY_DOUBLE), setOf(PLS_INTEGER))
            BINARY_DOUBLE -> listOf(setOf(BINARY_DOUBLE), setOf(BINARY_FLOAT), setOf(NUMBER), setOf(PLS_INTEGER))
            BINARY_FLOAT -> listOf(setOf(BINARY_FLOAT), setOf(BINARY_DOUBLE), setOf(NUMBER), setOf(PLS_INTEGER))
            VARCHAR2, RAW -> listOf(setOf(VARCHAR2, RAW))
            else -> listOf(setOf(actual))
        }

        /** The position of [formal] in what a value of [actual] prefers; null when it is of another group. */
        private fun tier(actual: OverloadRank, formal: OverloadRank): Int? =
            if (actual.group != formal.group) null else preference(actual).indexOfFirst { formal in it }.takeIf { it >= 0 }

        /**
         * The candidate Oracle binds among several that each could receive the call, or null when it cannot be told.
         * [actuals] is the rank of each written actual; each candidate lists the formal rank each actual binds to, by
         * argument index. Every candidate passed must be proven able to receive the call.
         *
         * Oracle prefers a candidate whose every bound formal is of the same group as its actual is preferred to
         * any candidate with a formal of another group; among those, one wins when, on every actual where their formals
         * differ, its formal comes strictly first in the actual's preference. Otherwise (a candidate of another group
         * only, a formal or an actual without an exact type, two candidates each better on a different actual, formals
         * Oracle cannot tell apart) the call stays undecided: Oracle raises PLS-00307 there, or its choice is not known.
         */
        fun select(actuals: List<OverloadRank?>, candidates: List<List<OverloadRank?>>): Int? {
            if (candidates.size < 2 || actuals.any { it == null }) return null
            if (candidates.any { formals -> formals.size != actuals.size || formals.any { it == null } }) return null
            val same = candidates.indices.filter { c -> candidates[c].indices.all { tier(actuals[it]!!, candidates[c][it]!!) != null } }
            val winners = same.filter { x -> same.all { y -> y == x || beats(actuals, candidates[x], candidates[y]) } }
            return winners.singleOrNull()
        }

        private fun beats(actuals: List<OverloadRank?>, x: List<OverloadRank?>, y: List<OverloadRank?>): Boolean {
            var differs = false
            for (index in actuals.indices) {
                if (x[index] == y[index]) continue
                differs = true
                val actual = actuals[index]!!
                if (tier(actual, x[index]!!)!! >= tier(actual, y[index]!!)!!) return false
            }
            return differs
        }
    }
}

/**
 * What a composite value or formal is, where Oracle's compatibility between composites depends on it: a ref cursor
 * variable (weak or strong) is accepted by a SYS_REFCURSOR formal and rejected by a formal of a schema
 * object type, and a schema object value is rejected by a SYS_REFCURSOR formal. Two schema object types are never told
 * apart this way: a synonym or a subtype may relate two names.
 */
internal enum class CompositeKind {
    /** A ref cursor value; as a formal, SYS_REFCURSOR. */
    REF_CURSOR,

    /** A type created with CREATE TYPE, or a synonym of one. */
    SCHEMA_OBJECT
}

/**
 * A formal of a declared record or collection type accepts only a value of that same declared type: another declared
 * type with the same fields or element is rejected (PLS-00306). A `%ROWTYPE`, which Oracle compares field by field, an
 * object type and a ref cursor have none.
 */
internal sealed interface DeclaredCompositeType {
    @OptIn(ZpaExperimentalApi::class)
    data class Project(val id: ProjectDeclarationId) : DeclaredCompositeType

    /** A type the project does not hold (in a block, a subprogram, a body without index fact): distinct from every project type. */
    data class Local(val fileId: String, val ordinal: Int) : DeclaredCompositeType
}
