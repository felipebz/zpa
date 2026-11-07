//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * External metadata supplied for an analysis run, with the schema in which unqualified units compile. Immutable and
 * safe to share between threads; it holds no connection. Pass [metadata] and [compilationSchema] to
 * [ProjectSnapshot.describeExternalType] and [ProjectSnapshot.narrowCall].
 */
@ZpaExperimentalApi
class ProjectExternalContext private constructor(
    /** The supplied metadata; [ProjectExternalMetadata.none] when no metadata was supplied. */
    val metadata: ProjectExternalMetadata,
    defaultCompilationSchema: ProjectIdentifier?,
    /** The schemas files compile in, by path; [ProjectSchemaPaths.none] when none were configured. */
    val schemaPaths: ProjectSchemaPaths = ProjectSchemaPaths.none()
) {
    /** The schema configured for units whose name carries no schema; empty when none was configured. */
    val defaultCompilationSchema: Optional<ProjectIdentifier> = Optional.ofNullable(defaultCompilationSchema)

    /**
     * The schema in which the unit named [unitName] compiles (a package name, or a standalone subprogram or type
     * name, as written in its CREATE statement): an explicit schema prefix (`hr.pkg`) always wins; an unqualified
     * name uses [defaultCompilationSchema]. Empty when neither establishes the schema, so callers keep the
     * uncertainty (for example a schema-context-missing description) instead of guessing.
     */
    fun compilationSchema(unitName: ProjectQualifiedName): Optional<ProjectIdentifier> = when (unitName.segments.size) {
        1 -> defaultCompilationSchema
        2 -> Optional.of(unitName.segments.first())
        else -> Optional.empty()
    }

    /**
     * The schema in which the unit named [unitName], written in the file [fileId], compiles: an explicit schema prefix,
     * else the schema of the first [schemaPaths] pattern matching the file, else [defaultCompilationSchema]. Empty when
     * none establishes it.
     */
    fun compilationSchema(unitName: ProjectQualifiedName, fileId: String): Optional<ProjectIdentifier> =
        if (unitName.segments.size == 1) fileSchema(fileId) else compilationSchema(unitName)

    /** The schema units of [fileId] written without one compile in: its path's schema, else the default; may be empty. */
    fun fileSchema(fileId: String): Optional<ProjectIdentifier> =
        schemaPaths.schemaOf(fileId).or { defaultCompilationSchema }

    override fun toString(): String =
        "ProjectExternalContext($metadata, default schema ${defaultCompilationSchema.map { it.toString() }.orElse("none")}" +
            (if (schemaPaths.entries.isEmpty()) ")" else ", schema paths $schemaPaths)")

    companion object {
        private val NONE = ProjectExternalContext(ProjectExternalMetadata.none(), null)

        /** No external metadata and no compilation schema. */
        @JvmStatic
        fun none(): ProjectExternalContext = NONE

        /** [metadata] with [defaultCompilationSchema] (null when unknown) for unqualified units. */
        @JvmStatic
        fun of(metadata: ProjectExternalMetadata, defaultCompilationSchema: ProjectIdentifier?): ProjectExternalContext =
            ProjectExternalContext(metadata, defaultCompilationSchema)

        /** As [of], with the schemas files compile in by path. */
        @JvmStatic
        fun of(metadata: ProjectExternalMetadata, defaultCompilationSchema: ProjectIdentifier?, schemaPaths: ProjectSchemaPaths): ProjectExternalContext =
            ProjectExternalContext(metadata, defaultCompilationSchema, schemaPaths)
    }
}
