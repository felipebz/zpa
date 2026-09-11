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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.project

/**
 * Correlates one package-body implementation with specification facts in a prepared project.
 * This is deliberately a project-model query; it has no AST or semantic-run dependencies.
 */
internal class ProjectSubprogramSpecificationResolver(
    private val projectAnalysisContext: ProjectAnalysisContext
) {
    fun resolve(body: PackageSubprogramDeclaration): ProjectSubprogramSpecificationResolution {
        require(body.role == DeclarationRole.BODY) {
            "Specification correlation requires a BODY declaration"
        }

        return when (val state = projectAnalysisContext.state) {
            ProjectAnalysisContext.State.NotPrepared ->
                ProjectSubprogramSpecificationResolution.NotPrepared(body)

            is ProjectAnalysisContext.State.Prepared -> {
                val candidates = state.result.index
                    .findSubprograms(body.owner, body.name)
                    .filter { it.matchesSpecification(body) }

                if (state.result.failures.isNotEmpty()) {
                    ProjectSubprogramSpecificationResolution.Incomplete(
                        body,
                        candidates,
                        state.result.failures
                    )
                } else {
                    when (candidates.size) {
                        0 -> ProjectSubprogramSpecificationResolution.NotFound(body)
                        1 -> ProjectSubprogramSpecificationResolution.Resolved(body, candidates.single())
                        else -> ProjectSubprogramSpecificationResolution.Ambiguous(body, candidates)
                    }
                }
            }
        }
    }
}

private fun PackageSubprogramDeclaration.matchesSpecification(body: PackageSubprogramDeclaration): Boolean =
    role == DeclarationRole.SPECIFICATION && headerIdentity() == body.headerIdentity()

internal sealed interface ProjectSubprogramSpecificationResolution {
    val body: PackageSubprogramDeclaration

    data class Resolved(
        override val body: PackageSubprogramDeclaration,
        val specification: PackageSubprogramDeclaration
    ) : ProjectSubprogramSpecificationResolution {
        init {
            require(body.role == DeclarationRole.BODY)
            require(specification.role == DeclarationRole.SPECIFICATION)
            require(specification.matchesSpecification(body))
        }
    }

    data class NotFound(
        override val body: PackageSubprogramDeclaration
    ) : ProjectSubprogramSpecificationResolution {
        init {
            require(body.role == DeclarationRole.BODY)
        }
    }

    class Ambiguous(
        override val body: PackageSubprogramDeclaration,
        candidates: Collection<PackageSubprogramDeclaration>
    ) : ProjectSubprogramSpecificationResolution {
        val candidates: List<PackageSubprogramDeclaration> = immutableList(candidates)

        init {
            require(body.role == DeclarationRole.BODY)
            require(this.candidates.size > 1) { "Ambiguous resolution requires multiple candidates" }
            require(this.candidates.all { it.matchesSpecification(body) })
        }

        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            body == other.body && candidates == other.candidates

        override fun hashCode(): Int = 31 * body.hashCode() + candidates.hashCode()

        override fun toString(): String = "Ambiguous($body, $candidates)"
    }

    class Incomplete(
        override val body: PackageSubprogramDeclaration,
        knownCandidates: Collection<PackageSubprogramDeclaration>,
        failures: Collection<ProjectIndexPreparationFailure>
    ) : ProjectSubprogramSpecificationResolution {
        val knownCandidates: List<PackageSubprogramDeclaration> = immutableList(knownCandidates)
        val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)

        init {
            require(body.role == DeclarationRole.BODY)
            require(this.knownCandidates.all { it.matchesSpecification(body) })
            require(this.failures.isNotEmpty()) { "An incomplete result requires preparation failures" }
        }

        override fun equals(other: Any?): Boolean = other is Incomplete &&
            body == other.body && knownCandidates == other.knownCandidates && failures == other.failures

        override fun hashCode(): Int = 31 * (31 * body.hashCode() + knownCandidates.hashCode()) + failures.hashCode()

        override fun toString(): String = "Incomplete($body, $knownCandidates, $failures)"
    }

    data class NotPrepared(
        override val body: PackageSubprogramDeclaration
    ) : ProjectSubprogramSpecificationResolution {
        init {
            require(body.role == DeclarationRole.BODY)
        }
    }
}
