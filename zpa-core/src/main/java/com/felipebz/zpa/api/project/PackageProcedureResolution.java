package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.Optional;

/** Result of resolving a package procedure for a zero-argument invocation. */
@ZpaExperimentalApi
public final class PackageProcedureResolution {

    private final Status status;
    private final PackageSubprogram procedure;

    private PackageProcedureResolution(Status status, PackageSubprogram procedure) {
        this.status = status;
        this.procedure = procedure;
    }

    public enum Status {
        RESOLVED,
        NOT_FOUND,
        NOT_CALLABLE,
        AMBIGUOUS,
        UNKNOWN_TARGET,
        AMBIGUOUS_TARGET,
        INCOMPLETE,
        NOT_PREPARED,
        NOT_APPLICABLE
    }

    public Status getStatus() {
        return status;
    }

    public Optional<PackageSubprogram> getProcedure() {
        return Optional.ofNullable(procedure);
    }

    static PackageProcedureResolution resolved(PackageSubprogram procedure) {
        return new PackageProcedureResolution(Status.RESOLVED, procedure);
    }

    static PackageProcedureResolution of(Status status) {
        if (status == Status.RESOLVED) {
            throw new IllegalArgumentException("Resolved results require a procedure");
        }
        return new PackageProcedureResolution(status, null);
    }
}
