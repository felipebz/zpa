package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.Objects;

/**
 * Transient preparation input, not a durable project fact. It may retain source text or
 * a reader and its captures; the returned snapshot retains none of them. File identities
 * are opaque strings, validated as nonblank and unique by preparation before any read.
 */
@ZpaExperimentalApi
public final class ProjectSourceInput {
    private final String fileId;
    private final ProjectSourceReader reader;

    private ProjectSourceInput(String fileId, ProjectSourceReader reader) {
        this.fileId = Objects.requireNonNull(fileId, "fileId");
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    /** Creates an input from caller-supplied, already decoded text. */
    public static ProjectSourceInput ofText(String fileId, String contents) {
        Objects.requireNonNull(contents, "contents");
        return new ProjectSourceInput(fileId, () -> contents);
    }

    /** Creates an input whose reader runs during each preparation, without memoization. */
    public static ProjectSourceInput ofReader(String fileId, ProjectSourceReader reader) {
        return new ProjectSourceInput(fileId, reader);
    }

    /** Returns the opaque caller-supplied identity, not a schema or filesystem location. */
    public String getFileId() {
        return fileId;
    }

    String read() throws Exception {
        return reader.read();
    }
}
