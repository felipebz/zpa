/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
