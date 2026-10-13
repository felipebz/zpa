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
 * Datatype evaluation of one structurally applicable candidate: one entry per structural binding, in written order.
 * The candidate is [Outcome.REJECTED] exactly when at least one binding is incompatible; unknown bindings never reject.
 */
@ZpaExperimentalApi
class ProjectCandidateCompatibility private constructor(
    /** The structural candidate, the same object as in [ProjectCallResolution.candidates]. */
    val candidate: ProjectCallCandidate,
    bindings: List<ProjectBindingCompatibility>
) {
    enum class Outcome { RETAINED, REJECTED }

    /** One per [ProjectCallCandidate.bindings] element, same order and binding objects. Runtime-unmodifiable. */
    val bindings: List<ProjectBindingCompatibility> = Collections.unmodifiableList(ArrayList(bindings))

    val outcome: Outcome =
        if (this.bindings.any { it.outcome == ProjectBindingCompatibility.Outcome.INCOMPATIBLE }) Outcome.REJECTED else Outcome.RETAINED

    init {
        require(candidate.applicability == ProjectCallCandidate.Applicability.APPLICABLE) { "Only applicable candidates are evaluated" }
        require(this.bindings.size == candidate.bindings.size && this.bindings.indices.all { this.bindings[it].binding === candidate.bindings[it] }) {
            "Evaluations must follow the candidate's own bindings"
        }
    }

    override fun toString(): String = "$outcome ${candidate.declaration.qualifiedName}"

    internal companion object {
        @JvmSynthetic
        fun of(candidate: ProjectCallCandidate, bindings: List<ProjectBindingCompatibility>) = ProjectCandidateCompatibility(candidate, bindings)
    }
}
