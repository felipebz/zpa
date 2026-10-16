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
package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalMetadata
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
    val columnCount: Int
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
