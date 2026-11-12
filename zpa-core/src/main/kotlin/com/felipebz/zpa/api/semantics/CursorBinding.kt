//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectPackageCursorView
import java.util.*

/**
 * The explicit cursor a name denotes where Oracle reads one (`OPEN`, `FETCH`, `CLOSE`, a cursor `FOR` loop, a
 * `%ROWTYPE` anchor), and, for `OPEN` and the `FOR` loop, how the written actuals bind to the cursor's parameters.
 * [Unknown] and [Unavailable] are never a proof.
 */
@ZpaExperimentalApi
sealed interface CursorBinding {
    /**
     * A cursor declared in this file, its name placed by [declaration]. [arguments] binds the actuals of `OPEN` and of
     * the `FOR` loop (no parenthesized list binds none); null for `FETCH`, `CLOSE` and `%ROWTYPE`, which pass none.
     */
    class Local(val declaration: NameDeclaration, val arguments: CursorArguments?) : CursorBinding {
        override fun equals(other: Any?) = other is Local && declaration == other.declaration && arguments == other.arguments

        override fun hashCode() = declaration.hashCode() * 31 + (arguments?.hashCode() ?: 0)

        override fun toString() = "Local($declaration${arguments?.let { ", $it" } ?: ""})"
    }

    /**
     * A cursor of a package specification in another project file: [cursor], named through its package (`pk.c`), or
     * alone in the body of that package. [arguments] as for [Local].
     */
    class Project(val cursor: ProjectPackageCursorView, val arguments: CursorArguments?) : CursorBinding {
        override fun equals(other: Any?) = other is Project && cursor.id == other.cursor.id && arguments == other.arguments

        override fun hashCode() = cursor.id.hashCode() * 31 + (arguments?.hashCode() ?: 0)

        override fun toString() = "Project(${cursor.qualifiedName}${arguments?.let { ", $it" } ?: ""})"
    }

    /**
     * Not decided; [reasons] says what the analysis could not see: a cursor the project does not hold, text the file
     * may hold differently, a project that names several cursors.
     */
    class Unknown(reasons: Set<UsageUncertainty>) : CursorBinding {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /**
     * No cursor: the node is not such a name, the name is proven something else (a cursor variable, a relation of a
     * `%ROWTYPE`, a package variable), or the file was not analyzed.
     */
    class Unavailable(val reason: UsageUnavailability) : CursorBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/**
 * How the written actuals of one cursor invocation bind to the cursor's parameters, as Oracle associates the actuals of
 * a call: positional ones first, then named ones, and a parameter with a default may be left out. Datatypes are not
 * compared.
 */
@ZpaExperimentalApi
class CursorArguments internal constructor(
    val status: Status,
    bindings: List<CursorArgumentBinding>,
    omitted: List<CursorParameter>,
    missing: List<CursorParameter>,
    private val rejected: Int?
) {
    enum class Status {
        /** Every actual binds a parameter, and every parameter without a default has one. */
        BOUND,

        /** More positional actuals than parameters (Oracle: PLS-00306). */
        TOO_MANY_ARGUMENTS,

        /** A named actual names no parameter (Oracle: PLS-00306). */
        UNKNOWN_LABEL,

        /** A named actual names a parameter a positional actual already binds. */
        DUPLICATE_BINDING,

        /** A parameter without a default has no actual (Oracle: PLS-00306). */
        MISSING_REQUIRED,

        /** The cursor declares two parameters of one name. */
        INVALID_SIGNATURE,

        /** A positional actual follows a named one, or a name is repeated: nothing is bound. */
        INVALID_NOTATION
    }

    /** [Status.BOUND] only: one binding per actual, in written order. */
    val bindings: List<CursorArgumentBinding> = java.util.List.copyOf(bindings)

    /** [Status.BOUND] only: the parameters with a default that no actual binds, in declaration order. */
    val omittedParameters: List<CursorParameter> = java.util.List.copyOf(omitted)

    /** [Status.MISSING_REQUIRED] only: the parameters without a default and without an actual, in declaration order. */
    val missingParameters: List<CursorParameter> = java.util.List.copyOf(missing)

    /**
     * [Status.TOO_MANY_ARGUMENTS], [Status.UNKNOWN_LABEL], [Status.DUPLICATE_BINDING] and [Status.INVALID_NOTATION]
     * only: the first offending actual, one-based.
     */
    val rejectedArgumentOrdinal: OptionalInt get() = rejected?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    override fun equals(other: Any?) = other is CursorArguments && status == other.status && bindings == other.bindings &&
        omittedParameters == other.omittedParameters && missingParameters == other.missingParameters && rejected == other.rejected

    override fun hashCode() = listOf(status, bindings, omittedParameters, missingParameters, rejected).hashCode()

    override fun toString() = when (status) {
        Status.BOUND -> "BOUND$bindings" + if (omittedParameters.isEmpty()) "" else " omitted $omittedParameters"
        Status.MISSING_REQUIRED -> "MISSING_REQUIRED $missingParameters"
        else -> status.name + (rejected?.let { " #$it" } ?: "")
    }
}

/** The actual [argumentOrdinal] (one-based, written order) bound to [parameter]. */
@ZpaExperimentalApi
class CursorArgumentBinding internal constructor(val argumentOrdinal: Int, val parameter: CursorParameter) {
    override fun equals(other: Any?) = other is CursorArgumentBinding && argumentOrdinal == other.argumentOrdinal && parameter == other.parameter

    override fun hashCode() = argumentOrdinal * 31 + parameter.hashCode()

    override fun toString() = "#$argumentOrdinal->$parameter"
}

/**
 * A parameter of a cursor: its [name] as written, [ordinal] one-based, whether it has a default, and its position: the
 * name's in this file ([fileId] null), the start of the parameter in another project file [fileId].
 */
@ZpaExperimentalApi
class CursorParameter internal constructor(
    val name: String,
    val ordinal: Int,
    val hasDefault: Boolean,
    val line: Int,
    val column: Int,
    val fileId: String? = null
) {
    override fun equals(other: Any?) = other is CursorParameter && name == other.name && ordinal == other.ordinal &&
        hasDefault == other.hasDefault && line == other.line && column == other.column && fileId == other.fileId

    override fun hashCode() = listOf(name, ordinal, hasDefault, line, column, fileId).hashCode()

    override fun toString() = "$name#$ordinal"
}
