package com.felipebz.zpa.project

/** Project state owned by an analysis run. It is intentionally not consumed semantically yet. */
class ProjectAnalysisContext private constructor(
    val state: State
) {
    sealed interface State {
        val kind: Kind

        enum class Kind {
            NOT_PREPARED,
            PREPARED_EMPTY,
            PREPARED_WITH_DECLARATIONS,
            PREPARED_WITH_FAILURES
        }

        data object NotPrepared : State {
            override val kind = Kind.NOT_PREPARED
        }

        class Prepared(val result: ProjectIndexPreparationResult) : State {
            override val kind = when {
                result.failures.isNotEmpty() -> Kind.PREPARED_WITH_FAILURES
                result.index.declarations.isEmpty() -> Kind.PREPARED_EMPTY
                else -> Kind.PREPARED_WITH_DECLARATIONS
            }
        }
    }

    companion object {
        val NOT_PREPARED = ProjectAnalysisContext(State.NotPrepared)

        fun prepared(result: ProjectIndexPreparationResult): ProjectAnalysisContext =
            ProjectAnalysisContext(State.Prepared(result))
    }
}
