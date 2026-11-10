//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectTypedCallResolution

/**
 * What the file's local analysis decided for one call: the project's resolution, narrowed by the types of the written
 * actuals, of a call whose callee is not declared in the file, or the local resolution of a call of the file's own
 * subprograms. [KeptLocal] and [Unavailable] answers are never a proof.
 */
@ZpaExperimentalApi
sealed interface CallBinding {
    /**
     * The first segment of the name is proven not declared in the file: the call went to the project, which resolved
     * the written name and narrowed its candidates by the types of the actuals. [resolution] is that result as computed,
     * with its status, selected candidate, structural candidates and evaluations; a status other than
     * [ProjectTypedCallResolution.Status.RESOLVED] proves nothing about Oracle.
     */
    class Project(val resolution: ProjectTypedCallResolution) : CallBinding {
        override fun equals(other: Any?) = other is Project && resolution == other.resolution

        override fun hashCode() = resolution.hashCode()

        override fun toString() = "Project(${resolution.status})"
    }

    /**
     * The whole name names a subprogram set of the file (or a method set, called alone or through `SELF` in a type
     * body). [selected] is the subprogram the call binds, present only for [LocalCallStatus.RESOLVED]; [candidates]
     * holds one declaration per subprogram of the set, empty for [LocalCallStatus.INCOMPLETE] and
     * [LocalCallStatus.INVALID_NOTATION].
     */
    class Local(val status: LocalCallStatus, val selected: NameDeclaration?, candidates: List<NameDeclaration>) : CallBinding {
        val candidates: List<NameDeclaration> = candidates.toList()

        override fun equals(other: Any?) = other is Local && status == other.status && selected == other.selected && candidates == other.candidates

        override fun hashCode() = (status.hashCode() * 31 + selected.hashCode()) * 31 + candidates.hashCode()

        override fun toString() = "Local($status${selected?.let { " $it" } ?: ""})"
    }

    /** The call was neither sent to the project nor resolved as a call of the file's subprograms; [reason] says why. */
    class KeptLocal(val reason: CallKeptLocalReason) : CallBinding {
        override fun equals(other: Any?) = other is KeptLocal && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "KeptLocal($reason)"
    }

    /** No evidence: the file was not analyzed, or the node is not the name of a call the analysis covers. */
    class Unavailable(val reason: UsageUnavailability) : CallBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/** The local resolution of a call of the file's subprograms. */
@ZpaExperimentalApi
enum class LocalCallStatus {
    /**
     * One candidate receives the call (it may still have bindings whose types are not known), or several do, each
     * proven to, and Oracle prefers the selected one.
     */
    RESOLVED,

    /** The one candidate that fits is a static method, called on the instance: Oracle selects it and rejects the call. */
    STATIC_ON_INSTANCE,

    /** The one candidate that fits is a member method, called from a static method, which has no instance: Oracle rejects it. */
    MEMBER_FROM_STATIC,

    /** Several candidates fit the call and nothing tells which Oracle binds. */
    AMBIGUOUS,

    /** No candidate fits the call by number, names and modes of its actuals. */
    NO_APPLICABLE_CANDIDATE,

    /** Some candidate fits the call structurally, and each of them has an actual of a type it is proven not to accept. */
    NO_COMPATIBLE_CANDIDATE,

    /** A positional actual follows a named one, or a name is repeated: no candidate is evaluated. */
    INVALID_NOTATION,

    /**
     * The set was found through text the analysis cannot see (a `${'$'}IF`, a substitution) or a lookup it cannot complete:
     * a hidden subprogram may join it.
     */
    INCOMPLETE
}

/** Why a call was neither sent to the project nor resolved as a call of the file's subprograms. */
@ZpaExperimentalApi
enum class CallKeptLocalReason {
    /**
     * A declaration of the file takes the first segment, and the whole name is not a subprogram set of the file: an
     * element of a local collection (`v(1)`), a method of a local value (`o.m(x)`), or a name the local resolution
     * rejects.
     */
    LOCAL_CAPTURE,

    /** Text the analysis cannot see (a `${'$'}IF`, a substitution), or a lookup it cannot complete, may hide a declaration of the first segment. */
    INCOMPLETE,

    /** The first segment's outcome, or the call's form (a later argument list with several actuals, `f(1)(2, 3)`), is not modeled. */
    UNSUPPORTED
}
