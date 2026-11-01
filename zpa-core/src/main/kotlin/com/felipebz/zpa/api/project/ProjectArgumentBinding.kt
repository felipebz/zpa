//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * One actual bound to one formal. [argument] is the request's own object and [parameter] is the candidate
 * declaration's own [ProjectSubprogramView.parameters] element.
 */
@ZpaExperimentalApi
class ProjectArgumentBinding private constructor(
    /** One-based, written order. */
    val argumentOrdinal: Int,
    val argument: ProjectCallArgument,
    /** The formal: name, one-based ordinal, mode, type reference, NOCOPY and default presence. */
    val parameter: ProjectParameterView
) {
    override fun toString(): String = "#$argumentOrdinal $argument -> ${parameter.name}#${parameter.ordinal}"

    internal companion object {
        @JvmSynthetic
        fun create(argumentOrdinal: Int, argument: ProjectCallArgument, parameter: ProjectParameterView) =
            ProjectArgumentBinding(argumentOrdinal, argument, parameter)
    }
}
