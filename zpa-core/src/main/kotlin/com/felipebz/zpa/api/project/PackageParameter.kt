//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** Immutable package parameter metadata exposed to project-aware custom rules. */
@ZpaExperimentalApi
class PackageParameter private constructor(
    /** One-based position of the parameter. */
    val ordinal: Int,
    val isNocopy: Boolean,
    /** Whether this declaration includes a parameter default expression. */
    val isDefaultPresent: Boolean
) {
    internal companion object {
        @JvmSynthetic
        fun of(ordinal: Int, nocopy: Boolean, defaultPresent: Boolean): PackageParameter {
            require(ordinal > 0) { "Package parameter ordinals are one-based" }
            return PackageParameter(ordinal, nocopy, defaultPresent)
        }
    }
}
