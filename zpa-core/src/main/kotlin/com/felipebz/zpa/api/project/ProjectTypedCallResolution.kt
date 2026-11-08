//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * A structural call resolution narrowed by datatype evidence ([ProjectSnapshot.narrowCall]). Narrowing removes
 * candidates whose bindings are proven incompatible; among candidates all proven compatible, it picks the one Oracle
 * prefers only when the exact types of the actuals and formals decide it. Source order never chooses.
 *
 * - [Status.RESOLVED]: the structural discovery was certain (structural RESOLVED or AMBIGUOUS) and every other
 *   applicable candidate is proven incompatible, or ([ranked]) every retained candidate is proven to receive the call
 *   and Oracle's preference among them, read from the exact types of the actuals and formals, picks one. The survivor
 *   may still have unknown bindings when it is the only one: RESOLVED does not assert that Oracle compiles or executes
 *   the call, only that no other known candidate can receive it, or would be chosen over it.
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
    private val selectedCandidate: ProjectCallCandidate?,
    /**
     * True when [Status.RESOLVED] was chosen among several retained candidates: the others are proven able to receive the
     * call too, and Oracle prefers [selected] to each of them (see [ProjectSnapshot.narrowCall]).
     */
    val ranked: Boolean = false
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
            Status.RESOLVED -> require(
            retained.any { it.candidate === selectedCandidate } && (retained.size == 1) != ranked
        ) { "Invalid RESOLVED narrowing" }
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
        fun of(
            structural: ProjectCallResolution,
            evaluations: List<ProjectCandidateCompatibility>,
            preferred: ProjectCallCandidate? = null
        ): ProjectTypedCallResolution {
            val certain = structural.status == ProjectCallResolution.Status.RESOLVED || structural.status == ProjectCallResolution.Status.AMBIGUOUS
            val retained = evaluations.filter { it.outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
            val ranked = certain && retained.size > 1 && preferred != null
            val status = when {
                !certain -> Status.NOT_NARROWED
                retained.size == 1 || ranked -> Status.RESOLVED
                retained.size > 1 -> Status.AMBIGUOUS
                else -> Status.NO_COMPATIBLE_CANDIDATE
            }
            val selected = if (ranked) preferred else retained.singleOrNull()?.candidate?.takeIf { status == Status.RESOLVED }
            return ProjectTypedCallResolution(status, structural, evaluations, selected, ranked)
        }
    }
}
