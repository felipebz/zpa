//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.project.*
import com.felipebz.zpa.project.ProjectTypeShape as IndexedTypeShape

/**
 * The identity of a type created with CREATE TYPE: one the project declares ([Project], an object type or a
 * collection), or one only the external metadata has ([External], by its exact owner and name, of any kind). A name
 * that reaches a type the project declares is that declaration, so the two never stand for the same type.
 */
@OptIn(ZpaExperimentalApi::class)
internal sealed interface ObjectTypeRef {
    data class Project(val id: ProjectDeclarationId) : ObjectTypeRef

    data class External(val owner: ProjectIdentifier, val name: ProjectIdentifier) : ObjectTypeRef
}

/**
 * The type created with CREATE TYPE a written type name reaches, seen from a schema, as Oracle resolves it: an object
 * of the schema, then, for a one-segment name the schema is known not to hold, a public synonym; a synonym (one the
 * sources create first, then the metadata's) is followed to its target. The target is the project's declaration of
 * that schema and name, else the type the metadata has there. Nothing is reached without metadata.
 */
@OptIn(ZpaExperimentalApi::class)
internal class ObjectTypeNames(private val snapshot: ProjectSnapshot, private val metadata: ProjectExternalMetadata) {
    private val objects = snapshot.objects(metadata)
    private val enabled = metadata.origin.isPresent

    /**
     * The type the written name [segments] reaches from [schema]; null when it reaches none, or that is not known. Oracle
     * reads the first segment of `a.t` as an object of the unit's schema (a package, a type, a synonym), then as a public
     * synonym, and as the schema `a` only when neither takes it: a two-segment name is read as `owner.type` only when
     * no object is named like its first segment ([ProjectSnapshot.provesNoObjectNamed]).
     */
    fun reach(segments: List<OracleIdentifier>, schema: ProjectIdentifier?): ObjectTypeRef? {
        if (!enabled) return null
        val names = segments.map(ProjectIdentifier::wrap)
        return when (names.size) {
            1 -> schema?.let { follow(it, names[0], unqualified = true) }
            2 -> if (snapshot.provesNoObjectNamed(segments[0], schema, metadata)) follow(names[0], names[1], unqualified = false) else null
            else -> null
        }
    }

    /** The type the supertype written in the `UNDER` clause of [type] reaches, from the schema of its occurrence. */
    fun supertype(occurrence: IndexedDeclaration): ObjectTypeRef? {
        val type = occurrence.declaration as? StandaloneTypeDeclaration ?: return null
        val written = type.supertype as? NamedTypeRef ?: return null
        val schema = snapshot.projectIndex.effectiveSchema(occurrence)?.let(ProjectIdentifier::wrap)
        return reach(written.name.segments, schema)
    }

    /**
     * The schema type a package subtype stands for, when the project describes nothing past it: [description] ends,
     * unresolved, after a subtype whose base type is a name of one or two segments, which reaches that type from the
     * schema of the subtype's package. A one-segment name counts only when no member of that package may take it
     * ([shadowed]) and it names no predefined type. A subtype of a schema type is that type.
     */
    fun throughSubtypes(description: ProjectExternalTypeDescription, shadowed: (QualifiedName, OracleIdentifier) -> Boolean): ObjectTypeRef? {
        val source = description.source
        if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE || source.status != ProjectTypeDescription.Status.UNRESOLVED_NAME) return null
        val subtype = source.steps.lastOrNull() as? ProjectSubtypeView ?: return null
        val base = subtype.baseType.takeIf { it.kind == ProjectTypeReference.Kind.NAMED } ?: return null
        val segments = base.name.name.segments
        if (segments.size == 1) {
            val name = segments.single()
            if (base.writtenIdentity != null || DatatypeFamilies.undeclaredTypeKind(name) != CompositeKind.SCHEMA_OBJECT) return null
            if (shadowed(subtype.owner.name, name)) return null
        }
        val occurrence = snapshot.occurrenceOf(subtype.id) ?: return null
        return reach(segments, snapshot.projectIndex.effectiveSchema(occurrence)?.let(ProjectIdentifier::wrap))
    }

    /** The object type [owner].[name], a name the dictionary writes exactly (a supertype): the project's, else the metadata's. */
    fun exact(owner: ProjectIdentifier, name: ProjectIdentifier): ObjectTypeRef? =
        if (!enabled) null else project(owner, name) ?: ObjectTypeRef.External(owner, name).takeIf { type(it) != null }

    /** What the metadata says of [ref]. */
    fun type(ref: ObjectTypeRef.External): ProjectExternalObjectType? = metadata.findObjectType(ref.owner, ref.name).orElse(null)

    /** The object type a dictionary datatype with an owner names (an attribute's, a method result's), or null. */
    fun ofDatatype(datatype: ProjectExternalDatatype): ObjectTypeRef? {
        val owner = datatype.typeOwner.orElse(null) ?: return null
        val name = runCatching { dictionaryIdentifier(datatype.name, "type") }.getOrNull() ?: return null
        return exact(owner, name)
    }

    private fun follow(schema: ProjectIdentifier, name: ProjectIdentifier, unqualified: Boolean): ObjectTypeRef? {
        val seen = HashSet<Pair<ProjectIdentifier, ProjectIdentifier>>()
        var owner = schema
        var current = name
        var public = unqualified
        while (seen.add(owner to current)) {
            project(owner, current)?.let { return it }
            // Another object the project declares under that name takes it: it is not an object type the metadata describes.
            if (snapshot.projectIndex.findInSchema(owner.identifier, QualifiedName(listOf(current.identifier))).isNotEmpty()) return null
            val found = objects.findObject(owner, current)
            val synonym = when {
                found == null && public && metadata.isSchemaComplete(owner) -> objects.findSynonym(ProjectExternalSynonym.PUBLIC, current) ?: return null
                found == null -> return null
                found.kind == ProjectExternalObject.Kind.SYNONYM -> objects.findSynonym(owner, current) ?: return null
                found.kind == ProjectExternalObject.Kind.TYPE -> return ObjectTypeRef.External(owner, current).takeIf { type(it) != null }
                else -> return null
            }
            if (synonym.dbLink.isPresent) return null
            owner = synonym.targetOwner.orElse(null) ?: return null
            current = synonym.targetName
            public = false
        }
        // Oracle rejects a synonym that resolves to itself (ORA-01775).
        return null
    }

    /** The object type or collection the project declares once with schema [owner] and name [name]. */
    private fun project(owner: ProjectIdentifier, name: ProjectIdentifier): ObjectTypeRef? {
        val occurrence = snapshot.projectIndex.findInSchema(owner.identifier, QualifiedName(listOf(name.identifier)))
            .singleOrNull { (it.declaration as? StandaloneTypeDeclaration)?.shape in SCHEMA_TYPE_SHAPES } ?: return null
        return ObjectTypeRef.Project(snapshot.viewsOf(listOf(occurrence)).single().id)
    }

    private companion object {
        val SCHEMA_TYPE_SHAPES = setOf(IndexedTypeShape.OBJECT, IndexedTypeShape.COLLECTION)
    }
}

/**
 * How two types created with CREATE TYPE relate through their supertypes, the project's (`UNDER` clauses) and the
 * external metadata's (`SUPERTYPE_OWNER`, `SUPERTYPE_NAME`), along a chain that may mix both. A value of an object type
 * can be passed to a formal of that type or of one of its supertypes, and to no other schema type (PLS-00306); among
 * candidates that each take it, Oracle prefers the formal nearest to the value's own type. A collection or an opaque
 * type is under no type and no type is under it: it takes only a value of itself.
 */
@OptIn(ZpaExperimentalApi::class)
internal class ObjectTypeHierarchy(private val snapshot: ProjectSnapshot, metadata: ProjectExternalMetadata = ProjectExternalMetadata.none()) {
    private sealed interface Node

    private class ProjectNode(val occurrence: IndexedDeclaration) : Node

    private class ExternalNode(val type: ProjectExternalObjectType) : Node

    /** A type followed by its supertypes, nearest first; [complete] when the walk reached a type that is under none. */
    private class Chain(val types: List<Node>, val complete: Boolean)

    /** The names of object types, as this hierarchy follows them. */
    val names = ObjectTypeNames(snapshot, metadata)

    private val chains = HashMap<ObjectTypeRef, Chain?>()

    /**
     * How many supertype steps lead from the type [actual] up to [formal] (0 for the same type); null when [formal] is
     * not one of its types, or when either is not an object type this hierarchy knows.
     */
    fun distance(actual: ObjectTypeRef, formal: ObjectTypeRef): Int? {
        val chain = chain(actual) ?: return null
        val target = node(formal) ?: return null
        return chain.types.indexOfFirst { same(it, target) }.takeIf { it >= 0 }
    }

    /**
     * Whether a value of the type [actual] can be passed to a formal of the type [formal]: compatible when [formal] is
     * that type or one of its supertypes, incompatible when the whole supertype chain is known and does not reach it,
     * or when [formal] is a collection or an opaque type, which is no type's supertype; null when that cannot be told
     * (a supertype neither the project holds once nor the metadata has, a project with preparation failures).
     */
    fun outcome(actual: ObjectTypeRef, formal: ObjectTypeRef): Outcome? {
        if (distance(actual, formal) != null) return Outcome.COMPATIBLE
        val chain = chain(actual) ?: return null
        val target = node(formal) ?: return null
        if (target is ProjectNode && target.occurrence.declaration !is StandaloneTypeDeclaration) return null
        if (snapshot.failures.isNotEmpty()) return null
        return if (chain.complete || !isObjectType(target)) Outcome.INCOMPATIBLE else null
    }

    private fun isObjectType(node: Node): Boolean = when (node) {
        is ProjectNode -> (node.occurrence.declaration as? StandaloneTypeDeclaration)?.shape == IndexedTypeShape.OBJECT
        is ExternalNode -> node.type.kind == ProjectExternalObjectType.Kind.OBJECT
    }

    private fun node(ref: ObjectTypeRef): Node? = when (ref) {
        is ObjectTypeRef.Project -> snapshot.occurrenceOf(ref.id)?.let(::ProjectNode)
        is ObjectTypeRef.External -> names.type(ref)?.let(::ExternalNode)
    }

    private fun same(a: Node, b: Node): Boolean = when {
        a is ProjectNode && b is ProjectNode -> a.occurrence === b.occurrence
        a is ExternalNode && b is ExternalNode -> a.type.owner == b.type.owner && a.type.name == b.type.name
        else -> false
    }

    private fun chain(ref: ObjectTypeRef): Chain? = chains.getOrPut(ref) { walk(ref) }

    private fun walk(ref: ObjectTypeRef): Chain? {
        var node = node(ref) ?: return null
        if (node is ProjectNode && node.occurrence.declaration !is StandaloneTypeDeclaration) return null
        val types = arrayListOf(node)
        while (true) {
            val next = when (val current = node) {
                is ProjectNode -> {
                    val type = current.occurrence.declaration as StandaloneTypeDeclaration
                    if (type.supertype == null) return Chain(types, true)
                    val held = snapshot.projectIndex.supertypeOccurrences(type)
                    when (held.size) {
                        1 -> ProjectNode(held.single())
                        // A supertype the project does not declare may be one the metadata has.
                        0 -> names.supertype(current.occurrence)?.let(::node)
                        else -> null
                    }
                }
                is ExternalNode -> {
                    val owner = current.type.supertypeOwner.orElse(null) ?: return Chain(types, true)
                    names.exact(owner, current.type.supertypeName.get())?.let(::node)
                }
            } ?: return Chain(types, false)
            if (next is ProjectNode && next.occurrence.declaration !is StandaloneTypeDeclaration) return Chain(types, false)
            if (types.any { same(it, next) }) return Chain(types, false)
            types += next
            node = next
        }
    }
}
