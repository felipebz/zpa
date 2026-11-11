//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList
import java.util.*
import com.felipebz.zpa.project.ProjectCallArguments as InternalCallArguments

/**
 * Actual parameters in written order; an actual's ordinal is its one-based position in [arguments]. The notation is
 * classified once, here, and an invalid notation is data, never an exception. Classification is the project's own
 * (`ProjectCallArguments` in the internal engine); this class only exposes it.
 */
@ZpaExperimentalApi
class ProjectCallArguments private constructor(arguments: List<ProjectCallArgument>) {
    enum class Notation { VALID, POSITIONAL_AFTER_NAMED, DUPLICATE_LABEL }

    /** Runtime-unmodifiable copy, in written order. */
    val arguments: List<ProjectCallArgument> = immutableList(arguments)

    @get:JvmSynthetic
    internal val internal: InternalCallArguments = InternalCallArguments.of(this.arguments.map { it.internal })

    val notation: Notation
        get() = when (internal.notation) {
            InternalCallArguments.Notation.VALID -> Notation.VALID
            InternalCallArguments.Notation.POSITIONAL_AFTER_NAMED -> Notation.POSITIONAL_AFTER_NAMED
            InternalCallArguments.Notation.DUPLICATE_LABEL -> Notation.DUPLICATE_LABEL
        }

    /** One-based ordinal of the first offending actual; present iff [notation] is not VALID. */
    val invalidArgumentOrdinal: OptionalInt
        get() = internal.invalidArgumentOrdinal?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    override fun toString(): String = arguments.joinToString(prefix = "(", postfix = ")")

    companion object {
        /** No actuals: `p.f` or `p.f()`. */
        @JvmStatic
        fun none(): ProjectCallArguments = ProjectCallArguments(emptyList())

        /** Defensively copies [arguments]; never throws for notation errors. */
        @JvmStatic
        fun of(arguments: List<ProjectCallArgument>): ProjectCallArguments = ProjectCallArguments(arguments)
    }
}
