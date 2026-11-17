//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalLookup.Status

/**
 * The schema objects, synonyms and relations a name may be read through: those of the external [metadata], the
 * synonyms written in the project's sources ([written], by owner and name) and the tables created there ([tables]).
 * A source declaration comes before a supplied object of the same schema and name, so a written synonym hides the
 * object, relation or synonym the metadata has under its owner and name. A table is the exception: the metadata's
 * relation is what the database holds, after any change applied since the script that created it, so a source table
 * only fills a relation the metadata does not have.
 */
@OptIn(ZpaExperimentalApi::class)
internal class SchemaObjects(
    val metadata: ProjectExternalMetadata,
    private val written: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalSynonym>,
    private val tables: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation> = emptyMap()
) {
    val hasWrittenSynonyms: Boolean get() = written.isNotEmpty()

    /** The synonyms written in the project's sources, each once. */
    val writtenSynonyms: Collection<ProjectExternalSynonym> get() = written.values

    /** Whether some relation may be found: the metadata was supplied, or the sources create tables. */
    val available: Boolean get() = metadata.origin.isPresent || tables.isNotEmpty()

    /** The origin of a lookup: the metadata's, else the sources'. */
    val origin: String get() = metadata.origin.orElse(SOURCES)

    val isPublicSynonymsComplete: Boolean get() = metadata.isPublicSynonymsComplete

    fun isSchemaComplete(schema: ProjectIdentifier): Boolean = metadata.isSchemaComplete(schema)

    fun findObject(schema: ProjectIdentifier, name: ProjectIdentifier): ProjectExternalObject? =
        written[schema to name]?.let { ProjectExternalObject.of(schema, name, ProjectExternalObject.Kind.SYNONYM) }
            ?: metadata.findObject(schema, name).orElse(null)
            ?: tables[schema to name]?.let { ProjectExternalObject.of(schema, name, ProjectExternalObject.Kind.TABLE) }

    fun findSynonym(owner: ProjectIdentifier, name: ProjectIdentifier): ProjectExternalSynonym? =
        written[owner to name] ?: metadata.findSynonym(owner, name).orElse(null)

    fun findRelation(schema: ProjectIdentifier, name: ProjectIdentifier): ProjectExternalLookup = lookup(schema, name, null)

    fun findColumn(schema: ProjectIdentifier, name: ProjectIdentifier, column: ProjectIdentifier): ProjectExternalLookup = lookup(schema, name, column)

    private fun lookup(schema: ProjectIdentifier, name: ProjectIdentifier, column: ProjectIdentifier?): ProjectExternalLookup {
        val supplied = if (column == null) metadata.findRelation(schema, name) else metadata.findColumn(schema, name, column)
        if (supplied.relation.isPresent) return supplied
        val table = tables[schema to name] ?: return supplied.takeIf { metadata.origin.isPresent || tables.isEmpty() }
            ?: ProjectExternalLookup.of(Status.NOT_COVERED, schema, name, column, null, null, origin)
        if (column == null) return ProjectExternalLookup.of(Status.FOUND, schema, name, null, table, null, origin)
        val found = table.column(column)
        val status = when {
            found != null -> Status.FOUND
            // A column of no list is never proved absent; a system-generated one is not written in any.
            table.isColumnsComplete && !column.identifier.lookupName.startsWith("SYS_") -> Status.COLUMN_NOT_FOUND
            else -> Status.NOT_COVERED
        }
        return ProjectExternalLookup.of(status, schema, name, column, table, found, origin)
    }

    private companion object {
        const val SOURCES = "project sources"
    }
}
