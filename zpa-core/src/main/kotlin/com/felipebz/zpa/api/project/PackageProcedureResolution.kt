//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/** Result of resolving a package procedure for a zero-argument invocation. */
@ZpaExperimentalApi
class PackageProcedureResolution private constructor(val status: Status, procedure: PackageSubprogram?) {
    enum class Status {
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

    val procedure: Optional<PackageSubprogram> = Optional.ofNullable(procedure)

    internal companion object {
        @JvmSynthetic
        fun resolved(procedure: PackageSubprogram) = PackageProcedureResolution(Status.RESOLVED, procedure)

        @JvmSynthetic
        fun of(status: Status): PackageProcedureResolution {
            require(status != Status.RESOLVED) { "Resolved results require a procedure" }
            return PackageProcedureResolution(status, null)
        }
    }
}
