//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.squid.DatabaseVersion
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
    /** `DBMS_DB_VERSION.VERSION` and `RELEASE`, as the database's package declares them; null when they could not be read. */
    val databaseVersion: DatabaseVersion? = null,
    /** Why [databaseVersion] could not be read (the driver's message, not sanitized); null when it was read. */
    val databaseVersionFailure: String? = null
) {
    /** Requested schemas declared complete (so absent relations are reported as absent). Runtime-unmodifiable. */
    val completeSchemas: Set<String> = Collections.unmodifiableSet(LinkedHashSet(completeSchemas))

    /**
     * Explicitly requested relations the session could not see. Not visible is not absent: they are simply not part
     * of [metadata], whose lookups then report them as not covered. Runtime-unmodifiable.
     */
    val notVisible: Set<OracleRelationName> = Collections.unmodifiableSet(LinkedHashSet(notVisible))

    override fun toString(): String =
        "OracleMetadataResult($relationCount relations, $columnCount columns, complete=$completeSchemas, notVisible=$notVisible, ${metadata.origin.orElse("")})"
}
