//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/** Result of correlating a package-body implementation with its package specification. */
@ZpaExperimentalApi
class PackageSpecificationResolution private constructor(
    val status: Status,
    body: PackageSubprogram?,
    specification: PackageSubprogram?
) {
    /** The outcome of the package specification query. */
    enum class Status {
        RESOLVED,
        NOT_FOUND,
        AMBIGUOUS,
        INCOMPLETE,
        NOT_PREPARED,
        NOT_APPLICABLE
    }

    val body: Optional<PackageSubprogram> = Optional.ofNullable(body)

    val specification: Optional<PackageSubprogram> = Optional.ofNullable(specification)

    internal companion object {
        @JvmSynthetic
        fun resolved(body: PackageSubprogram, specification: PackageSubprogram) =
            PackageSpecificationResolution(Status.RESOLVED, body, specification)

        @JvmSynthetic
        fun notFound() = PackageSpecificationResolution(Status.NOT_FOUND, null, null)

        @JvmSynthetic
        fun ambiguous() = PackageSpecificationResolution(Status.AMBIGUOUS, null, null)

        @JvmSynthetic
        fun incomplete() = PackageSpecificationResolution(Status.INCOMPLETE, null, null)

        @JvmSynthetic
        fun notPrepared() = PackageSpecificationResolution(Status.NOT_PREPARED, null, null)

        @JvmSynthetic
        fun notApplicable() = PackageSpecificationResolution(Status.NOT_APPLICABLE, null, null)
    }
}
