package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import com.felipebz.zpa.project.ProjectIndexPreparationResult;
import java.util.Collections;
import java.util.List;

/**
 * Immutable metadata facade retaining the existing authoritative preparation result/index.
 * Safe to retain after preparation: no input, reader, full source text, extractor, parser,
 * AST or file-local semantic state is reachable through this snapshot. Public lists are
 * runtime-unmodifiable, including from Java, and metadata reads never reacquire sources.
 */
@ZpaExperimentalApi
public final class ProjectSnapshot {
    private final ProjectIndexPreparationResult preparationResult;
    private final ProjectPreparationState preparationState;
    private final List<ProjectPreparationFailure> failures;
    private final List<String> fileIds;

    // Both metadata lists are privately owned by the preparation adapter; wrap without copying.
    ProjectSnapshot(ProjectIndexPreparationResult preparationResult,
                    ProjectPreparationState preparationState,
                    List<ProjectPreparationFailure> failures,
                    List<String> fileIds) {
        this.preparationResult = preparationResult;
        this.preparationState = preparationState;
        this.failures = Collections.unmodifiableList(failures);
        this.fileIds = Collections.unmodifiableList(fileIds);
    }

    public ProjectPreparationState getPreparationState() {
        return preparationState;
    }

    public int getAttemptedFileCount() {
        return preparationResult.getAttemptedFileCount();
    }

    /** Sources whose read/extraction completed, not a syntactic or coverage guarantee. */
    public int getSuccessfulFileCount() {
        return preparationResult.getSuccessfulFileCount();
    }

    /** Runtime-unmodifiable failure summaries in deterministic file-identity order. */
    public List<ProjectPreparationFailure> getFailures() {
        return failures;
    }

    /**
     * All attempted source identities, including failed sources, in existing index order.
     * IDs are opaque; ordering is deterministic presentation order, not semantic ranking.
     */
    public List<String> getFileIds() {
        return fileIds;
    }
}
