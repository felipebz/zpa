package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.Collection;

/** Source-independent entry point to the existing Project System preparation pipeline. */
@ZpaExperimentalApi
public final class ProjectSnapshots {
    private ProjectSnapshots() {
    }

    /** Prepares synchronously with concurrent source reads/extraction enabled. */
    public static ProjectSnapshot prepare(Collection<ProjectSourceInput> sources) {
        return prepare(sources, true);
    }

    /**
     * Stabilizes collection membership, then synchronously reads/extracts all sources.
     * Do not mutate the collection while its initial membership copy is being taken.
     * With concurrency disabled, reader invocations do not overlap; otherwise readers may
     * run on worker threads. Queries never reread sources; another prepare call reads again.
     *
     * <p>Nonblank, unique file IDs are validated before reads. Reader/extractor Exceptions
     * produce per-file summaries and empty facts for those files; Errors continue to escape.
     * Unsupported source categories are not failures merely because no facts are extracted.
     * The snapshot retains the authoritative index, not inputs, readers or full source text.
     *
     * @throws IllegalArgumentException if file identities are blank or duplicated
     */
    public static ProjectSnapshot prepare(Collection<ProjectSourceInput> sources, boolean concurrent) {
        return ProjectSnapshotPreparation.prepare(sources, concurrent);
    }
}
