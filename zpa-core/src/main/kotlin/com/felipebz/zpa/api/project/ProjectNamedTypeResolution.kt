//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * Result of a name-based project type lookup. It reports only what the supplied, supported
 * project index contains: [Status.NOT_FOUND_IN_PROJECT] is not proof that the type is absent
 * from Oracle or that source is invalid. Preparation failures take precedence over candidate
 * cardinality. Subtype bases, anchors and `REF` are not followed.
 */
@ZpaExperimentalApi
class ProjectNamedTypeResolution private constructor(
    val status: Status,
    val name: ProjectQualifiedName,
    packageOwner: ProjectQualifiedName?,
    declaration: ProjectTypeDeclarationView?,
    candidates: List<ProjectTypeDeclarationView>,
    failures: List<ProjectPreparationFailure>
) {
    enum class Status {
        RESOLVED,
        AMBIGUOUS,
        NOT_FOUND_IN_PROJECT,
        INCOMPLETE_INDEX,
        NOT_PREPARED
    }

    /** The explicit package lookup context supplied by the caller, if any. */
    val packageOwner: Optional<ProjectQualifiedName> = Optional.ofNullable(packageOwner)

    /** Present only for [Status.RESOLVED]. */
    val declaration: Optional<ProjectTypeDeclarationView> = Optional.ofNullable(declaration)

    /** Runtime-unmodifiable known matches in index order. */
    val candidates: List<ProjectTypeDeclarationView> = Collections.unmodifiableList(ArrayList(candidates))

    /** Runtime-unmodifiable preparation failures; nonempty only for [Status.INCOMPLETE_INDEX]. */
    val failures: List<ProjectPreparationFailure> = Collections.unmodifiableList(ArrayList(failures))

    init {
        when (status) {
            Status.RESOLVED -> require(declaration != null && candidates.size == 1 &&
                declaration.id == candidates.single().id && failures.isEmpty()) { "Invalid RESOLVED result" }
            Status.AMBIGUOUS -> require(declaration == null && candidates.size > 1 && failures.isEmpty()) {
                "Invalid AMBIGUOUS result"
            }
            Status.NOT_FOUND_IN_PROJECT, Status.NOT_PREPARED ->
                require(declaration == null && candidates.isEmpty() && failures.isEmpty()) { "Invalid $status result" }
            Status.INCOMPLETE_INDEX -> require(declaration == null && failures.isNotEmpty()) {
                "Invalid INCOMPLETE_INDEX result"
            }
        }
    }

    internal companion object {
        @JvmSynthetic
        fun of(
            name: ProjectQualifiedName,
            packageOwner: ProjectQualifiedName?,
            candidates: List<ProjectTypeDeclarationView>,
            failures: List<ProjectPreparationFailure>
        ): ProjectNamedTypeResolution = when {
            failures.isNotEmpty() ->
                ProjectNamedTypeResolution(Status.INCOMPLETE_INDEX, name, packageOwner, null, candidates, failures)
            candidates.isEmpty() ->
                ProjectNamedTypeResolution(Status.NOT_FOUND_IN_PROJECT, name, packageOwner, null, candidates, failures)
            candidates.size == 1 ->
                ProjectNamedTypeResolution(Status.RESOLVED, name, packageOwner, candidates.single(), candidates, failures)
            else -> ProjectNamedTypeResolution(Status.AMBIGUOUS, name, packageOwner, null, candidates, failures)
        }

        @JvmSynthetic
        fun notPrepared(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?) =
            ProjectNamedTypeResolution(Status.NOT_PREPARED, name, packageOwner, null, emptyList(), emptyList())
    }
}
