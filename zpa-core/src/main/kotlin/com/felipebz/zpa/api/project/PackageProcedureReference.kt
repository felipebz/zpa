//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/** A package-procedure target used by the generic project-analysis resolver. */
@ZpaExperimentalApi
class PackageProcedureReference private constructor(owner: String?, packageName: String?, val procedureName: String) {
    init {
        requireName(procedureName, "procedure name")
        owner?.let { requireName(it, "owner name") }
        packageName?.let { requireName(it, "package name") }
    }

    val owner: Optional<String> = Optional.ofNullable(owner)

    /** Empty when this reference targets the package containing the analysis node. */
    val packageName: Optional<String> = Optional.ofNullable(packageName)

    companion object {
        /** References a procedure in the package containing the current analysis node. */
        @JvmStatic
        fun currentPackage(procedureName: String) = PackageProcedureReference(null, null, procedureName)

        /** References a procedure in a package owned by the current schema/owner. */
        @JvmStatic
        fun inPackage(packageName: String, procedureName: String) = PackageProcedureReference(null, packageName, procedureName)

        /** References a procedure in an explicitly named owner and package. */
        @JvmStatic
        fun inOwnerPackage(owner: String, packageName: String, procedureName: String) =
            PackageProcedureReference(owner, packageName, procedureName)

        private fun requireName(value: String, description: String) =
            require(value.isNotBlank()) { "$description cannot be blank" }
    }
}
