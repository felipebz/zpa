//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList
import java.util.*
import com.felipebz.zpa.project.ProjectCallCandidates as InternalCallCandidates
import com.felipebz.zpa.project.ProjectCallUncertainty as InternalCallUncertainty

/**
 * Call candidate discovery for one target. This is not call resolution: no actual argument is bound and nothing is
 * selected. The status reports the dominant state; [uncertainties] keeps every one that holds, also under
 * [Status.INCOMPLETE_INDEX]. A candidate count never implies target certainty. All collections are
 * runtime-unmodifiable.
 */
@ZpaExperimentalApi
class ProjectCallCandidates private constructor(
    val status: Status,
    val target: ProjectCallTarget,
    candidates: List<ProjectSubprogramView>,
    uncertainties: Collection<ProjectCallUncertainty>,
    packageSpecifications: List<ProjectDeclarationView>,
    failures: List<ProjectPreparationFailure>,
    synonyms: List<ProjectExternalSynonym> = emptyList(),
    synonymTarget: ProjectQualifiedName? = null
) {
    /**
     * In precedence order: NOT_PREPARED, INCOMPLETE_INDEX, AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS,
     * SYNONYM_TARGET_NOT_IN_PROJECT, UNKNOWN_PACKAGE, NOT_FOUND_IN_PROJECT, FOUND.
     */
    enum class Status {
        FOUND,
        NOT_FOUND_IN_PROJECT,
        UNKNOWN_PACKAGE,
        AMBIGUOUS_TARGET,
        PARTIAL_DECLARATIONS,
        INCOMPLETE_INDEX,
        NOT_PREPARED,

        /**
         * The first segment of the target is a synonym ([synonyms]) for an object the project does not hold: the
         * object [synonymTarget] (`DBMS_OUTPUT` a public synonym for `SYS.DBMS_OUTPUT`, whose source is not supplied),
         * an object in another database (a synonym over a database link), or none (a synonym that resolves to itself,
         * which Oracle rejects). Not a missing name.
         */
        SYNONYM_TARGET_NOT_IN_PROJECT
    }

    /**
     * The synonyms the target's first segment was read through, in order, when the snapshot follows synonyms
     * ([ProjectSnapshot.preferring] with metadata); empty when no synonym takes it. Runtime-unmodifiable.
     */
    val synonyms: List<ProjectExternalSynonym> = immutableList(synonyms)

    /** `[owner, name]` of the object the [synonyms] end at; empty when they end at a database link or a cycle. */
    val synonymTarget: Optional<ProjectQualifiedName> = Optional.ofNullable(synonymTarget)

    /** Package SPECIFICATION and STANDALONE procedures/functions in index order, never deduplicated or ranked. */
    val candidates: List<ProjectSubprogramView> = immutableList(candidates)

    val uncertainties: Set<ProjectCallUncertainty> =
        Collections.unmodifiableSet(EnumSet.noneOf(ProjectCallUncertainty::class.java).apply { addAll(uncertainties) })

    /** Retained specifications of every package interpretation considered, in index order. */
    val packageSpecifications: List<ProjectDeclarationView> =
        immutableList(packageSpecifications)

    /** Nonempty exactly for [Status.INCOMPLETE_INDEX]. */
    val failures: List<ProjectPreparationFailure> = immutableList(failures)

    init {
        require(this.failures.isEmpty() != (status == Status.INCOMPLETE_INDEX)) {
            "Failures are present exactly for INCOMPLETE_INDEX"
        }
        require(this.candidates.all {
            it.role == ProjectDeclarationRole.SPECIFICATION || it.role == ProjectDeclarationRole.STANDALONE
        }) { "Call candidates are specification or standalone occurrences" }
        require(this.packageSpecifications.all {
            it.kind == ProjectDeclarationKind.PACKAGE && it.role == ProjectDeclarationRole.SPECIFICATION
        }) { "Package evidence holds package specifications only" }
        require(status != Status.NOT_PREPARED ||
            (this.candidates.isEmpty() && this.uncertainties.isEmpty() && this.packageSpecifications.isEmpty())) {
            "NOT_PREPARED results are empty"
        }
        require(status != Status.SYNONYM_TARGET_NOT_IN_PROJECT || (this.synonyms.isNotEmpty() && this.candidates.isEmpty())) {
            "SYNONYM_TARGET_NOT_IN_PROJECT names its synonyms and has no candidate"
        }
        require(synonymTarget == null || this.synonyms.isNotEmpty()) { "A synonym target comes with its synonyms" }
    }

    internal companion object {
        /**
         * Projects the authoritative internal result; nothing is re-evaluated here. [failures] is the snapshot's
         * existing projection of the same preparation failures the finder reports.
         */
        @JvmSynthetic
        fun of(
            result: InternalCallCandidates,
            target: ProjectCallTarget,
            scope: ProjectSnapshotScope,
            failures: List<ProjectPreparationFailure>,
            reading: SynonymReading? = null
        ): ProjectCallCandidates {
            val status = when (result.status) {
                InternalCallCandidates.Status.FOUND -> Status.FOUND
                InternalCallCandidates.Status.NOT_FOUND_IN_PROJECT -> Status.NOT_FOUND_IN_PROJECT
                InternalCallCandidates.Status.UNKNOWN_PACKAGE -> Status.UNKNOWN_PACKAGE
                InternalCallCandidates.Status.AMBIGUOUS_TARGET -> Status.AMBIGUOUS_TARGET
                InternalCallCandidates.Status.PARTIAL_DECLARATIONS -> Status.PARTIAL_DECLARATIONS
                InternalCallCandidates.Status.INCOMPLETE_INDEX -> Status.INCOMPLETE_INDEX
                InternalCallCandidates.Status.NOT_PREPARED -> Status.NOT_PREPARED
            }
            val outside = reading != null && (status == Status.NOT_FOUND_IN_PROJECT || status == Status.UNKNOWN_PACKAGE)
            return ProjectCallCandidates(
                if (outside) Status.SYNONYM_TARGET_NOT_IN_PROJECT else status,
                target,
                ProjectSnapshotDeclarations.views(scope, result.candidates).map { it as ProjectSubprogramView },
                result.uncertainties.map {
                    when (it) {
                        InternalCallUncertainty.NAMESPACE_COMPETITION -> ProjectCallUncertainty.NAMESPACE_COMPETITION
                        InternalCallUncertainty.DUPLICATE_SPECIFICATION -> ProjectCallUncertainty.DUPLICATE_SPECIFICATION
                        InternalCallUncertainty.PARTIAL_DECLARATIONS -> ProjectCallUncertainty.PARTIAL_DECLARATIONS
                    }
                },
                if (outside) emptyList() else ProjectSnapshotDeclarations.views(scope, result.packageSpecifications),
                failures,
                reading?.synonyms.orEmpty(),
                reading?.target
            )
        }

        /** A target whose synonyms end outside the project before any lookup: at a database link or a cycle. */
        @JvmSynthetic
        fun outsideProject(target: ProjectCallTarget, reading: SynonymReading) = ProjectCallCandidates(
            Status.SYNONYM_TARGET_NOT_IN_PROJECT, target, emptyList(), emptyList(), emptyList(), emptyList(), reading.synonyms, reading.target
        )
    }
}
