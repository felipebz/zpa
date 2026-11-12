//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList
import java.util.*

/**
 * A source type description composed with supplied external relation metadata. [source] is the unchanged result of
 * [ProjectSnapshot.describeType] for the same request; the external side ([lookup], [externalCandidates]) holds only
 * detached descriptors with their metadata origin. The two sides are never merged: an external relation never becomes
 * a project declaration, and a project declaration never acquires an external identity.
 *
 * Only [Status.RESOLVED] claims how Oracle binds the anchor; every other status reports what the supplied evidence
 * proves or leaves open. [Status.NOT_EXTERNAL] says the external reading does not apply at the reference (a proven
 * source capture, a source-resolved walk, an anchor Oracle rejects, or a shape no relation can have); it does not say
 * that the source compiles.
 */
@ZpaExperimentalApi
class ProjectExternalTypeDescription private constructor(
    val status: Status,
    val source: ProjectTypeDescription,
    anchor: ProjectTypeReference.Anchor?,
    anchorName: ProjectQualifiedName?,
    relationName: ProjectQualifiedName?,
    lookup: ProjectExternalLookup?,
    sourceCandidates: List<ProjectDeclarationView>,
    externalCandidates: List<ProjectExternalRelation>,
    synonyms: List<ProjectExternalSynonym>,
    capturingObject: ProjectExternalObject?
) {
    enum class Status {
        /**
         * The reading binds to the found relation (and column): scope captures are excluded. An unqualified relation
         * name is an object of the compilation schema, else its private synonym, else a public synonym ([synonyms]); a
         * schema-qualified anchor is read as a schema only when no object of the compilation schema and no public
         * synonym takes its first segment, which needs the compilation schema and the public synonyms complete.
         */
        RESOLVED,

        /**
         * The external reading does not apply: the source walk, a proven capture, a rejected anchor or the shape decides.
         * An object of the compilation schema, or a public synonym, that takes the first segment of a schema-qualified
         * anchor is [capturingObject]; a synonym chain that ends at an object other than a table or view is
         * [capturingObject] too, and one that resolves to itself (Oracle's ORA-01775) has none.
         */
        NOT_EXTERNAL,

        /** An external reading may be correct, but a capture or a source alternative the evidence cannot see is not excluded. */
        CAPTURE_NOT_EXCLUDED,

        /** Source preparation failed or did not happen: a missing file may hold a capturing declaration. */
        SOURCE_INCOMPLETE,

        /** A positive source interpretation and a positive external relation reading of the same anchor compete. */
        CONFLICT,

        /** No external metadata was supplied. */
        UNAVAILABLE,

        /** An unqualified reading needs the current schema, which was not supplied. */
        SCHEMA_CONTEXT_MISSING,

        /**
         * The supplied metadata neither confirms nor refutes the reading: coverage not declared, partial columns, a
         * synonym whose target the metadata does not hold, or a name a public synonym may take when public synonyms are
         * not complete.
         */
        INCOMPLETE,

        /**
         * No relation, private synonym or public synonym takes the name: its schema is complete and, for an unqualified
         * name, so are the public synonyms. Oracle may still resolve the written name through a path the metadata does
         * not model (for example `dual.dummy%TYPE`), so this is not proof that the reference is invalid.
         */
        RELATION_NOT_FOUND,

        /** The column is absent under declared complete column coverage. */
        COLUMN_NOT_FOUND,

        /**
         * The source walk stopped at an anchor behind a database link (`t.c@link%TYPE`), or a synonym leads to one. The
         * supplied metadata describes the local database only, so no lookup is performed; [anchor] and [anchorName]
         * give the anchor, [synonyms] the synonyms followed.
         */
        REMOTE
    }

    /**
     * The anchor kind interpreted: the stopped anchor, or a followed anchor that competes with a relation. A source walk
     * that stopped with a pending [ProjectTypeDescription.fieldPath] is interpreted as a `%TYPE` anchor: a field of a
     * `%ROWTYPE` value (`g_cust.name%TYPE`, `g_cust customer%ROWTYPE`) is read as the column `customer.name%TYPE`.
     */
    val anchor: Optional<ProjectTypeReference.Anchor> = Optional.ofNullable(anchor)

    /** The written name of that anchor, followed by the pending field path of the source walk, if any. */
    val anchorName: Optional<ProjectQualifiedName> = Optional.ofNullable(anchorName)

    /** The interpreted relation reading as `[schema, relation]`. */
    val relationName: Optional<ProjectQualifiedName> = Optional.ofNullable(relationName)

    /** The exact lookup of that reading, when it was performed. */
    val lookup: Optional<ProjectExternalLookup> = Optional.ofNullable(lookup)

    /**
     * Project declarations bearing on the result: the capturing or rejecting declaration for [Status.NOT_EXTERNAL],
     * the followed values for [Status.CONFLICT], and otherwise the possible captors (declarations named like the
     * anchor's prefixes) and conditionally compiled declarations the answer would rest on. Runtime-unmodifiable.
     */
    val sourceCandidates: List<ProjectDeclarationView> = immutableList(sourceCandidates)

    /** Competing external relations (see [Status.CONFLICT]). Runtime-unmodifiable. */
    val externalCandidates: List<ProjectExternalRelation> = immutableList(externalCandidates)

    /**
     * The synonyms the relation reading went through, in order: the synonym the written name found first, then the
     * synonym each target is, up to [relationName] (or to the database link or object it ends at). Empty when the
     * name is a relation itself. Runtime-unmodifiable.
     */
    val synonyms: List<ProjectExternalSynonym> = immutableList(synonyms)

    /** The external object that takes the anchor instead of a relation (see [Status.NOT_EXTERNAL]). */
    val capturingObject: Optional<ProjectExternalObject> = Optional.ofNullable(capturingObject)

    init {
        require((anchor == null) == (anchorName == null)) { "Anchor kind and name come together" }
        val lookupStatus = lookup?.status
        fun anchored() = anchor != null && relationName != null && lookup != null
        when (status) {
            Status.RESOLVED -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.FOUND &&
                sourceCandidates.isEmpty() && externalCandidates.isEmpty()) { "Invalid RESOLVED description" }
            Status.NOT_EXTERNAL -> require(relationName == null && lookup == null && externalCandidates.isEmpty()) { "Invalid NOT_EXTERNAL description" }
            Status.CONFLICT -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.FOUND &&
                sourceCandidates.isNotEmpty() && externalCandidates.isNotEmpty()) { "Invalid CONFLICT description" }
            Status.UNAVAILABLE -> require(anchor != null && (lookup == null || lookupStatus == ProjectExternalLookup.Status.UNAVAILABLE) &&
                externalCandidates.isEmpty()) { "Invalid UNAVAILABLE description" }
            Status.SCHEMA_CONTEXT_MISSING -> require(anchor != null && relationName == null && lookup == null && externalCandidates.isEmpty()) {
                "Invalid SCHEMA_CONTEXT_MISSING description"
            }
            Status.INCOMPLETE -> require(anchored() && externalCandidates.isEmpty() && (lookupStatus == ProjectExternalLookup.Status.NOT_COVERED ||
                (lookupStatus == ProjectExternalLookup.Status.FOUND && !lookup.relation.get().isColumnsComplete))) { "Invalid INCOMPLETE description" }
            Status.RELATION_NOT_FOUND -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.RELATION_NOT_FOUND && externalCandidates.isEmpty()) {
                "Invalid RELATION_NOT_FOUND description"
            }
            Status.COLUMN_NOT_FOUND -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.COLUMN_NOT_FOUND && externalCandidates.isEmpty()) {
                "Invalid COLUMN_NOT_FOUND description"
            }
            Status.SOURCE_INCOMPLETE -> require(externalCandidates.isEmpty()) { "Invalid SOURCE_INCOMPLETE description" }
            Status.CAPTURE_NOT_EXCLUDED -> Unit
            Status.REMOTE -> require(anchor != null && relationName == null && lookup == null && sourceCandidates.isEmpty() &&
                externalCandidates.isEmpty()) { "Invalid REMOTE description" }
        }
        require(capturingObject == null || status == Status.NOT_EXTERNAL || status == Status.CAPTURE_NOT_EXCLUDED) { "Only a capture has a capturing object" }
    }

    override fun toString(): String =
        "$status ${anchorName.map { "$it%${anchor.get()}" }.orElse("")}${relationName.map { " -> $it" }.orElse("")}".trimEnd()

    internal companion object {
        @JvmSynthetic
        fun of(
            status: Status,
            source: ProjectTypeDescription,
            anchor: ProjectTypeReference.Anchor?,
            anchorName: ProjectQualifiedName?,
            relationName: ProjectQualifiedName?,
            lookup: ProjectExternalLookup?,
            sourceCandidates: List<ProjectDeclarationView>,
            externalCandidates: List<ProjectExternalRelation>,
            synonyms: List<ProjectExternalSynonym> = emptyList(),
            capturingObject: ProjectExternalObject? = null
        ) = ProjectExternalTypeDescription(
            status, source, anchor, anchorName, relationName, lookup, sourceCandidates, externalCandidates, synonyms, capturingObject
        )
    }
}
