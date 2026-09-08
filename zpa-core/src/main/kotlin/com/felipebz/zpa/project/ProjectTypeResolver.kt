package com.felipebz.zpa.project

/**
 * Resolves only named references against the frozen project declaration index. This result
 * intentionally remains in the project model and is not adapted to PlSqlDatatype yet.
 */
class ProjectTypeResolver(
    private val projectAnalysisContext: ProjectAnalysisContext
) {
    fun resolve(reference: NamedTypeRef): ProjectTypeResolution = when (val state = projectAnalysisContext.state) {
        ProjectAnalysisContext.State.NotPrepared -> ProjectTypeResolution.NotPrepared(reference)
        is ProjectAnalysisContext.State.Prepared -> {
            val candidates = state.result.index.findTypes(
                reference.name.segments.dropLast(1).takeIf { it.isNotEmpty() }?.let(::QualifiedName),
                reference.name.last
            )
            if (state.result.failures.isNotEmpty()) {
                ProjectTypeResolution.IncompleteIndex(reference, candidates, state.result.failures)
            } else {
                when (candidates.size) {
                    0 -> ProjectTypeResolution.NotFoundInProject(reference)
                    1 -> ProjectTypeResolution.Resolved(reference, candidates.single())
                    else -> ProjectTypeResolution.Ambiguous(reference, candidates)
                }
            }
        }
    }
}

sealed interface ProjectTypeResolution {
    val reference: NamedTypeRef

    data class Resolved(
        override val reference: NamedTypeRef,
        val declaration: ProjectTypeDeclaration
    ) : ProjectTypeResolution

    class Ambiguous(
        override val reference: NamedTypeRef,
        candidates: Collection<ProjectTypeDeclaration>
    ) : ProjectTypeResolution {
        val candidates: List<ProjectTypeDeclaration> = immutableList(candidates)

        init {
            require(candidates.size > 1) { "Ambiguous resolution requires multiple candidates" }
        }

        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            reference == other.reference && candidates == other.candidates

        override fun hashCode(): Int = 31 * reference.hashCode() + candidates.hashCode()

        override fun toString(): String = "Ambiguous($reference, $candidates)"
    }

    data class NotFoundInProject(
        override val reference: NamedTypeRef
    ) : ProjectTypeResolution

    class IncompleteIndex(
        override val reference: NamedTypeRef,
        knownCandidates: Collection<ProjectTypeDeclaration>,
        failures: Collection<ProjectIndexPreparationFailure>
    ) : ProjectTypeResolution {
        val knownCandidates: List<ProjectTypeDeclaration> = immutableList(knownCandidates)
        val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)

        init {
            require(failures.isNotEmpty()) { "An incomplete result requires preparation failures" }
        }

        override fun equals(other: Any?): Boolean = other is IncompleteIndex &&
            reference == other.reference && knownCandidates == other.knownCandidates && failures == other.failures

        override fun hashCode(): Int = 31 * (31 * reference.hashCode() + knownCandidates.hashCode()) + failures.hashCode()

        override fun toString(): String = "IncompleteIndex($reference, $knownCandidates, $failures)"
    }

    data class NotPrepared(
        override val reference: NamedTypeRef
    ) : ProjectTypeResolution
}
