//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
