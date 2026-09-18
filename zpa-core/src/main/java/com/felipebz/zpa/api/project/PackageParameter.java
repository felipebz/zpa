package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;

/** Immutable package parameter metadata exposed to project-aware custom rules. */
@ZpaExperimentalApi
public final class PackageParameter {

    private final int ordinal;
    private final boolean nocopy;
    private final boolean defaultPresent;

    PackageParameter(int ordinal, boolean nocopy) {
        this(ordinal, nocopy, false);
    }

    PackageParameter(int ordinal, boolean nocopy, boolean defaultPresent) {
        if (ordinal <= 0) {
            throw new IllegalArgumentException("Package parameter ordinals are one-based");
        }
        this.ordinal = ordinal;
        this.nocopy = nocopy;
        this.defaultPresent = defaultPresent;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public boolean isNocopy() {
        return nocopy;
    }

    /** Whether this declaration includes a parameter default expression. */
    public boolean isDefaultPresent() {
        return defaultPresent;
    }
}
