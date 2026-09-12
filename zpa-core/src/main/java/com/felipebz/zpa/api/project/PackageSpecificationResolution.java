package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.Optional;

/** Result of correlating a package-body implementation with its package specification. */
@ZpaExperimentalApi
public final class PackageSpecificationResolution {

    private final Status status;
    private final PackageSubprogram body;
    private final PackageSubprogram specification;

    PackageSpecificationResolution(Status status, PackageSubprogram body, PackageSubprogram specification) {
        this.status = status;
        this.body = body;
        this.specification = specification;
    }

    /** The outcome of the package specification query. */
    public enum Status {
        RESOLVED,
        NOT_FOUND,
        AMBIGUOUS,
        INCOMPLETE,
        NOT_PREPARED,
        NOT_APPLICABLE
    }

    public Status getStatus() {
        return status;
    }

    public Optional<PackageSubprogram> getBody() {
        return Optional.ofNullable(body);
    }

    public Optional<PackageSubprogram> getSpecification() {
        return Optional.ofNullable(specification);
    }

    static PackageSpecificationResolution resolved(PackageSubprogram body, PackageSubprogram specification) {
        return new PackageSpecificationResolution(Status.RESOLVED, body, specification);
    }

    static PackageSpecificationResolution notFound() {
        return new PackageSpecificationResolution(Status.NOT_FOUND, null, null);
    }

    static PackageSpecificationResolution ambiguous() {
        return new PackageSpecificationResolution(Status.AMBIGUOUS, null, null);
    }

    static PackageSpecificationResolution incomplete() {
        return new PackageSpecificationResolution(Status.INCOMPLETE, null, null);
    }

    static PackageSpecificationResolution notPrepared() {
        return new PackageSpecificationResolution(Status.NOT_PREPARED, null, null);
    }

    static PackageSpecificationResolution notApplicable() {
        return new PackageSpecificationResolution(Status.NOT_APPLICABLE, null, null);
    }
}
