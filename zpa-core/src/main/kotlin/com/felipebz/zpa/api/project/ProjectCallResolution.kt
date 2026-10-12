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
import com.felipebz.zpa.project.ProjectCallResolution as InternalCallResolution
import com.felipebz.zpa.project.ProjectCallUncertainty as InternalCallUncertainty

/**
 * Structural call resolution: discovery (as [ProjectCallCandidates]) plus kind and actual-to-formal association for
 * every candidate. Datatypes, actual expressions, lexical scope and the catalog are never consulted; equal-arity
 * overloads that differ only by datatype stay [Status.AMBIGUOUS]. [selected] is present only for [Status.RESOLVED],
 * and it is the very object found in [candidates]. All collections are runtime-unmodifiable.
 */
@ZpaExperimentalApi
class ProjectCallResolution private constructor(
    val status: Status,
    val target: ProjectCallTarget,
    val kind: ProjectCallKind,
    val arguments: ProjectCallArguments,
    candidates: List<ProjectCallCandidate>,
    uncertainties: Collection<ProjectCallUncertainty>,
    packageSpecifications: List<ProjectDeclarationView>,
    private val selectedCandidate: ProjectCallCandidate?,
    failures: List<ProjectPreparationFailure>
) {
    /**
     * In precedence order: INVALID_NOTATION, NOT_PREPARED, INCOMPLETE_INDEX, AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS,
     * UNKNOWN_PACKAGE, NOT_FOUND_IN_PROJECT, WRONG_KIND, then NO_APPLICABLE_CANDIDATE / RESOLVED / AMBIGUOUS.
     */
    enum class Status {
        RESOLVED, AMBIGUOUS, NO_APPLICABLE_CANDIDATE, WRONG_KIND, NOT_FOUND_IN_PROJECT,
        UNKNOWN_PACKAGE, AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS, INCOMPLETE_INDEX,
        INVALID_NOTATION, NOT_PREPARED
    }

    /** Every discovered public candidate with its evaluation, in discovery order; never ranked. */
    val candidates: List<ProjectCallCandidate> = Collections.unmodifiableList(ArrayList(candidates))

    /** As in [ProjectCallCandidates.uncertainties]. */
    val uncertainties: Set<ProjectCallUncertainty> =
        Collections.unmodifiableSet(EnumSet.noneOf(ProjectCallUncertainty::class.java).apply { addAll(uncertainties) })

    val packageSpecifications: List<ProjectDeclarationView> = Collections.unmodifiableList(ArrayList(packageSpecifications))

    /** Nonempty exactly for [Status.INCOMPLETE_INDEX]. */
    val failures: List<ProjectPreparationFailure> = Collections.unmodifiableList(ArrayList(failures))

    /** Present only for [Status.RESOLVED]. */
    val selected: Optional<ProjectCallCandidate> get() = Optional.ofNullable(selectedCandidate)

    init {
        require(this.failures.isEmpty() != (status == Status.INCOMPLETE_INDEX)) {
            "Failures are present exactly for INCOMPLETE_INDEX"
        }
        require((selectedCandidate != null) == (status == Status.RESOLVED)) { "A candidate is selected exactly for RESOLVED" }
        require(selectedCandidate == null ||
            (this.candidates.any { it === selectedCandidate } && selectedCandidate.applicability == ProjectCallCandidate.Applicability.APPLICABLE)) {
            "The selected candidate is an APPLICABLE member of the candidates"
        }
        require((status == Status.INVALID_NOTATION) == (arguments.notation != ProjectCallArguments.Notation.VALID)) {
            "INVALID_NOTATION exactly for an invalid request notation"
        }
        require((status != Status.NOT_PREPARED && status != Status.INVALID_NOTATION) ||
            (this.candidates.isEmpty() && this.uncertainties.isEmpty() && this.packageSpecifications.isEmpty())) {
            "NOT_PREPARED and INVALID_NOTATION results are empty"
        }
    }

    internal companion object {
        /**
         * Projects the binder's result. Candidate declarations become one batch of snapshot views; each internal
         * candidate maps onto the view at the same position, and the selection onto the projected candidate at the
         * position of the internal selection. [failures] is the snapshot's projection of the binder's failures.
         */
        @JvmSynthetic
        fun of(
            result: InternalCallResolution,
            target: ProjectCallTarget,
            kind: ProjectCallKind,
            arguments: ProjectCallArguments,
            scope: ProjectSnapshotScope,
            failures: List<ProjectPreparationFailure>
        ): ProjectCallResolution {
            val views = ProjectSnapshotDeclarations.views(scope, result.candidates.map { it.declaration })
            val candidates = result.candidates.mapIndexed { index, candidate ->
                ProjectCallCandidate.of(candidate, views[index] as ProjectSubprogramView, arguments)
            }
            val selectedIndex = result.candidates.indexOfFirst { it === result.selected }
            return ProjectCallResolution(
                when (result.status) {
                    InternalCallResolution.Status.RESOLVED -> Status.RESOLVED
                    InternalCallResolution.Status.AMBIGUOUS -> Status.AMBIGUOUS
                    InternalCallResolution.Status.NO_APPLICABLE_CANDIDATE -> Status.NO_APPLICABLE_CANDIDATE
                    InternalCallResolution.Status.WRONG_KIND -> Status.WRONG_KIND
                    InternalCallResolution.Status.NOT_FOUND_IN_PROJECT -> Status.NOT_FOUND_IN_PROJECT
                    InternalCallResolution.Status.UNKNOWN_PACKAGE -> Status.UNKNOWN_PACKAGE
                    InternalCallResolution.Status.AMBIGUOUS_TARGET -> Status.AMBIGUOUS_TARGET
                    InternalCallResolution.Status.PARTIAL_DECLARATIONS -> Status.PARTIAL_DECLARATIONS
                    InternalCallResolution.Status.INCOMPLETE_INDEX -> Status.INCOMPLETE_INDEX
                    InternalCallResolution.Status.INVALID_NOTATION -> Status.INVALID_NOTATION
                    InternalCallResolution.Status.NOT_PREPARED -> Status.NOT_PREPARED
                },
                target,
                kind,
                arguments,
                candidates,
                result.uncertainties.map {
                    when (it) {
                        InternalCallUncertainty.NAMESPACE_COMPETITION -> ProjectCallUncertainty.NAMESPACE_COMPETITION
                        InternalCallUncertainty.DUPLICATE_SPECIFICATION -> ProjectCallUncertainty.DUPLICATE_SPECIFICATION
                        InternalCallUncertainty.PARTIAL_DECLARATIONS -> ProjectCallUncertainty.PARTIAL_DECLARATIONS
                    }
                },
                ProjectSnapshotDeclarations.views(scope, result.packageSpecifications),
                candidates.getOrNull(selectedIndex),
                // INVALID_NOTATION is decided before the project is consulted and carries no failures.
                if (result.failures.isEmpty()) emptyList() else failures
            )
        }
    }
}
