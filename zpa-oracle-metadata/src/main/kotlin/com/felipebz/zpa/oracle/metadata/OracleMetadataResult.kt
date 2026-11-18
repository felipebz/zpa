//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.squid.DatabaseVersion
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * A successful acquisition: detached, immutable metadata (holding no connection) plus its provenance. The session
 * values describe the acquiring JDBC session only; choosing the schema an analyzed unit compiles in (its owner for
 * stored code) is the caller's decision.
 */
@ZpaExperimentalApi
class OracleMetadataResult internal constructor(
    /** The metadata for [com.felipebz.zpa.api.project.ProjectSnapshot.describeExternalType] and `narrowCall`. */
    val metadata: ProjectExternalMetadata,
    /** `SESSION_USER` of the acquiring session. */
    val sessionUser: String,
    /** `CURRENT_SCHEMA` of the acquiring session. */
    val sessionCurrentSchema: String,
    /** `DB_UNIQUE_NAME`. */
    val database: String,
    /** `CON_NAME` (the PDB name, or `CDB$ROOT`); empty for databases before 12c, which have no containers. */
    val container: String,
    /** Local time at which the acquisition transaction started. */
    val acquiredAt: Instant,
    completeSchemas: Collection<String>,
    notVisible: Collection<OracleRelationName>,
    /** Relations added to [metadata]. */
    val relationCount: Int,
    /** Columns added to [metadata]. */
    val columnCount: Int,
    /** Distinct object names (`ALL_OBJECTS` rows of the requested schemas) added to [metadata]. */
    val objectCount: Int,
    /** Synonyms (of the requested schemas and public ones) added to [metadata]. */
    val synonymCount: Int,
    /** `DBMS_DB_VERSION.VERSION` and `RELEASE`, as the database's package declares them; null when they could not be read. */
    val databaseVersion: DatabaseVersion? = null,
    /** Why [databaseVersion] could not be read (the driver's message, not sanitized); null when it was read. */
    val databaseVersionFailure: String? = null,
    /** Object types added to [metadata]: those of the requested schemas, those their synonyms and the public synonyms reach, and their supertypes. */
    val objectTypeCount: Int = 0,
    /** Attributes the [objectTypeCount] types declare themselves. */
    val typeAttributeCount: Int = 0,
    /** Methods the [objectTypeCount] types declare themselves, each overload apart. */
    val typeMethodCount: Int = 0,
    /** How long reading the object types took, every query of them included. */
    val objectTypeReading: Duration = Duration.ZERO,
    /** Subprogram overloads added to [metadata], read with the same scope as the object types. */
    val subprogramCount: Int = 0,
    /** `ALL_ARGUMENTS` rows read for them: parameters and results. */
    val argumentRowCount: Int = 0,
    /** How long reading `ALL_ARGUMENTS` took, every query of it included. */
    val subprogramReading: Duration = Duration.ZERO
) {
    /** Requested schemas declared complete (so absent relations are reported as absent). Runtime-unmodifiable. */
    val completeSchemas: Set<String> = Collections.unmodifiableSet(LinkedHashSet(completeSchemas))

    /**
     * Explicitly requested relations the session could not see. Not visible is not absent: they are simply not part
     * of [metadata], whose lookups then report them as not covered. Runtime-unmodifiable.
     */
    val notVisible: Set<OracleRelationName> = Collections.unmodifiableSet(LinkedHashSet(notVisible))

    override fun toString(): String =
        "OracleMetadataResult($relationCount relations, $columnCount columns, $objectCount objects, $synonymCount synonyms, " +
            "$objectTypeCount object types ($typeAttributeCount attributes, $typeMethodCount methods, read in ${objectTypeReading.toMillis()} ms), " +
            "$subprogramCount subprograms ($argumentRowCount argument rows, read in ${subprogramReading.toMillis()} ms), " +
            "complete=$completeSchemas, publicSynonymsComplete=${metadata.isPublicSynonymsComplete}, notVisible=$notVisible, " +
            "${metadata.origin.orElse("")})"
}
