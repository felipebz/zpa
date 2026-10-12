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
import java.util.*
import com.felipebz.zpa.project.ProjectCallArgument as InternalCallArgument

/**
 * One actual parameter as written: positional, or named with a label. No expression, token or source text is held.
 * Labels match formals by [ProjectIdentifier] equality (`a`, `A` and `"A"` are one label; `"a"` is another).
 */
@ZpaExperimentalApi
class ProjectCallArgument private constructor(private val labelIdentifier: ProjectIdentifier?) {
    @get:JvmSynthetic
    internal val internal: InternalCallArgument =
        if (labelIdentifier == null) InternalCallArgument.positional() else InternalCallArgument.named(labelIdentifier.identifier)

    val isNamed: Boolean get() = labelIdentifier != null

    /** Present only for a named argument. */
    val label: Optional<ProjectIdentifier> get() = Optional.ofNullable(labelIdentifier)

    override fun toString(): String = labelIdentifier?.let { "$it => ?" } ?: "?"

    companion object {
        @JvmStatic
        fun positional(): ProjectCallArgument = ProjectCallArgument(null)

        @JvmStatic
        fun named(label: ProjectIdentifier): ProjectCallArgument = ProjectCallArgument(label)
    }
}
