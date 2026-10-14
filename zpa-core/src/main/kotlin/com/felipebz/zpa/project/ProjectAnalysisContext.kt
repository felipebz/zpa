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
