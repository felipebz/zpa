//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
