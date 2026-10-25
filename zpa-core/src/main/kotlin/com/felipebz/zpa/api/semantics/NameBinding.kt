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
 * What a plain name written in an expression (or the name of a call in a PL/SQL expression, or the cursor of `OPEN`,
 * `FETCH` or `CLOSE`) binds among the file's declarations, in PL/SQL or in SQL inside PL/SQL,
 * as the local name resolution decides it. Built-ins (`SYSDATE`, `USER`…) and schema objects are not declarations of
 * the file: a caller that knows one decides with [NothingLocal].
 */
@ZpaExperimentalApi
sealed interface NameBinding {
    /**
     * A declaration of the file named by the name; in SQL, no relation of the statement can have a column of that name
     * (the only relations are the `DUAL` table, and the name is not `DUMMY`).
     */
    class Local(val declaration: NameDeclaration) : NameBinding {
        override fun equals(other: Any?) = other is Local && declaration == other.declaration

        override fun hashCode() = declaration.hashCode()

        override fun toString() = "Local($declaration)"
    }

    /**
     * In SQL: a column of a relation in scope would take the name before [declaration], which binds it otherwise (a
     * declaration of the file, or of the package's specification in another project file). The analysis does not know
     * the relations' columns.
     */
    class ColumnOrLocal(val declaration: NameDeclaration) : NameBinding {
        override fun equals(other: Any?) = other is ColumnOrLocal && declaration == other.declaration

        override fun hashCode() = declaration.hashCode()

        override fun toString() = "ColumnOrLocal($declaration)"
    }

    /**
     * Proven: no declaration of the file is visible under the name. [sqlColumnPossible] is true in SQL when a relation
     * of the statement could have a column of that name (any relation but the `DUAL` table, or the name `DUMMY`).
     */
    class NothingLocal(val sqlColumnPossible: Boolean) : NameBinding {
        override fun equals(other: Any?) = other is NothingLocal && sqlColumnPossible == other.sqlColumnPossible

        override fun hashCode() = sqlColumnPossible.hashCode()

        override fun toString() = if (sqlColumnPossible) "NothingLocal(column possible)" else "NothingLocal"
    }

    /** Not decided; [reasons] says what the analysis could not see. Never a proof. */
    class Unknown(reasons: Set<UsageUncertainty>) : NameBinding {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence: the file was not analyzed, or the node is not such a name. */
    class Unavailable(val reason: UsageUnavailability) : NameBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/**
 * A declaration that a name binds: its kind and position. In this file ([fileId] null) the position is its name's; a
 * member of the package specification in another project file [fileId] is placed at the start of its declaration.
 */
@ZpaExperimentalApi
class NameDeclaration internal constructor(val kind: NameDeclarationKind, val line: Int, val column: Int, val fileId: String? = null) {
    override fun equals(other: Any?) = other is NameDeclaration && kind == other.kind && line == other.line && column == other.column &&
        fileId == other.fileId

    override fun hashCode() = ((kind.hashCode() * 31 + line) * 31 + column) * 31 + fileId.hashCode()

    override fun toString() = "$kind@" + (fileId?.let { "$it:" } ?: "") + "$line:$column"
}

/** The kind of a declaration a name binds. */
@ZpaExperimentalApi
enum class NameDeclarationKind {
    /** A variable or constant. */
    VARIABLE,

    /** A subprogram or cursor parameter. */
    PARAMETER,

    /** The index of a `FOR` loop, or the record of a cursor `FOR` loop. */
    LOOP_INDEX,

    /** A function: the name calls it without arguments. */
    FUNCTION,

    PROCEDURE,
    CURSOR,
    EXCEPTION,

    /** A subtype, a record, collection or ref cursor type, or an object type. */
    TYPE,

    /** An attribute of an object type. */
    ATTRIBUTE,

    /** A label, package, trigger or type body. */
    OTHER
}
