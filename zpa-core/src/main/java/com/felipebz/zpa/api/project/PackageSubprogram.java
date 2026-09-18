package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable package subprogram metadata exposed to project-aware custom rules. */
@ZpaExperimentalApi
public final class PackageSubprogram {

    private final PackageSubprogramKind kind;
    private final List<PackageParameter> parameters;
    private final boolean deterministic;

    PackageSubprogram(PackageSubprogramKind kind, Iterable<PackageParameter> parameters) {
        this(kind, parameters, false);
    }

    PackageSubprogram(
        PackageSubprogramKind kind,
        Iterable<PackageParameter> parameters,
        boolean deterministic
    ) {
        this.kind = kind;
        this.deterministic = deterministic;
        List<PackageParameter> copy = new ArrayList<>();
        for (PackageParameter parameter : parameters) {
            copy.add(parameter);
        }
        this.parameters = Collections.unmodifiableList(copy);
    }

    public PackageSubprogramKind getKind() {
        return kind;
    }

    public List<PackageParameter> getParameters() {
        return parameters;
    }

    /** Whether this package function declaration is marked DETERMINISTIC. */
    public boolean isDeterministic() {
        return deterministic;
    }
}
