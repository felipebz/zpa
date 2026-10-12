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
 * Result of an exact external lookup by asserted identity. Absence is claimed only under declared coverage:
 * [Status.RELATION_NOT_FOUND] needs complete schema coverage and [Status.COLUMN_NOT_FOUND] complete column coverage
 * (and a name outside the system-generated `SYS_` prefix); otherwise the result is [Status.NOT_COVERED]. No alias,
 * synonym, search path or lexical scope is interpreted.
 */
@ZpaExperimentalApi
class ProjectExternalLookup private constructor(
    val status: Status,
    val schema: ProjectIdentifier,
    val relationName: ProjectIdentifier,
    columnName: ProjectIdentifier?,
    relation: ProjectExternalRelation?,
    column: ProjectExternalColumn?,
    origin: String?
) {
    enum class Status { FOUND, RELATION_NOT_FOUND, COLUMN_NOT_FOUND, NOT_COVERED, UNAVAILABLE }

    /** Present for column lookups. */
    val columnName: Optional<ProjectIdentifier> = Optional.ofNullable(columnName)

    /** The known relation: present for FOUND, COLUMN_NOT_FOUND, and NOT_COVERED when the relation is supplied. */
    val relation: Optional<ProjectExternalRelation> = Optional.ofNullable(relation)

    /** Present only for a FOUND column lookup. */
    val column: Optional<ProjectExternalColumn> = Optional.ofNullable(column)

    /** The metadata snapshot's origin label; empty only for UNAVAILABLE. */
    val origin: Optional<String> = Optional.ofNullable(origin)

    init {
        when (status) {
            Status.FOUND -> require(relation != null && (columnName == null) == (column == null)) { "Invalid FOUND lookup" }
            Status.COLUMN_NOT_FOUND -> require(relation != null && columnName != null && column == null) { "Invalid COLUMN_NOT_FOUND lookup" }
            Status.RELATION_NOT_FOUND -> require(relation == null && column == null) { "Invalid RELATION_NOT_FOUND lookup" }
            Status.NOT_COVERED -> require(column == null && (relation == null || columnName != null)) { "Invalid NOT_COVERED lookup" }
            Status.UNAVAILABLE -> require(relation == null && column == null && origin == null) { "Invalid UNAVAILABLE lookup" }
        }
        require((status == Status.UNAVAILABLE) == (origin == null)) { "Origin is absent exactly for UNAVAILABLE" }
    }

    override fun toString(): String = "$status $schema.$relationName${columnName.map { ".$it" }.orElse("")}"

    internal companion object {
        @JvmSynthetic
        fun of(
            status: Status,
            schema: ProjectIdentifier,
            relationName: ProjectIdentifier,
            columnName: ProjectIdentifier?,
            relation: ProjectExternalRelation?,
            column: ProjectExternalColumn?,
            origin: String?
        ) = ProjectExternalLookup(status, schema, relationName, columnName, relation, column, origin)
    }
}
