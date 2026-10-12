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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * Immutable snapshot of external (Oracle data dictionary) relations supplied by the consumer, with explicit coverage:
 * a schema declared complete lists every relation, and a relation declared column-complete lists every user-generated
 * column. It is independent of any [ProjectSnapshot], performs no I/O and is safe for concurrent reads. Acquisition,
 * connections, caching, synonyms and schema aliases belong to the consumer.
 */
@ZpaExperimentalApi
class ProjectExternalMetadata private constructor(
    origin: String?,
    completeSchemas: Set<ProjectIdentifier>,
    relations: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>
) {
    /** The opaque provenance label given to [builder]; empty only for [none]. */
    val origin: Optional<String> = Optional.ofNullable(origin)

    private val completeSchemas: Set<ProjectIdentifier> = Collections.unmodifiableSet(LinkedHashSet(completeSchemas))
    private val relations: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation> =
        Collections.unmodifiableMap(LinkedHashMap(relations))

    /** Exact relation lookup: the caller asserts that the identity names a relation. */
    fun findRelation(schema: ProjectIdentifier, relation: ProjectIdentifier): ProjectExternalLookup =
        lookup(schema, relation, null)

    /** Exact column lookup of an asserted relation identity. */
    fun findColumn(schema: ProjectIdentifier, relation: ProjectIdentifier, column: ProjectIdentifier): ProjectExternalLookup =
        lookup(schema, relation, column)

    private fun lookup(schema: ProjectIdentifier, relationName: ProjectIdentifier, columnName: ProjectIdentifier?): ProjectExternalLookup {
        val label = origin.orElse(null)
            ?: return ProjectExternalLookup.of(ProjectExternalLookup.Status.UNAVAILABLE, schema, relationName, columnName, null, null, null)
        val relation = relations[schema to relationName]
        if (relation == null) {
            val status = if (schema in completeSchemas) ProjectExternalLookup.Status.RELATION_NOT_FOUND else ProjectExternalLookup.Status.NOT_COVERED
            return ProjectExternalLookup.of(status, schema, relationName, columnName, null, null, label)
        }
        if (columnName == null) {
            return ProjectExternalLookup.of(ProjectExternalLookup.Status.FOUND, schema, relationName, null, relation, null, label)
        }
        val column = relation.column(columnName)
        val status = when {
            column != null -> ProjectExternalLookup.Status.FOUND
            relation.isColumnsComplete && !columnName.identifier.lookupName.startsWith(SYSTEM_PREFIX) -> ProjectExternalLookup.Status.COLUMN_NOT_FOUND
            else -> ProjectExternalLookup.Status.NOT_COVERED
        }
        return ProjectExternalLookup.of(status, schema, relationName, columnName, relation, column, label)
    }

    override fun toString(): String = origin.map { "external metadata $it (${relations.size} relations)" }.orElse("no external metadata")

    /**
     * Collects coverage and relations; not thread-safe. [build] copies its state, so later changes never affect a
     * built snapshot. Obtained only through [ProjectExternalMetadata.builder].
     */
    sealed class Builder {
        /** Declares that every relation of [dictionarySchema] (an exact dictionary name) is supplied. */
        abstract fun completeSchema(dictionarySchema: String): Builder

        /** @throws IllegalArgumentException if a relation with the same schema and name was already added */
        abstract fun relation(relation: ProjectExternalRelation): Builder

        abstract fun build(): ProjectExternalMetadata
    }

    private class CollectingBuilder(private val origin: String) : Builder() {
        private val completeSchemas = LinkedHashSet<ProjectIdentifier>()
        private val relations = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>()

        override fun completeSchema(dictionarySchema: String): Builder {
            completeSchemas += dictionaryIdentifier(dictionarySchema, "schema")
            return this
        }

        override fun relation(relation: ProjectExternalRelation): Builder {
            val key = relation.schema to relation.name
            require(key !in relations) { "Duplicate external relation: ${relation.qualifiedName}" }
            relations[key] = relation
            return this
        }

        override fun build(): ProjectExternalMetadata = ProjectExternalMetadata(origin, completeSchemas, relations)
    }

    companion object {
        private const val SYSTEM_PREFIX = "SYS_"
        private val NONE = ProjectExternalMetadata(null, emptySet(), emptyMap())

        /** No external metadata: every lookup is UNAVAILABLE. */
        @JvmStatic
        fun none(): ProjectExternalMetadata = NONE

        /** @param origin an opaque, nonblank provenance label (for example a database and acquisition time) */
        @JvmStatic
        fun builder(origin: String): Builder {
            require(origin.isNotBlank()) { "The origin must not be blank" }
            return CollectingBuilder(origin)
        }
    }
}
