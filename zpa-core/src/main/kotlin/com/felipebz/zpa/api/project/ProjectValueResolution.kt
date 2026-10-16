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
 * Result of a classified lookup of one package variable or constant by its written name. It reports only what the
 * supplied, supported project index contains: [Status.NOT_FOUND_IN_PROJECT] is not proof that the declaration is
 * absent from Oracle or that source is invalid. Preparation failures take precedence over everything except an
 * unprepared snapshot, even when no candidate is known. [candidates] lists every known declaration with the effective
 * qualified name in index order, whatever its kind; only a [ProjectPackageValueView] is an eligible target. A package
 * exception or cursor with the name ([ProjectPackageExceptionView], [ProjectPackageCursorView]) is listed as a
 * candidate and leaves the status [Status.NOT_FOUND_IN_PROJECT].
 */
@ZpaExperimentalApi
class ProjectValueResolution private constructor(
    val status: Status,
    val name: ProjectQualifiedName,
    packageOwner: ProjectQualifiedName?,
    value: ProjectPackageValueView?,
    candidates: List<ProjectDeclarationView>,
    failures: List<ProjectPreparationFailure>
) {
    enum class Status {
        /** Exactly one known declaration has the name and it is a package value. */
        RESOLVED,

        /** At least one package value and more than one known declaration share the name. */
        AMBIGUOUS,

        /** No package value is known; non-value declarations with the name may be listed as candidates. */
        NOT_FOUND_IN_PROJECT,

        /** Some source failed preparation: no certain result, whatever is known. */
        INCOMPLETE_INDEX,

        /** The snapshot was not prepared. */
        NOT_PREPARED
    }

    /** The explicit package context supplied by the caller, if any (also kept when a qualified name overrides it). */
    val packageOwner: Optional<ProjectQualifiedName> = Optional.ofNullable(packageOwner)

    /** Present only for [Status.RESOLVED]. */
    val value: Optional<ProjectPackageValueView> = Optional.ofNullable(value)

    /** Runtime-unmodifiable known declarations with the effective qualified name, in index order. */
    val candidates: List<ProjectDeclarationView> = Collections.unmodifiableList(ArrayList(candidates))

    /** Runtime-unmodifiable preparation failures; nonempty only for [Status.INCOMPLETE_INDEX]. */
    val failures: List<ProjectPreparationFailure> = Collections.unmodifiableList(ArrayList(failures))

    init {
        val values = candidates.count { it is ProjectPackageValueView }
        when (status) {
            Status.RESOLVED -> require(
                value != null && candidates.size == 1 && candidates.single() === value && failures.isEmpty()
            ) { "Invalid RESOLVED result" }
            Status.AMBIGUOUS -> require(value == null && candidates.size > 1 && values > 0 && failures.isEmpty()) {
                "Invalid AMBIGUOUS result"
            }
            Status.NOT_FOUND_IN_PROJECT -> require(value == null && values == 0 && failures.isEmpty()) {
                "Invalid NOT_FOUND_IN_PROJECT result"
            }
            Status.INCOMPLETE_INDEX -> require(value == null && failures.isNotEmpty()) { "Invalid INCOMPLETE_INDEX result" }
            Status.NOT_PREPARED -> require(value == null && candidates.isEmpty() && failures.isEmpty()) {
                "Invalid NOT_PREPARED result"
            }
        }
    }

    internal companion object {
        @JvmSynthetic
        fun of(
            name: ProjectQualifiedName,
            packageOwner: ProjectQualifiedName?,
            candidates: List<ProjectDeclarationView>,
            failures: List<ProjectPreparationFailure>
        ): ProjectValueResolution = when {
            failures.isNotEmpty() -> ProjectValueResolution(Status.INCOMPLETE_INDEX, name, packageOwner, null, candidates, failures)
            candidates.none { it is ProjectPackageValueView } ->
                ProjectValueResolution(Status.NOT_FOUND_IN_PROJECT, name, packageOwner, null, candidates, failures)
            candidates.size == 1 ->
                ProjectValueResolution(Status.RESOLVED, name, packageOwner, candidates.single() as ProjectPackageValueView, candidates, failures)
            else -> ProjectValueResolution(Status.AMBIGUOUS, name, packageOwner, null, candidates, failures)
        }

        @JvmSynthetic
        fun notPrepared(name: ProjectQualifiedName, packageOwner: ProjectQualifiedName?) =
            ProjectValueResolution(Status.NOT_PREPARED, name, packageOwner, null, emptyList(), emptyList())
    }
}
