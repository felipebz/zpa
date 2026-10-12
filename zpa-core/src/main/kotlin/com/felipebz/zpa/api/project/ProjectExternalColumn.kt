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

/**
 * Detached column of an external relation. [position] is `COLUMN_ID`: empty for an invisible user column, which
 * exists (a `%TYPE` anchor may name it) but is not a `%ROWTYPE` field. No position is ever assigned by ZPA.
 */
@ZpaExperimentalApi
class ProjectExternalColumn private constructor(
    val name: ProjectIdentifier,
    position: Int?,
    datatype: ProjectExternalDatatype?
) {
    val position: OptionalInt = position?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    /** Empty when the provider did not supply the datatype; this is not an unsupported type. */
    val datatype: Optional<ProjectExternalDatatype> = Optional.ofNullable(datatype)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalColumn && name == other.name && position == other.position && datatype == other.datatype

    override fun hashCode(): Int = Objects.hash(name, position, datatype)

    override fun toString(): String = "${name.originalSpelling}#${if (position.isPresent) position.asInt else "-"} ${datatype.map { it.toString() }.orElse("?")}"

    companion object {
        /**
         * @param dictionaryName `COLUMN_NAME` exactly as in the data dictionary (case-sensitive, not quoted, never split)
         * @param position `COLUMN_ID` (at least 1), or null for an invisible column
         * @param datatype the datatype, or null when unknown
         * @throws IllegalArgumentException for an empty name, a name containing `"` or a position below 1
         */
        @JvmStatic
        fun of(dictionaryName: String, position: Int?, datatype: ProjectExternalDatatype?): ProjectExternalColumn {
            require(position == null || position >= 1) { "The column position must be at least 1: $position" }
            return ProjectExternalColumn(dictionaryIdentifier(dictionaryName, "column"), position, datatype)
        }
    }
}
