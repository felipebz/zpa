//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
                    .findSpecificationOccurrences(body)
                    .map { it.declaration as PackageSubprogramDeclaration }

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

/**
 * Canonical specification occurrences matching [body], in index order. The resolver and the
 * public same-snapshot correlation share this single selection.
 */
internal fun ProjectSymbolIndex.findSpecificationOccurrences(body: PackageSubprogramDeclaration): List<IndexedDeclaration> {
    // A body pairs with the specification of its package in the same schema (its written owner, else its file's).
    val schema = unitSchema(body.owner, body.fileId)
    val candidates = if (schema == null) findSubprogramOccurrences(body.owner, body.name)
    else findInSchema(schema, QualifiedName(listOf(body.owner.last, body.name)))
    return candidates.filter { (it.declaration as? PackageSubprogramDeclaration)?.matchesSpecification(body) == true }
}

/**
 * Whether this specification heading matches [body]'s header. The package names are not compared: the pairing of the
 * packages, by schema and name without owner, is the lookup's ([findSpecificationOccurrences]), and a written owner
 * on one side only is the same package.
 */
private fun PackageSubprogramDeclaration.matchesSpecification(body: PackageSubprogramDeclaration): Boolean {
    if (role != DeclarationRole.SPECIFICATION || owner.last != body.owner.last) return false
    val spec = headerIdentity()
    val other = body.headerIdentity()
    return spec.overload.kind == other.overload.kind && spec.overload.name.last == other.overload.name.last &&
        spec.overload.parameters == other.overload.parameters && spec.parameters == other.parameters &&
        spec.returnTypeKey == other.returnTypeKey
}

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
