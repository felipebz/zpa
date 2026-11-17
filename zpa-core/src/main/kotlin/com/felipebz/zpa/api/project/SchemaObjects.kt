//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * The schema objects and synonyms a name may be read through: those of the external [metadata], and the synonyms
 * written in the project's sources ([written], by owner and name). A source declaration comes before a supplied
 * object of the same schema and name, so a written synonym hides the object, relation or synonym the metadata has
 * under its owner and name.
 */
@OptIn(ZpaExperimentalApi::class)
internal class SchemaObjects(
    val metadata: ProjectExternalMetadata,
    private val written: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalSynonym>
) {
    val hasWrittenSynonyms: Boolean get() = written.isNotEmpty()

    val isPublicSynonymsComplete: Boolean get() = metadata.isPublicSynonymsComplete

    fun isSchemaComplete(schema: ProjectIdentifier): Boolean = metadata.isSchemaComplete(schema)

    fun findObject(schema: ProjectIdentifier, name: ProjectIdentifier): ProjectExternalObject? =
        written[schema to name]?.let { ProjectExternalObject.of(schema, name, ProjectExternalObject.Kind.SYNONYM) }
            ?: metadata.findObject(schema, name).orElse(null)

    fun findSynonym(owner: ProjectIdentifier, name: ProjectIdentifier): ProjectExternalSynonym? =
        written[owner to name] ?: metadata.findSynonym(owner, name).orElse(null)
}
