package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource

/** Java API bridge; all source adapters and extraction state are invocation-local. */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectSnapshotPreparation {
    @JvmStatic
    fun prepare(sources: Collection<ProjectSourceInput>, concurrent: Boolean): ProjectSnapshot {
        // Stabilize caller membership before constructing validated core source adapters.
        val inputs = sources.toList()
        val preparedSources = inputs.map { input ->
            ProjectSource(FileId(input.fileId)) { input.read() }
        }
        val result = ProjectIndexPreparation().prepare(preparedSources, concurrent)
        val state = ProjectAnalysisContext.prepared(result).state.kind
        val preparationState = when (state) {
            ProjectAnalysisContext.State.Kind.NOT_PREPARED -> ProjectPreparationState.NOT_PREPARED
            ProjectAnalysisContext.State.Kind.PREPARED_EMPTY -> ProjectPreparationState.PREPARED_EMPTY
            ProjectAnalysisContext.State.Kind.PREPARED_WITH_DECLARATIONS ->
                ProjectPreparationState.PREPARED_WITH_DECLARATIONS
            ProjectAnalysisContext.State.Kind.PREPARED_WITH_FAILURES ->
                ProjectPreparationState.PREPARED_WITH_FAILURES
        }
        return ProjectSnapshot(
            result,
            preparationState,
            result.failures.map { ProjectPreparationFailure(it.fileId.value, it.exceptionType) },
            result.index.fileIds.map { it.value }
        )
    }
}
