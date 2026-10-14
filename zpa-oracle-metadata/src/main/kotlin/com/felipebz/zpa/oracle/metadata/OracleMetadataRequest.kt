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
import java.time.Duration
import java.util.*

/**
 * What to acquire: whole schemas (every table, object table and view with its columns) and individually named
 * relations. Names are exact dictionary names. Immutable; build it with [builder].
 */
@ZpaExperimentalApi
class OracleMetadataRequest private constructor(
    schemas: Collection<String>,
    relations: Collection<OracleRelationName>,
    /** Timeout applied to every dictionary statement of the acquisition. */
    val queryTimeout: Duration
) {
    /** Schemas to enumerate completely, in request order. Runtime-unmodifiable. */
    val schemas: Set<String> = Collections.unmodifiableSet(LinkedHashSet(schemas))

    /** Relations to acquire individually, in request order. Runtime-unmodifiable. */
    val relations: Set<OracleRelationName> = Collections.unmodifiableSet(LinkedHashSet(relations))

    override fun toString(): String = "OracleMetadataRequest(schemas=$schemas, relations=$relations, queryTimeout=$queryTimeout)"

    /** Mutable, single-thread builder. */
    class Builder internal constructor() {
        private val schemas = LinkedHashSet<String>()
        private val relations = LinkedHashSet<OracleRelationName>()
        private var queryTimeout: Duration = DEFAULT_QUERY_TIMEOUT

        /** Enumerates [dictionarySchema] completely. @throws IllegalArgumentException for an invalid name */
        fun schema(dictionarySchema: String): Builder = apply { schemas += OracleRelationName.dictionaryName(dictionarySchema, "schema") }

        /** Acquires one relation. @throws IllegalArgumentException for an invalid name */
        fun relation(dictionarySchema: String, dictionaryName: String): Builder = apply {
            relations += OracleRelationName.of(dictionarySchema, dictionaryName)
        }

        /** Per-statement timeout, at least one second (default 60 seconds). @throws IllegalArgumentException otherwise */
        fun queryTimeout(timeout: Duration): Builder = apply {
            require(timeout.seconds >= 1 && timeout.seconds <= Int.MAX_VALUE) { "The query timeout must be between 1 second and ${Int.MAX_VALUE} seconds: $timeout" }
            queryTimeout = timeout
        }

        /** @throws IllegalStateException if no schema or relation was requested */
        fun build(): OracleMetadataRequest {
            check(schemas.isNotEmpty() || relations.isNotEmpty()) { "At least one schema or relation must be requested" }
            return OracleMetadataRequest(schemas, relations, queryTimeout)
        }
    }

    companion object {
        private val DEFAULT_QUERY_TIMEOUT: Duration = Duration.ofSeconds(60)

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
