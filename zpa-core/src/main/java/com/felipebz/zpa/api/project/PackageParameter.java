package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;

/** Immutable package parameter metadata exposed to project-aware custom rules. */
@ZpaExperimentalApi
public final class PackageParameter {

    private final int ordinal;
    private final boolean nocopy;

    PackageParameter(int ordinal, boolean nocopy) {
        if (ordinal <= 0) {
            throw new IllegalArgumentException("Package parameter ordinals are one-based");
        }
        this.ordinal = ordinal;
        this.nocopy = nocopy;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public boolean isNocopy() {
        return nocopy;
    }
}
