//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList

/** Immutable package subprogram metadata exposed to project-aware custom rules. */
@ZpaExperimentalApi
class PackageSubprogram private constructor(
    val kind: PackageSubprogramKind,
    parameters: List<PackageParameter>,
    /** Whether this package function declaration is marked DETERMINISTIC. */
    val isDeterministic: Boolean
) {
    val parameters: List<PackageParameter> = immutableList(parameters)

    internal companion object {
        @JvmSynthetic
        fun of(kind: PackageSubprogramKind, parameters: List<PackageParameter>, deterministic: Boolean) =
            PackageSubprogram(kind, parameters, deterministic)
    }
}
