package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;

/** Preparation lifecycle state, not a claim of complete PL/SQL coverage or validity. */
@ZpaExperimentalApi
public enum ProjectPreparationState {
    /** No preparation has occurred; reserved for capabilities supplied by internal lifecycles. */
    NOT_PREPARED,
    /** Read/extraction completed without failures but retained no supported declarations. */
    PREPARED_EMPTY,
    /** Read/extraction completed without failures and retained supported declarations. */
    PREPARED_WITH_DECLARATIONS,
    /** At least one source failed; takes precedence over empty/nonempty declaration counts. */
    PREPARED_WITH_FAILURES
}
