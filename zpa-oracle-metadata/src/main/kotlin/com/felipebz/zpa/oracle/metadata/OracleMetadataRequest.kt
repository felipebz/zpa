//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.time.Duration
import java.util.*

/**
 * What to acquire: whole schemas (every table, object table and view with its columns, every object name and every
 * synonym of the schema), individually named relations and public synonyms. Names are exact dictionary names.
 * Immutable; build it with [builder].
 */
@ZpaExperimentalApi
class OracleMetadataRequest private constructor(
    schemas: Collection<String>,
    relations: Collection<OracleRelationName>,
    completeSchemas: Collection<String>,
    /**
     * Whether public synonyms are read (`ALL_SYNONYMS` rows owned by `PUBLIC`): all of them when
     * [publicSynonymTargets] is empty, otherwise only those whose `TABLE_OWNER` is one of the targets. Every public
     * synonym is visible to every session whatever its privileges on the target (even when the target does not
     * exist), so an unrestricted answer is complete, and an empty one means the database has no public synonym. A
     * restricted answer is never complete: an empty one means no public synonym points into those schemas, and public
     * synonyms for other schemas may exist.
     */
    val publicSynonyms: Boolean,
    publicSynonymTargets: Collection<String>,
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

    /**
     * The `TABLE_OWNER`s public synonyms are restricted to ([Builder.publicSynonymsFor]), in request order; empty when
     * every public synonym is read or none is (see [publicSynonyms]). Runtime-unmodifiable.
     */
    val publicSynonymTargets: Set<String> = Collections.unmodifiableSet(LinkedHashSet(publicSynonymTargets))

    override fun toString(): String =
        "OracleMetadataRequest(schemas=$schemas, relations=$relations, completeSchemas=$completeSchemas, " +
            "publicSynonyms=$publicSynonyms, publicSynonymTargets=$publicSynonymTargets, queryTimeout=$queryTimeout)"

    /** Mutable, single-thread builder. */
    class Builder internal constructor() {
        private val schemas = LinkedHashSet<String>()
        private val relations = LinkedHashSet<OracleRelationName>()
        private val completeSchemas = LinkedHashSet<String>()
        private val publicSynonymTargets = LinkedHashSet<String>()
        private var allPublicSynonyms = false
        private var queryTimeout: Duration = DEFAULT_QUERY_TIMEOUT

        /**
         * Enumerates [dictionarySchema] completely: its relations with their columns (`ALL_ALL_TABLES`, `ALL_VIEWS`,
         * `ALL_TAB_COLS`), its object names (`ALL_OBJECTS`) and its synonyms (`ALL_SYNONYMS`). `PUBLIC` is refused:
         * request public synonyms with [publicSynonyms] or [publicSynonymsFor].
         *
         * @throws IllegalArgumentException for an invalid name or `PUBLIC`
         */
        fun schema(dictionarySchema: String): Builder = apply {
            val name = OracleRelationName.dictionaryName(dictionarySchema, "schema")
            require(name != PUBLIC) { "PUBLIC is not a schema; request public synonyms with publicSynonyms() or publicSynonymsFor()" }
            schemas += name
        }

        /** Acquires one relation. @throws IllegalArgumentException for an invalid name */
        fun relation(dictionarySchema: String, dictionaryName: String): Builder = apply {
            relations += OracleRelationName.of(dictionarySchema, dictionaryName)
        }

        /**
         * Asserts that the acquiring session sees every relation, object name and synonym of [dictionarySchema], which
         * must also be requested with [schema]: the acquisition then declares it complete, as it does for the session
         * user's own schema. The assertion is the caller's alone: zpa does not check the session's privileges (`ALL_*`
         * views show only what the session may access). A wrong assertion turns a relation the session cannot see into
         * `RELATION_NOT_FOUND` instead of an unavailable lookup, and an object it cannot see into a proven absence.
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
         * Reads every public synonym (`ALL_SYNONYMS` rows owned by `PUBLIC`), superseding any [publicSynonymsFor]
         * restriction. Oracle shows every public synonym to every session whatever its privileges on the target, so
         * the answer is complete and the metadata declares public synonyms complete: a name absent from it is not a
         * public synonym, and an empty answer means the database has none.
         */
        fun publicSynonyms(): Builder = apply { allPublicSynonyms = true }

        /**
         * Reads the public synonyms whose `TABLE_OWNER` is [targetSchema]; repeatable, the targets add up. Ignored
         * when [publicSynonyms] is also called. A restricted answer is never complete: an empty one means no public
         * synonym points into the given schemas, while public synonyms for other schemas may exist.
         *
         * @throws IllegalArgumentException for an invalid name
         */
        fun publicSynonymsFor(targetSchema: String): Builder = apply {
            publicSynonymTargets += OracleRelationName.dictionaryName(targetSchema, "schema")
        }

        /**
         * @throws IllegalStateException if no schema, relation or public synonym was requested, or if a schema asserted
         * complete was not requested with [schema]
         */
        fun build(): OracleMetadataRequest {
            val publicSynonyms = allPublicSynonyms || publicSynonymTargets.isNotEmpty()
            check(schemas.isNotEmpty() || relations.isNotEmpty() || publicSynonyms) {
                "At least one schema, relation or public synonym must be requested"
            }
            val unrequested = completeSchemas - schemas
            check(unrequested.isEmpty()) { "A schema asserted complete must also be requested with schema(): $unrequested" }
            val targets = if (allPublicSynonyms) emptySet() else publicSynonymTargets
            return OracleMetadataRequest(schemas, relations, completeSchemas, publicSynonyms, targets, queryTimeout)
        }
    }

    companion object {
        private const val PUBLIC = "PUBLIC"
        private val DEFAULT_QUERY_TIMEOUT: Duration = Duration.ofSeconds(60)

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
