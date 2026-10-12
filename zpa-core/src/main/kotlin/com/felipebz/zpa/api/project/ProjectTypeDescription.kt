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
 * Bounded, read-only description of a type name or reference, obtained by following **package subtype aliases and
 * supported source-backed `%TYPE` anchors (through package variables and constants)** over the supplied project facts.
 * It is not a datatype graph and not an Oracle type resolver: a description stops at the first non-subtype
 * declaration (its record fields or collection element are not traversed), at a built-in name, at an anchor this
 * source-only resolver does not resolve, at `REF` syntax, at an unresolved lookup or at a cycle.
 * [Status.UNRESOLVED_NAME] and [Status.UNRESOLVED_VALUE] say only that the supplied project facts do not identify the
 * target uniquely and completely; neither is proof that the source is invalid or that Oracle lacks the object.
 */
@ZpaExperimentalApi
class ProjectTypeDescription private constructor(
    val status: Status,
    steps: List<ProjectDeclarationView>,
    declaration: ProjectTypeDeclarationView?,
    builtInCategory: ProjectBuiltInTypeCategory?,
    terminalReference: ProjectTypeReference?,
    resolution: ProjectNamedTypeResolution?,
    valueResolution: ProjectValueResolution?,
    anchorStop: AnchorStop?,
    writtenAnchor: ProjectWrittenAnchor?
) {
    enum class Status {
        /** A non-subtype type declaration was reached: [declaration] is present. */
        DECLARED_TYPE,

        /** A recognized built-in name was reached: [builtInCategory] is present. */
        BUILT_IN,

        /** A named type lookup did not resolve uniquely and completely: [resolution] holds the existing result. */
        UNRESOLVED_NAME,

        /** Interpretation stopped at an anchor that is not resolved from source: [anchorStop] says why. */
        ANCHORED_REFERENCE,

        /** Interpretation stopped at Oracle `REF` datatype syntax. */
        REF_REFERENCE,

        /** An already followed value or subtype occurrence was reached again. */
        CYCLE,

        /** A `%TYPE` anchor named no uniquely and completely identified package value: [valueResolution] holds why. */
        UNRESOLVED_VALUE
    }

    /** Why an anchor was not resolved from source. */
    enum class AnchorStop {
        /** `%ROWTYPE`: not resolved from source here. */
        ROWTYPE,

        /** The name needs a record field path (or a competing field-path interpretation exists). */
        FIELD_PATH,

        /** A scope this resolver does not model (sibling field, parameter, body-private declaration, non-value
         * package declaration, unidentifiable reference holder, ambiguous package specification, standalone name). */
        ENCLOSING_SCOPE,

        /** The value is declared after, or is, the reference in the same package specification. */
        DECLARED_LATER
    }

    /** The followed package values and subtypes in traversal order, each occurrence once. Runtime-unmodifiable. */
    val steps: List<ProjectDeclarationView> = Collections.unmodifiableList(ArrayList(steps))

    /** The value steps of [steps], in order. Runtime-unmodifiable. */
    val values: List<ProjectPackageValueView> = Collections.unmodifiableList(steps.filterIsInstance<ProjectPackageValueView>())

    /** The subtype steps of [steps], in order. Runtime-unmodifiable. */
    val subtypes: List<ProjectSubtypeView> = Collections.unmodifiableList(steps.filterIsInstance<ProjectSubtypeView>())

    /** Present only for [Status.DECLARED_TYPE]. */
    val declaration: Optional<ProjectTypeDeclarationView> = Optional.ofNullable(declaration)

    /** Present only for [Status.BUILT_IN]. */
    val builtInCategory: Optional<ProjectBuiltInTypeCategory> = Optional.ofNullable(builtInCategory)

    /**
     * The last retained reference that was examined, with its real source range: the supplied reference, or the base
     * reference or declared type of the last followed subtype or value; for the anchor stops it is the anchored
     * reference responsible. Empty when a name-only query stopped at the initial name, or when a written-anchor query
     * stopped at the written anchor itself (then [writtenAnchor] is the responsible anchor). A name or a written anchor
     * has no source range and none is invented.
     */
    val terminalReference: Optional<ProjectTypeReference> = Optional.ofNullable(terminalReference)

    /**
     * The request, present for every description produced by [ProjectSnapshot.describeType] with a
     * [ProjectWrittenAnchor] (whatever the status), and empty for name and retained-reference queries.
     */
    val writtenAnchor: Optional<ProjectWrittenAnchor> = Optional.ofNullable(writtenAnchor)

    /** Present only for [Status.UNRESOLVED_NAME]: the existing named-type result of the step that did not resolve. */
    val resolution: Optional<ProjectNamedTypeResolution> = Optional.ofNullable(resolution)

    /** Present only for [Status.UNRESOLVED_VALUE]: the existing value lookup result that stopped the walk. */
    val valueResolution: Optional<ProjectValueResolution> = Optional.ofNullable(valueResolution)

    /** Present only for [Status.ANCHORED_REFERENCE]. */
    val anchorStop: Optional<AnchorStop> = Optional.ofNullable(anchorStop)

    init {
        val targets = (declaration != null) to (builtInCategory != null)
        val noExtras = resolution == null && valueResolution == null && anchorStop == null
        // A stop at the written request: only a written-anchor query, before any step was followed.
        val atRequest = terminalReference == null && writtenAnchor != null && steps.isEmpty()
        when (status) {
            Status.DECLARED_TYPE -> require(targets == (true to false) && noExtras) { "Invalid DECLARED_TYPE description" }
            Status.BUILT_IN -> require(targets == (false to true) && noExtras) { "Invalid BUILT_IN description" }
            Status.UNRESOLVED_NAME -> require(
                targets == (false to false) && resolution != null && valueResolution == null && anchorStop == null &&
                    resolution.status != ProjectNamedTypeResolution.Status.RESOLVED
            ) { "Invalid UNRESOLVED_NAME description" }
            Status.UNRESOLVED_VALUE -> require(
                targets == (false to false) && valueResolution != null && resolution == null && anchorStop == null &&
                    valueResolution.status != ProjectValueResolution.Status.RESOLVED &&
                    (terminalReference?.kind == ProjectTypeReference.Kind.ANCHORED ||
                        (atRequest && writtenAnchor!!.anchor == ProjectTypeReference.Anchor.TYPE &&
                            valueResolution.name == writtenAnchor.name))
            ) { "Invalid UNRESOLVED_VALUE description" }
            Status.ANCHORED_REFERENCE -> require(
                targets == (false to false) && resolution == null && valueResolution == null && anchorStop != null &&
                    (terminalReference?.kind == ProjectTypeReference.Kind.ANCHORED ||
                        (atRequest && writtenAnchor!!.anchor ==
                            if (anchorStop == AnchorStop.ROWTYPE) ProjectTypeReference.Anchor.ROWTYPE else ProjectTypeReference.Anchor.TYPE))
            ) { "Invalid ANCHORED_REFERENCE description" }
            Status.REF_REFERENCE -> require(
                targets == (false to false) && noExtras && terminalReference?.kind == ProjectTypeReference.Kind.REF
            ) { "Invalid REF_REFERENCE description" }
            Status.CYCLE -> require(targets == (false to false) && noExtras && steps.isNotEmpty()) { "Invalid CYCLE description" }
        }
    }

    override fun toString(): String = "$status ${steps.map { it.qualifiedName }}"

    internal companion object {
        @JvmSynthetic
        fun declared(steps: List<ProjectDeclarationView>, declaration: ProjectTypeDeclarationView, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.DECLARED_TYPE, steps, declaration, null, terminal, null, null, null, written)

        @JvmSynthetic
        fun builtIn(steps: List<ProjectDeclarationView>, category: ProjectBuiltInTypeCategory, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.BUILT_IN, steps, null, category, terminal, null, null, null, written)

        @JvmSynthetic
        fun unresolved(steps: List<ProjectDeclarationView>, resolution: ProjectNamedTypeResolution, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.UNRESOLVED_NAME, steps, null, null, terminal, resolution, null, null, written)

        @JvmSynthetic
        fun unresolvedValue(steps: List<ProjectDeclarationView>, resolution: ProjectValueResolution, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.UNRESOLVED_VALUE, steps, null, null, terminal, null, resolution, null, written)

        @JvmSynthetic
        fun anchorStopped(steps: List<ProjectDeclarationView>, reference: ProjectTypeReference?, stop: AnchorStop, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.ANCHORED_REFERENCE, steps, null, null, reference, null, null, stop, written)

        @JvmSynthetic
        fun refStopped(steps: List<ProjectDeclarationView>, reference: ProjectTypeReference, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.REF_REFERENCE, steps, null, null, reference, null, null, null, written)

        @JvmSynthetic
        fun cycle(steps: List<ProjectDeclarationView>, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.CYCLE, steps, null, null, terminal, null, null, null, written)
    }
}
