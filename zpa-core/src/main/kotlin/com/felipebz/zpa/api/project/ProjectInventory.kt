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
import java.util.Collections

/**
 * Inventory of known declarations, not a resolution. Declarations are never ranked or
 * deduplicated. [Status.INCOMPLETE_INDEX] means preparation had failures, so absence or
 * uniqueness is not conclusive; known declarations are still returned. An empty inventory is
 * not proof that source is invalid Oracle code. All collections are runtime-unmodifiable.
 */
@ZpaExperimentalApi
class ProjectInventory<T : ProjectDeclarationView> private constructor(
    val status: Status,
    declarations: List<T>,
    failures: List<ProjectPreparationFailure>
) {
    enum class Status {
        COMPLETE,
        INCOMPLETE_INDEX,
        NOT_PREPARED
    }

    val declarations: List<T>
    val failures: List<ProjectPreparationFailure>

    init {
        when (status) {
            Status.COMPLETE -> require(failures.isEmpty()) { "COMPLETE inventories carry no failures" }
            Status.INCOMPLETE_INDEX -> require(failures.isNotEmpty()) { "INCOMPLETE_INDEX requires failure evidence" }
            Status.NOT_PREPARED ->
                require(declarations.isEmpty() && failures.isEmpty()) { "NOT_PREPARED inventories are empty" }
        }
        this.declarations = Collections.unmodifiableList(ArrayList(declarations))
        this.failures = Collections.unmodifiableList(ArrayList(failures))
    }

    internal companion object {
        @JvmSynthetic
        fun <T : ProjectDeclarationView> of(
            declarations: List<T>,
            failures: List<ProjectPreparationFailure>
        ): ProjectInventory<T> = ProjectInventory(
            if (failures.isEmpty()) Status.COMPLETE else Status.INCOMPLETE_INDEX,
            declarations,
            failures
        )

        @JvmSynthetic
        fun <T : ProjectDeclarationView> notPrepared(): ProjectInventory<T> =
            ProjectInventory(Status.NOT_PREPARED, emptyList(), emptyList())
    }
}
