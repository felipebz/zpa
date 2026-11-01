//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * Result of correlating one package-body subprogram with specification declarations of the same
 * snapshot using the existing exact heading match (kind, owner, name, ordered parameter names,
 * modes, complete heading datatypes and function return; defaults, NOCOPY and DETERMINISTIC are
 * ignored). This is not call resolution. [Status.NOT_FOUND] does not prove the body is private:
 * the specification may be missing or outside current extraction coverage. Preparation failures
 * take precedence over candidate cardinality.
 */
@ZpaExperimentalApi
class ProjectSpecificationCorrelation private constructor(
    val status: Status,
    /** The selected body occurrence, always present. */
    val body: ProjectPackageSubprogramView,
    specification: ProjectPackageSubprogramView?,
    candidates: List<ProjectPackageSubprogramView>,
    failures: List<ProjectPreparationFailure>
) {
    enum class Status {
        RESOLVED,
        NOT_FOUND,
        AMBIGUOUS,
        INCOMPLETE
    }

    /** Present only for [Status.RESOLVED]. */
    val specification: Optional<ProjectPackageSubprogramView> = Optional.ofNullable(specification)

    /** Runtime-unmodifiable matching specification occurrences in index order, never deduplicated. */
    val candidates: List<ProjectPackageSubprogramView> = Collections.unmodifiableList(ArrayList(candidates))

    /** Runtime-unmodifiable preparation failures; nonempty only for [Status.INCOMPLETE]. */
    val failures: List<ProjectPreparationFailure> = Collections.unmodifiableList(ArrayList(failures))

    init {
        when (status) {
            Status.RESOLVED -> require(specification != null && candidates.size == 1 &&
                specification.id == candidates.single().id && failures.isEmpty()) { "Invalid RESOLVED result" }
            Status.NOT_FOUND -> require(specification == null && candidates.isEmpty() && failures.isEmpty()) {
                "Invalid NOT_FOUND result"
            }
            Status.AMBIGUOUS -> require(specification == null && candidates.size > 1 && failures.isEmpty()) {
                "Invalid AMBIGUOUS result"
            }
            Status.INCOMPLETE -> require(specification == null && failures.isNotEmpty()) {
                "Invalid INCOMPLETE result"
            }
        }
    }

    internal companion object {
        @JvmSynthetic
        fun of(
            body: ProjectPackageSubprogramView,
            candidates: List<ProjectPackageSubprogramView>,
            failures: List<ProjectPreparationFailure>
        ): ProjectSpecificationCorrelation = when {
            failures.isNotEmpty() -> ProjectSpecificationCorrelation(Status.INCOMPLETE, body, null, candidates, failures)
            candidates.isEmpty() -> ProjectSpecificationCorrelation(Status.NOT_FOUND, body, null, candidates, failures)
            candidates.size == 1 ->
                ProjectSpecificationCorrelation(Status.RESOLVED, body, candidates.single(), candidates, failures)
            else -> ProjectSpecificationCorrelation(Status.AMBIGUOUS, body, null, candidates, failures)
        }
    }
}
