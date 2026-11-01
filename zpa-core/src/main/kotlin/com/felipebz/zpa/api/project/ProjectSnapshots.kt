//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
        return ProjectSnapshot.create(ProjectAnalysisContext.prepared(result))
    }
}
