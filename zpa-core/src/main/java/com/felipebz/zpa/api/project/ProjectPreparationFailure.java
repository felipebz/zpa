package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;

/** Durable failure summary: no exception, message, cause, stack trace or parser state. */
@ZpaExperimentalApi
public final class ProjectPreparationFailure {
    private final String fileId;
    private final String exceptionType;

    ProjectPreparationFailure(String fileId, String exceptionType) {
        this.fileId = fileId;
        this.exceptionType = exceptionType;
    }

    public String getFileId() {
        return fileId;
    }

    /** Fully qualified class name of the caught Exception, not its root cause. */
    public String getExceptionType() {
        return exceptionType;
    }
}
