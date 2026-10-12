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
    val arguments: List<ProjectCallArgument> = Collections.unmodifiableList(ArrayList(arguments))

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
