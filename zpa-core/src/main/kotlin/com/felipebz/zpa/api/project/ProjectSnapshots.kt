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
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource

/** Source-independent entry point to the existing Project System preparation pipeline. */
@ZpaExperimentalApi
object ProjectSnapshots {
    /** Prepares synchronously with concurrent source reads/extraction enabled. */
    @JvmStatic
    fun prepare(sources: Collection<ProjectSourceInput>): ProjectSnapshot = prepare(sources, true)

    /**
     * Stabilizes collection membership, then synchronously reads/extracts all sources.
     * Do not mutate the collection while its initial membership copy is being taken.
     * With concurrency disabled, reader invocations do not overlap; otherwise readers may
     * run on worker threads. Queries never reread sources; another prepare call reads again.
     *
     * Nonblank, unique file IDs are validated before reads. Reader/extractor Exceptions
     * produce per-file summaries and empty facts for those files; Errors continue to escape.
     * Unsupported source categories are not failures merely because no facts are extracted.
     * The snapshot retains the authoritative index, not inputs, readers or full source text.
     *
     * @throws IllegalArgumentException if file identities are blank or duplicated
     */
    @JvmStatic
    fun prepare(sources: Collection<ProjectSourceInput>, concurrent: Boolean): ProjectSnapshot {
        // Stabilize caller membership before constructing validated core source adapters.
        val inputs = sources.toList()
        val preparedSources = inputs.map { input ->
            ProjectSource(FileId(input.fileId)) { input.read() }
        }
        val result = ProjectIndexPreparation().prepare(preparedSources, concurrent)
        val preparationState = when (ProjectAnalysisContext.prepared(result).state.kind) {
            ProjectAnalysisContext.State.Kind.NOT_PREPARED -> ProjectPreparationState.NOT_PREPARED
            ProjectAnalysisContext.State.Kind.PREPARED_EMPTY -> ProjectPreparationState.PREPARED_EMPTY
            ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS ->
                ProjectPreparationState.PREPARED_WITH_DECLARATIONS
            ProjectAnalysisContext.State.Kind.PREPARED_WITH_FAILURES ->
                ProjectPreparationState.PREPARED_WITH_FAILURES
        }
        return ProjectSnapshot.create(
            result,
            preparationState,
            result.failures.map { ProjectPreparationFailure.create(it.fileId.value, it.exceptionType) },
            result.index.fileIds.map { it.value }
        )
    }
}
