//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalContext
import com.felipebz.zpa.api.project.ProjectSnapshot

/**
 * Project state owned by an analysis run, plus the external metadata context supplied for it (none by default).
 * Both are immutable and shared by every file analysis of the run.
 */
@OptIn(ZpaExperimentalApi::class)
class ProjectAnalysisContext private constructor(
    val state: State,
    val externalContext: ProjectExternalContext = ProjectExternalContext.none()
) {
    /**
     * The same prepared project with [context] as its external metadata context; the prepared index is shared, not
     * prepared again.
     */
    fun withExternalContext(context: ProjectExternalContext): ProjectAnalysisContext = ProjectAnalysisContext(state, context)

    /**
     * A new immutable snapshot backed by this context's prepared index: nothing is read or prepared again. For use
     * by the code owning the analysis run (for example to report on project declarations outside checks); checks use
     * `ProjectAnalysis.snapshot`. Each call issues a snapshot with its own declaration-ID scope.
     */
    fun createSnapshot(): ProjectSnapshot = ProjectSnapshot.create(this)

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
