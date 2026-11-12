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
    completeSchemas: Collection<String>,
    /** Timeout applied to every dictionary statement of the acquisition. */
    val queryTimeout: Duration
) {
    /** Schemas to enumerate completely, in request order. Runtime-unmodifiable. */
    val schemas: Set<String> = Collections.unmodifiableSet(LinkedHashSet(schemas))

    /** Relations to acquire individually, in request order. Runtime-unmodifiable. */
    val relations: Set<OracleRelationName> = Collections.unmodifiableSet(LinkedHashSet(relations))

    /**
     * Requested schemas the caller asserts the session sees completely ([Builder.completeSchema]), in request order;
     * each is one of [schemas]. Runtime-unmodifiable.
     */
    val completeSchemas: Set<String> = Collections.unmodifiableSet(LinkedHashSet(completeSchemas))

    override fun toString(): String =
        "OracleMetadataRequest(schemas=$schemas, relations=$relations, completeSchemas=$completeSchemas, queryTimeout=$queryTimeout)"

    /** Mutable, single-thread builder. */
    class Builder internal constructor() {
        private val schemas = LinkedHashSet<String>()
        private val relations = LinkedHashSet<OracleRelationName>()
        private val completeSchemas = LinkedHashSet<String>()
        private var queryTimeout: Duration = DEFAULT_QUERY_TIMEOUT

        /** Enumerates [dictionarySchema] completely. @throws IllegalArgumentException for an invalid name */
        fun schema(dictionarySchema: String): Builder = apply { schemas += OracleRelationName.dictionaryName(dictionarySchema, "schema") }

        /** Acquires one relation. @throws IllegalArgumentException for an invalid name */
        fun relation(dictionarySchema: String, dictionaryName: String): Builder = apply {
            relations += OracleRelationName.of(dictionarySchema, dictionaryName)
        }

        /**
         * Asserts that the acquiring session sees every relation of [dictionarySchema], which must also be requested
         * with [schema]: the acquisition then declares it complete, as it does for the session user's own schema. The
         * assertion is the caller's alone: zpa does not check the session's privileges (`ALL_*` views show only what
         * the session may access). A wrong assertion turns a relation the session cannot see into
         * `RELATION_NOT_FOUND` instead of an unavailable lookup.
         *
         * @throws IllegalArgumentException for an invalid name
         */
        fun completeSchema(dictionarySchema: String): Builder = apply {
            completeSchemas += OracleRelationName.dictionaryName(dictionarySchema, "schema")
        }

        /** Per-statement timeout, at least one second (default 60 seconds). @throws IllegalArgumentException otherwise */
        fun queryTimeout(timeout: Duration): Builder = apply {
            require(timeout.seconds >= 1 && timeout.seconds <= Int.MAX_VALUE) { "The query timeout must be between 1 second and ${Int.MAX_VALUE} seconds: $timeout" }
            queryTimeout = timeout
        }

        /**
         * @throws IllegalStateException if no schema or relation was requested, or if a schema asserted complete was
         * not requested with [schema]
         */
        fun build(): OracleMetadataRequest {
            check(schemas.isNotEmpty() || relations.isNotEmpty()) { "At least one schema or relation must be requested" }
            val unrequested = completeSchemas - schemas
            check(unrequested.isEmpty()) { "A schema asserted complete must also be requested with schema(): $unrequested" }
            return OracleMetadataRequest(schemas, relations, completeSchemas, queryTimeout)
        }
    }

    companion object {
        private val DEFAULT_QUERY_TIMEOUT: Duration = Duration.ofSeconds(60)

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
