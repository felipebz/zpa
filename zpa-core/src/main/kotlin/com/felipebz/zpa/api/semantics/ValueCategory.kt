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
package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * The category of the declared type of a named value, following subtypes and `%TYPE` anchors to where they end, as
 * the local name resolution and, for declarations in other files, the prepared project decide it.
 */
@ZpaExperimentalApi
sealed interface ValueCategory {
    /** A record: a `%ROWTYPE` of a table, view or cursor, or a variable of a `RECORD` type. */
    class Record(val origin: RecordOrigin) : ValueCategory {
        override fun equals(other: Any?) = other is Record && origin == other.origin

        override fun hashCode() = origin.hashCode()

        override fun toString() = "Record($origin)"
    }

    /**
     * A collection; [kind] is null when the declaration that would tell is not in the file. [element] is the category
     * of its elements.
     */
    class Collection(val kind: CollectionKind?, val element: ValueCategory) : ValueCategory {
        override fun equals(other: Any?) = other is Collection && kind == other.kind && element == other.element

        override fun hashCode() = (kind?.hashCode() ?: 0) * 31 + element.hashCode()

        override fun toString() = "Collection(${kind ?: "?"} of $element)"
    }

    /** An instance of an object type. */
    object ObjectInstance : ValueCategory {
        override fun toString() = "ObjectInstance"
    }

    /** A cursor variable (`REF CURSOR`, `SYS_REFCURSOR`). */
    object CursorVariable : ValueCategory {
        override fun toString() = "CursorVariable"
    }

    /** A value of a predefined scalar type (numeric, character, date, boolean, LOB, JSON). */
    object Scalar : ValueCategory {
        override fun toString() = "Scalar"
    }

    /** Not decided; [reasons] says what the analysis could not see. Never a proof of any category. */
    class Unknown(reasons: Set<UsageUncertainty>) : ValueCategory {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence: the file was not analyzed, or the node is not such a name. */
    class Unavailable(val reason: UsageUnavailability) : ValueCategory {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/** Where a record's type comes from. */
@ZpaExperimentalApi
enum class RecordOrigin {
    /** `%ROWTYPE` of a table, view or cursor, or the record of a cursor `FOR` loop. */
    ROWTYPE,

    /** A `RECORD` type. */
    RECORD_TYPE
}

/** The kind of a collection type. */
@ZpaExperimentalApi
enum class CollectionKind {
    /** `TABLE OF … INDEX BY …`. */
    ASSOCIATIVE_ARRAY,

    /** `TABLE OF …`. */
    NESTED_TABLE,

    /** `VARRAY (n) OF …`. */
    VARRAY
}
