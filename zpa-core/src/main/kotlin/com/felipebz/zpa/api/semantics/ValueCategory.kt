//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectQualifiedName

/**
 * The category of the declared type of a named value, following subtypes and `%TYPE` anchors to where they end, as
 * the local name resolution and, for declarations in other files, the prepared project decide it.
 */
@ZpaExperimentalApi
sealed interface ValueCategory {
    /**
     * A record: a `%ROWTYPE` of a table, view or cursor, or a variable of a `RECORD` type. [relation] tells, for a
     * [RecordOrigin.ROWTYPE] record, the relation whose columns its fields follow, when the analysis knows it.
     * [selectList] is, for the row of a cursor's query (or of a cursor `FOR` loop's query), the select list its fields
     * follow; null when the record follows none the analysis read: a relation's columns, a `RECORD` type, a cursor's
     * `RETURN` type.
     */
    class Record(val origin: RecordOrigin, val relation: RecordRelation? = null, val selectList: RecordSelectList? = null) : ValueCategory {
        override fun equals(other: Any?) = other is Record && origin == other.origin && relation == other.relation && selectList == other.selectList

        override fun hashCode() = (origin.hashCode() * 31 + (relation?.hashCode() ?: 0)) * 31 + (selectList?.hashCode() ?: 0)

        override fun toString() = "Record($origin${relation?.let { ", $it" } ?: ""}${selectList?.let { ", $it" } ?: ""})"
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

    /** A value of a predefined scalar type; [family] is null when the analysis does not place the type in one. */
    class Scalar(val family: ScalarFamily?) : ValueCategory {
        override fun equals(other: Any?) = other is Scalar && family == other.family

        override fun hashCode() = family.hashCode()

        override fun toString() = "Scalar(${family ?: "?"})"
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

/** The family of a predefined scalar type. */
@ZpaExperimentalApi
enum class ScalarFamily {
    /** NUMBER, PLS_INTEGER, BINARY_FLOAT, BINARY_DOUBLE and their predefined subtypes. */
    NUMERIC,

    /** VARCHAR2, CHAR, NVARCHAR2, NCHAR and their predefined subtypes. */
    CHARACTER,

    /** CLOB and NCLOB. */
    CLOB,

    BLOB,
    RAW,

    /** DATE and every TIMESTAMP variant. */
    DATETIME,

    /** INTERVAL DAY TO SECOND and INTERVAL YEAR TO MONTH. */
    INTERVAL,

    BOOLEAN,
    JSON,

    /** ROWID and UROWID. */
    ROWID
}

/** The relation a `%ROWTYPE` record's fields follow, in order. */
@ZpaExperimentalApi
sealed interface RecordRelation {
    /**
     * The relation written before `%ROWTYPE`, or the one a query selects all the columns of (`SELECT *` or `SELECT x.*`
     * from that relation alone): its columns, in its order. [name] is the name as written: a table, a view or a synonym,
     * or a package cursor the analysis cannot rule out (a package the project does not have, or one it cannot place in
     * the unit's schema).
     */
    class Named(val name: ProjectQualifiedName) : RecordRelation {
        override fun equals(other: Any?) = other is Named && name == other.name

        override fun hashCode() = name.hashCode()

        override fun toString() = "Named($name)"
    }

    /** The row of a query with its own select list: no relation's columns, whatever relations it reads. */
    object QueryColumns : RecordRelation {
        override fun toString() = "QueryColumns"
    }
}

/**
 * The select list a cursor row's fields follow, in order: one [items] entry per select item, named as Oracle names the
 * field (a column's name, or the alias). A `*`, a `t.*` and an expression without an alias give an item without a name:
 * the record may have fields this does not list. [complete] when every item has a name: the record has exactly those
 * fields. Oracle rejects a record whose select list repeats a name (PLS-00402), while the cursor itself compiles.
 */
@ZpaExperimentalApi
class RecordSelectList internal constructor(items: List<SelectListItem>) {
    val items: List<SelectListItem> = java.util.List.copyOf(items)

    val complete: Boolean get() = items.all { it.name != null }

    override fun equals(other: Any?) = other is RecordSelectList && items == other.items

    override fun hashCode() = items.hashCode()

    override fun toString() = items.joinToString(", ", "[", "]") { it.name ?: "?" }
}

/**
 * One item of a [RecordSelectList]: the field [name] Oracle gives it (an unquoted name in upper case, a quoted alias with
 * its spelling), null when not known, and the [declaration] [FieldBinding.Field] gives for it.
 */
@ZpaExperimentalApi
class SelectListItem internal constructor(val name: String?, val declaration: FieldDeclaration) {
    override fun equals(other: Any?) = other is SelectListItem && name == other.name && declaration == other.declaration

    override fun hashCode() = (name?.hashCode() ?: 0) * 31 + declaration.hashCode()

    override fun toString() = "${name ?: "?"}=$declaration"
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
