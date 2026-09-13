package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;
import java.util.Objects;
import java.util.Optional;

/** A package-procedure target used by the generic project-analysis resolver. */
@ZpaExperimentalApi
public final class PackageProcedureReference {

    private final String owner;
    private final String packageName;
    private final String procedureName;

    private PackageProcedureReference(String owner, String packageName, String procedureName) {
        this.owner = owner;
        this.packageName = packageName;
        this.procedureName = requireName(procedureName, "procedure name");
        if (owner != null) {
            requireName(owner, "owner name");
        }
        if (packageName != null) {
            requireName(packageName, "package name");
        }
    }

    /** References a procedure in the package containing the current analysis node. */
    public static PackageProcedureReference currentPackage(String procedureName) {
        return new PackageProcedureReference(null, null, procedureName);
    }

    /** References a procedure in a package owned by the current schema/owner. */
    public static PackageProcedureReference inPackage(String packageName, String procedureName) {
        return new PackageProcedureReference(null, packageName, procedureName);
    }

    /** References a procedure in an explicitly named owner and package. */
    public static PackageProcedureReference inOwnerPackage(
        String owner,
        String packageName,
        String procedureName
    ) {
        return new PackageProcedureReference(owner, packageName, procedureName);
    }

    public Optional<String> getOwner() {
        return Optional.ofNullable(owner);
    }

    /** Empty when this reference targets the package containing the analysis node. */
    public Optional<String> getPackageName() {
        return Optional.ofNullable(packageName);
    }

    public String getProcedureName() {
        return procedureName;
    }

    private static String requireName(String value, String description) {
        Objects.requireNonNull(value, description);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(description + " cannot be blank");
        }
        return value;
    }
}
