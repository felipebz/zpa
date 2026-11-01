//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
