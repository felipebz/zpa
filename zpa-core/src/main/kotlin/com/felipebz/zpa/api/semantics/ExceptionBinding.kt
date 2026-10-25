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

/** The exception a name in a `RAISE` statement or a `WHEN` handler names, as the local name resolution decides it. */
@ZpaExperimentalApi
sealed interface ExceptionBinding {
    /** A user-defined exception: two names name the same exception when their [exception]s are equal. */
    class Declared(val exception: ExceptionDeclaration) : ExceptionBinding {
        override fun toString() = "Declared($exception)"
    }

    /** A predefined exception of package STANDARD (`NO_DATA_FOUND`…) that no declaration of the file redeclares. */
    class Predefined(val name: String) : ExceptionBinding {
        override fun equals(other: Any?) = other is Predefined && name == other.name

        override fun hashCode() = name.hashCode()

        override fun toString() = "Predefined($name)"
    }

    /** No declaration of the file and not predefined: a schema object's exception (`pkg.e`), or an undeclared name. */
    object NotLocal : ExceptionBinding {
        override fun toString() = "NotLocal"
    }

    /** The name binds something Oracle rejects here: another kind of declaration, a duplicate, a later declaration. */
    object Invalid : ExceptionBinding {
        override fun toString() = "Invalid"
    }

    /** Not decided; [reasons] says what the analysis could not see. */
    class Unknown(reasons: Set<UsageUncertainty>) : ExceptionBinding {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence: the file was not analyzed, or the node is not such a name. */
    class Unavailable(val reason: UsageUnavailability) : ExceptionBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/** Where a user-defined exception is declared. */
@ZpaExperimentalApi
enum class ExceptionScope {
    /** A block's declarations (an anonymous block, or a block inside a subprogram). */
    BLOCK,

    /** A subprogram's own declarations. */
    SUBPROGRAM,

    /** A trigger's declarations. */
    TRIGGER,

    PACKAGE_SPECIFICATION,
    PACKAGE_BODY
}

/**
 * A user-defined exception: equal for the same declaration. [line] and [column] are its name's position in this file;
 * both are null for a declaration in a package specification in another file.
 */
@ZpaExperimentalApi
class ExceptionDeclaration internal constructor(
    private val key: String,
    val scope: ExceptionScope,
    val line: Int?,
    val column: Int?,
    val errorCode: ExceptionErrorCode
) {
    override fun equals(other: Any?) = other is ExceptionDeclaration && key == other.key

    override fun hashCode() = key.hashCode()

    override fun toString() = "$scope" + (line?.let { "@$it:$column" } ?: "") + (if (errorCode == ExceptionErrorCode.None) "" else " $errorCode")
}

/**
 * Whether a `PRAGMA EXCEPTION_INIT` ties a user-defined exception to an error code. Oracle raises such an exception as
 * that error, and handlers match it by the code: any handler naming an exception with the same code, or the predefined
 * exception for it, catches it.
 */
@ZpaExperimentalApi
sealed interface ExceptionErrorCode {
    /** No pragma: raised as "User-Defined Exception" (`SQLCODE` 1), caught only by a handler naming this declaration. */
    object None : ExceptionErrorCode {
        override fun toString() = "None"
    }

    /** Tied to [code]; null when the code is not written as an integer literal. */
    class Associated(val code: Int?) : ExceptionErrorCode {
        override fun equals(other: Any?) = other is Associated && code == other.code

        override fun hashCode() = code.hashCode()

        override fun toString() = "Associated($code)"
    }

    /** A pragma may exist that the analysis cannot see; [reasons] says why. Never a proof of [None]. */
    class Unknown(reasons: Set<UsageUncertainty>) : ExceptionErrorCode {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }
}
