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
 * Detached external relation (table or view) identified by its exact dictionary schema and name. It is not a project
 * declaration: it has no file, source range or declaration ID.
 */
@ZpaExperimentalApi
class ProjectExternalRelation private constructor(
    val schema: ProjectIdentifier,
    val name: ProjectIdentifier,
    /**
     * Every user-generated column (visible and invisible, i.e. every `ALL_TAB_COLUMNS` row) is supplied. System-generated
     * hidden columns are outside this guarantee.
     */
    val isColumnsComplete: Boolean,
    columns: List<ProjectExternalColumn>,
    rowObjectType: ProjectExternalDatatype?
) {
    /** `[schema, name]`, each one segment. */
    val qualifiedName: ProjectQualifiedName = ProjectQualifiedName.ofIdentifiers(listOf(schema, name))

    /** Positioned columns in ascending `COLUMN_ID`, then unpositioned (invisible) columns in supplied order. Runtime-unmodifiable. */
    val columns: List<ProjectExternalColumn> = Collections.unmodifiableList(
        columns.filter { it.position.isPresent }.sortedBy { it.position.asInt } + columns.filter { !it.position.isPresent }
    )

    /** The `%ROWTYPE` fields: positioned columns only, in ascending `COLUMN_ID`. Runtime-unmodifiable. */
    val rowFields: List<ProjectExternalColumn> = Collections.unmodifiableList(this.columns.filter { it.position.isPresent })

    /**
     * Present only for an object table, whose `%ROWTYPE` is an instance of this type rather than a record; [rowFields]
     * then lists only the supplied top-level attribute columns.
     */
    val rowObjectType: Optional<ProjectExternalDatatype> = Optional.ofNullable(rowObjectType)

    private val byName: Map<ProjectIdentifier, ProjectExternalColumn> = this.columns.associateBy { it.name }

    @JvmSynthetic
    internal fun column(name: ProjectIdentifier): ProjectExternalColumn? = byName[name]

    override fun equals(other: Any?): Boolean = other is ProjectExternalRelation && schema == other.schema &&
        name == other.name && isColumnsComplete == other.isColumnsComplete && columns == other.columns &&
        rowObjectType == other.rowObjectType

    override fun hashCode(): Int = Objects.hash(schema, name, isColumnsComplete, columns, rowObjectType)

    override fun toString(): String = "$qualifiedName (${columns.size} columns${if (isColumnsComplete) "" else ", partial"})"

    companion object {
        /**
         * @param dictionarySchema `OWNER` exactly as in the data dictionary
         * @param dictionaryName `TABLE_NAME` exactly as in the data dictionary
         * @param columnsComplete whether every user-generated column is supplied
         * @throws IllegalArgumentException for invalid names, duplicate column names or duplicate positions
         */
        @JvmStatic
        fun of(dictionarySchema: String, dictionaryName: String, columnsComplete: Boolean, columns: List<ProjectExternalColumn>): ProjectExternalRelation =
            create(dictionarySchema, dictionaryName, columnsComplete, columns, null)

        /** As [of], for an object table whose row is an instance of [rowType]. */
        @JvmStatic
        fun objectTable(
            dictionarySchema: String,
            dictionaryName: String,
            rowType: ProjectExternalDatatype,
            columnsComplete: Boolean,
            columns: List<ProjectExternalColumn>
        ): ProjectExternalRelation = create(dictionarySchema, dictionaryName, columnsComplete, columns, rowType)

        private fun create(
            schema: String,
            name: String,
            columnsComplete: Boolean,
            columns: List<ProjectExternalColumn>,
            rowType: ProjectExternalDatatype?
        ): ProjectExternalRelation {
            val copy = columns.toList()
            require(copy.none { false }) { "A column must not be null" }
            val duplicateName = copy.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }
            require(duplicateName == null) { "Duplicate column name: ${duplicateName!!.key}" }
            val duplicatePosition = copy.filter { it.position.isPresent }.groupBy { it.position.asInt }.entries.firstOrNull { it.value.size > 1 }
            require(duplicatePosition == null) { "Duplicate column position: ${duplicatePosition!!.key}" }
            return ProjectExternalRelation(dictionaryIdentifier(schema, "schema"), dictionaryIdentifier(name, "relation"), columnsComplete, copy, rowType)
        }
    }
}
