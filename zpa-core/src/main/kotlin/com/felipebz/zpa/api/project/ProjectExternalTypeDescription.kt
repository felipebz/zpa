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
    externalCandidates: List<ProjectExternalRelation>
) {
    enum class Status {
        /** The unqualified reading binds to the found relation (and column): scope captures are excluded. */
        RESOLVED,

        /** The external reading does not apply: the source walk, a proven capture, a rejected anchor or the shape decides. */
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

        /** Two external readings were both found: the current-schema relation and the schema-qualified relation. */
        AMBIGUOUS,

        /** The supplied metadata neither confirms nor refutes the reading (coverage not declared, or partial columns). */
        INCOMPLETE,

        /** The relation is absent under declared complete schema coverage. */
        RELATION_NOT_FOUND,

        /** The column is absent under declared complete column coverage. */
        COLUMN_NOT_FOUND
    }

    /** The anchor kind interpreted: the stopped anchor, or a followed anchor that competes with a relation. */
    val anchor: Optional<ProjectTypeReference.Anchor> = Optional.ofNullable(anchor)

    /** The written name of that anchor. */
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
    val sourceCandidates: List<ProjectDeclarationView> = Collections.unmodifiableList(ArrayList(sourceCandidates))

    /** Competing or capturing external relations (see [Status.CONFLICT], [Status.AMBIGUOUS]). Runtime-unmodifiable. */
    val externalCandidates: List<ProjectExternalRelation> = Collections.unmodifiableList(ArrayList(externalCandidates))

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
            Status.AMBIGUOUS -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.FOUND && externalCandidates.size == 2) {
                "Invalid AMBIGUOUS description"
            }
            Status.UNAVAILABLE -> require(anchor != null && (lookup == null || lookupStatus == ProjectExternalLookup.Status.UNAVAILABLE) &&
                externalCandidates.isEmpty()) { "Invalid UNAVAILABLE description" }
            Status.SCHEMA_CONTEXT_MISSING -> require(anchor != null && relationName == null && lookup == null && externalCandidates.isEmpty()) {
                "Invalid SCHEMA_CONTEXT_MISSING description"
            }
            Status.INCOMPLETE -> require(anchored() && externalCandidates.isEmpty() && (lookupStatus == ProjectExternalLookup.Status.NOT_COVERED ||
                (lookupStatus == ProjectExternalLookup.Status.FOUND && !lookup!!.relation.get().isColumnsComplete))) { "Invalid INCOMPLETE description" }
            Status.RELATION_NOT_FOUND -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.RELATION_NOT_FOUND && externalCandidates.isEmpty()) {
                "Invalid RELATION_NOT_FOUND description"
            }
            Status.COLUMN_NOT_FOUND -> require(anchored() && lookupStatus == ProjectExternalLookup.Status.COLUMN_NOT_FOUND && externalCandidates.isEmpty()) {
                "Invalid COLUMN_NOT_FOUND description"
            }
            Status.SOURCE_INCOMPLETE -> require(externalCandidates.isEmpty()) { "Invalid SOURCE_INCOMPLETE description" }
            Status.CAPTURE_NOT_EXCLUDED -> Unit
        }
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
            externalCandidates: List<ProjectExternalRelation>
        ) = ProjectExternalTypeDescription(status, source, anchor, anchorName, relationName, lookup, sourceCandidates, externalCandidates)
    }
}
