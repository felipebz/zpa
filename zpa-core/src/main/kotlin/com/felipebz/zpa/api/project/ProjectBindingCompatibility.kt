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

/**
 * Datatype comparison of one structural binding: the actual's evidence against the formal's described type. Only
 * [Outcome.INCOMPATIBLE] removes a candidate. [Outcome.COMPATIBLE] means a single procedure with this pairing compiles
 * on Oracle; it implies no overload preference and says nothing about runtime conversions.
 */
@ZpaExperimentalApi
class ProjectBindingCompatibility private constructor(
    /** The structural binding, the same object as in [ProjectCallCandidate.bindings]. */
    val binding: ProjectArgumentBinding,
    val outcome: Outcome,
    actualFamily: ProjectDatatypeFamily?,
    formalFamily: ProjectDatatypeFamily?,
    unknownReason: UnknownReason?
) {
    enum class Outcome { COMPATIBLE, INCOMPATIBLE, UNKNOWN }

    enum class UnknownReason {
        /** The actual has no usable evidence. */
        ACTUAL_UNKNOWN,

        /** The actual is the `NULL` literal, accepted by every formal type. */
        NULL_LITERAL,

        /** The formal's type description is inconclusive (sources, metadata, conditional compilation, shadowing). */
        FORMAL_INCONCLUSIVE,

        /** The pairing depends on the PLSQL_IMPLICIT_CONVERSION_BOOL setting of the calling unit. */
        ENVIRONMENT_DEPENDENT,

        /** Both families are known but the pairing is not decidable (interval kinds, composite identity). */
        UNSUPPORTED_PAIR,

        /** A side names a type outside the supported families (for example LONG, REF CURSOR, owned types). */
        UNCLASSIFIED_TYPE
    }

    /** The actual's family, when classified. */
    val actualFamily: Optional<ProjectDatatypeFamily> = Optional.ofNullable(actualFamily)

    /** The formal's family, when classified. */
    val formalFamily: Optional<ProjectDatatypeFamily> = Optional.ofNullable(formalFamily)

    /** Present exactly for [Outcome.UNKNOWN]. */
    val unknownReason: Optional<UnknownReason> = Optional.ofNullable(unknownReason)

    init {
        require((outcome == Outcome.UNKNOWN) == (unknownReason != null)) { "An unknown reason is present exactly for UNKNOWN" }
        require(outcome == Outcome.UNKNOWN || (actualFamily != null && formalFamily != null)) { "A decided outcome needs both families" }
    }

    override fun toString(): String =
        "$binding: $outcome${unknownReason.map { " ($it)" }.orElse("")} ${actualFamily.orElse(null)} -> ${formalFamily.orElse(null)}"

    internal companion object {
        @JvmSynthetic
        fun of(
            binding: ProjectArgumentBinding,
            outcome: Outcome,
            actualFamily: ProjectDatatypeFamily?,
            formalFamily: ProjectDatatypeFamily?,
            unknownReason: UnknownReason?
        ) = ProjectBindingCompatibility(binding, outcome, actualFamily, formalFamily, unknownReason)
    }
}
