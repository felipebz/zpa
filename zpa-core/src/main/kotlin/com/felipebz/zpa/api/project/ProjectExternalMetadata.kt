//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * Immutable snapshot of external (Oracle data dictionary) relations, object names, synonyms and object types supplied
 * by the consumer, with explicit coverage: a schema declared complete lists every relation and every object name (each
 * relation, synonym and object type of the schema is one of its objects), a relation declared column-complete lists
 * every user-generated column, and public synonyms declared complete list every public synonym. Object types are not
 * covered by a complete schema: a type absent here is not known to be absent. It is independent of any
 * [ProjectSnapshot], performs no I/O and is safe for concurrent reads. Acquisition, connections, caching and schema
 * aliases belong to the consumer.
 */
@ZpaExperimentalApi
class ProjectExternalMetadata private constructor(
    origin: String?,
    completeSchemas: Set<ProjectIdentifier>,
    relations: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>,
    objects: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObject>,
    synonyms: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalSynonym>,
    objectTypes: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObjectType>,
    /** Every public synonym is supplied. */
    val isPublicSynonymsComplete: Boolean
) {
    /** The opaque provenance label given to [builder]; empty only for [none]. */
    val origin: Optional<String> = Optional.ofNullable(origin)

    private val completeSchemas: Set<ProjectIdentifier> = Collections.unmodifiableSet(LinkedHashSet(completeSchemas))
    private val relations: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation> =
        Collections.unmodifiableMap(LinkedHashMap(relations))
    private val objects: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObject> = Collections.unmodifiableMap(LinkedHashMap(objects))
    private val synonyms: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalSynonym> = Collections.unmodifiableMap(LinkedHashMap(synonyms))
    private val objectTypes: Map<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObjectType> =
        Collections.unmodifiableMap(LinkedHashMap(objectTypes))

    /** Exact relation lookup: the caller asserts that the identity names a relation. */
    fun findRelation(schema: ProjectIdentifier, relation: ProjectIdentifier): ProjectExternalLookup =
        lookup(schema, relation, null)

    /** Exact column lookup of an asserted relation identity. */
    fun findColumn(schema: ProjectIdentifier, relation: ProjectIdentifier, column: ProjectIdentifier): ProjectExternalLookup =
        lookup(schema, relation, column)

    /** Whether [schema] was declared complete: every relation and object name of it is supplied. */
    fun isSchemaComplete(schema: ProjectIdentifier): Boolean = schema in completeSchemas

    /**
     * The object of [schema] named [name]: a supplied object name, relation (as a table or view) or synonym. Empty
     * proves absence only when [isSchemaComplete] (for `PUBLIC`, [isPublicSynonymsComplete]).
     */
    fun findObject(schema: ProjectIdentifier, name: ProjectIdentifier): Optional<ProjectExternalObject> {
        val key = schema to name
        val found = objects[key]
            ?: relations[key]?.let { ProjectExternalObject.of(schema, name, if (it.isView) ProjectExternalObject.Kind.VIEW else ProjectExternalObject.Kind.TABLE) }
            ?: synonyms[key]?.let { ProjectExternalObject.of(schema, name, ProjectExternalObject.Kind.SYNONYM) }
            ?: objectTypes[key]?.let { ProjectExternalObject.of(schema, name, ProjectExternalObject.Kind.TYPE) }
        return Optional.ofNullable(found)
    }

    /** The synonym [owner].[name] (owner `PUBLIC` for a public synonym), when supplied. */
    fun findSynonym(owner: ProjectIdentifier, name: ProjectIdentifier): Optional<ProjectExternalSynonym> = Optional.ofNullable(synonyms[owner to name])

    /**
     * The object type [owner].[name], with what it declares, when supplied. The name is the type's own, not a synonym of
     * it: following a synonym is the reader's, from its own schema. Empty never proves absence.
     */
    fun findObjectType(owner: ProjectIdentifier, name: ProjectIdentifier): Optional<ProjectExternalObjectType> =
        Optional.ofNullable(objectTypes[owner to name])

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

    override fun toString(): String =
        origin.map { "external metadata $it (${relations.size} relations, ${synonyms.size} synonyms, ${objectTypes.size} object types)" }.orElse("no external metadata")

    /**
     * Collects coverage, relations, object names, synonyms and object types; not thread-safe. [build] copies its state,
     * so later changes never affect a built snapshot. Obtained only through [ProjectExternalMetadata.builder].
     */
    sealed class Builder {
        /** Declares that every relation and object name of [dictionarySchema] (an exact dictionary name) is supplied. */
        abstract fun completeSchema(dictionarySchema: String): Builder

        /** @throws IllegalArgumentException if a relation with the same schema and name was already added */
        abstract fun relation(relation: ProjectExternalRelation): Builder

        /**
         * Adds an object name. The same object may be added again; @throws IllegalArgumentException if another kind
         * was already added under its schema and name
         */
        abstract fun schemaObject(schemaObject: ProjectExternalObject): Builder

        /** @throws IllegalArgumentException if a synonym with the same owner and name was already added */
        abstract fun synonym(synonym: ProjectExternalSynonym): Builder

        /**
         * Adds an object type with what it declares; it is also an object name of kind TYPE. @throws
         * IllegalArgumentException if a type with the same owner and name was already added
         */
        abstract fun objectType(objectType: ProjectExternalObjectType): Builder

        /** Declares that every public synonym is supplied. */
        abstract fun completePublicSynonyms(): Builder

        abstract fun build(): ProjectExternalMetadata
    }

    private class CollectingBuilder(private val origin: String) : Builder() {
        private val completeSchemas = LinkedHashSet<ProjectIdentifier>()
        private val relations = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalRelation>()
        private val objects = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObject>()
        private val synonyms = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalSynonym>()
        private val objectTypes = LinkedHashMap<Pair<ProjectIdentifier, ProjectIdentifier>, ProjectExternalObjectType>()
        private var publicSynonymsComplete = false

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

        override fun schemaObject(schemaObject: ProjectExternalObject): Builder {
            val key = schemaObject.schema to schemaObject.name
            val previous = objects.putIfAbsent(key, schemaObject)
            require(previous == null || previous == schemaObject) { "Conflicting external objects: $previous and $schemaObject" }
            return this
        }

        override fun synonym(synonym: ProjectExternalSynonym): Builder {
            val key = synonym.owner to synonym.name
            require(key !in synonyms) { "Duplicate external synonym: $synonym" }
            synonyms[key] = synonym
            return this
        }

        override fun objectType(objectType: ProjectExternalObjectType): Builder {
            val key = objectType.owner to objectType.name
            require(key !in objectTypes) { "Duplicate external object type: $objectType" }
            objectTypes[key] = objectType
            return this
        }

        override fun completePublicSynonyms(): Builder {
            publicSynonymsComplete = true
            return this
        }

        override fun build(): ProjectExternalMetadata =
            ProjectExternalMetadata(origin, completeSchemas, relations, objects, synonyms, objectTypes, publicSynonymsComplete)
    }

    companion object {
        private const val SYSTEM_PREFIX = "SYS_"
        private val NONE = ProjectExternalMetadata(null, emptySet(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), false)

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
