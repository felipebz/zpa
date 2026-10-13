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
 * A structural call resolution narrowed by datatype evidence ([ProjectSnapshot.narrowCall]). Narrowing only removes
 * candidates whose bindings are proven incompatible; compatible candidates are never ranked and source order never
 * chooses.
 *
 * - [Status.RESOLVED]: the structural discovery was certain (structural RESOLVED or AMBIGUOUS) and every other
 *   applicable candidate is proven incompatible. The survivor may still have unknown bindings: RESOLVED does not assert
 *   that Oracle compiles or executes the call, only that no other known candidate can receive it.
 * - [Status.AMBIGUOUS]: several applicable candidates survive.
 * - [Status.NO_COMPATIBLE_CANDIDATE]: every applicable candidate of a certain discovery has an incompatible binding
 *   for the supplied evidence. This is not a claim that Oracle rejects the source.
 * - [Status.NOT_NARROWED]: the structural status is neither RESOLVED nor AMBIGUOUS (incomplete or partial sources,
 *   ambiguous target, invalid notation, …); evaluations are still reported for applicable candidates as evidence.
 */
@ZpaExperimentalApi
class ProjectTypedCallResolution private constructor(
    val status: Status,
    /** The structural resolution passed to [ProjectSnapshot.narrowCall], the very same object. */
    val structural: ProjectCallResolution,
    evaluations: List<ProjectCandidateCompatibility>,
    private val selectedCandidate: ProjectCallCandidate?
) {
    enum class Status { RESOLVED, AMBIGUOUS, NO_COMPATIBLE_CANDIDATE, NOT_NARROWED }

    /** One per structurally applicable candidate, in structural order. Runtime-unmodifiable. */
    val evaluations: List<ProjectCandidateCompatibility> = Collections.unmodifiableList(ArrayList(evaluations))

    /** Present only for [Status.RESOLVED]: a retained candidate, the same object as in the structural result. */
    val selected: Optional<ProjectCallCandidate> get() = Optional.ofNullable(selectedCandidate)

    init {
        val applicable = structural.candidates.filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
        require(this.evaluations.size == applicable.size && this.evaluations.indices.all { this.evaluations[it].candidate === applicable[it] }) {
            "One evaluation per applicable structural candidate, in order"
        }
        val certain = structural.status == ProjectCallResolution.Status.RESOLVED || structural.status == ProjectCallResolution.Status.AMBIGUOUS
        require((status == Status.NOT_NARROWED) == !certain) { "NOT_NARROWED exactly when the structural status is not RESOLVED or AMBIGUOUS" }
        val retained = this.evaluations.filter { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
        when (status) {
            Status.RESOLVED -> require(retained.size == 1 && retained.single().candidate === selectedCandidate) { "Invalid RESOLVED narrowing" }
            Status.AMBIGUOUS -> require(retained.size > 1 && selectedCandidate == null) { "Invalid AMBIGUOUS narrowing" }
            Status.NO_COMPATIBLE_CANDIDATE -> require(this.evaluations.isNotEmpty() && retained.isEmpty() && selectedCandidate == null) {
                "Invalid NO_COMPATIBLE_CANDIDATE narrowing"
            }
            Status.NOT_NARROWED -> require(selectedCandidate == null) { "NOT_NARROWED never selects" }
        }
    }

    override fun toString(): String = "$status (structural ${structural.status}, ${evaluations.size} evaluated)"

    internal companion object {
        @JvmSynthetic
        fun of(structural: ProjectCallResolution, evaluations: List<ProjectCandidateCompatibility>): ProjectTypedCallResolution {
            val certain = structural.status == ProjectCallResolution.Status.RESOLVED || structural.status == ProjectCallResolution.Status.AMBIGUOUS
            val retained = evaluations.filter { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
            val status = when {
                !certain -> Status.NOT_NARROWED
                retained.size == 1 -> Status.RESOLVED
                retained.size > 1 -> Status.AMBIGUOUS
                else -> Status.NO_COMPATIBLE_CANDIDATE
            }
            return ProjectTypedCallResolution(status, structural, evaluations, retained.singleOrNull()?.candidate?.takeIf { status == Status.RESOLVED })
        }
    }
}
