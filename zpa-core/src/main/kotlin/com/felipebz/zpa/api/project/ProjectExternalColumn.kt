//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    datatype: ProjectExternalDatatype?,
    nullable: Boolean?
) {
    val position: OptionalInt = position?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    /** Empty when the provider did not supply the datatype; this is not an unsupported type. */
    val datatype: Optional<ProjectExternalDatatype> = Optional.ofNullable(datatype)

    /**
     * Whether a row may hold null in the column: false only when the provider guarantees that no row does. Empty when
     * the provider did not supply it.
     */
    val nullable: Optional<Boolean> = Optional.ofNullable(nullable)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalColumn && name == other.name && position == other.position && datatype == other.datatype &&
            nullable == other.nullable

    override fun hashCode(): Int = Objects.hash(name, position, datatype, nullable)

    override fun toString(): String =
        "${name.originalSpelling}#${if (position.isPresent) position.asInt else "-"} ${datatype.map { it.toString() }.orElse("?")}" +
            if (nullable.orElse(true)) "" else " not null"

    companion object {
        /**
         * @param dictionaryName `COLUMN_NAME` exactly as in the data dictionary (case-sensitive, not quoted, never split)
         * @param position `COLUMN_ID` (at least 1), or null for an invisible column
         * @param datatype the datatype, or null when unknown
         * @param nullable false when no row can hold null in the column, true when one may, null when unknown
         * @throws IllegalArgumentException for an empty name, a name containing `"` or a position below 1
         */
        @JvmStatic
        @JvmOverloads
        fun of(dictionaryName: String, position: Int?, datatype: ProjectExternalDatatype?, nullable: Boolean? = null): ProjectExternalColumn {
            require(position == null || position >= 1) { "The column position must be at least 1: $position" }
            return ProjectExternalColumn(dictionaryIdentifier(dictionaryName, "column"), position, datatype, nullable)
        }

        @JvmSynthetic
        internal fun fromSource(name: ProjectIdentifier, position: Int?, datatype: ProjectExternalDatatype?, nullable: Boolean?) =
            ProjectExternalColumn(name, position, datatype, nullable)
    }
}
