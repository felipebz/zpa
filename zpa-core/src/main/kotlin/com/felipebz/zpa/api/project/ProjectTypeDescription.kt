//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList
import java.util.*

/**
 * Bounded, read-only description of a type name or reference, obtained by following **package subtype aliases and
 * supported source-backed `%TYPE` anchors (through package variables and constants, and through the record fields an
 * anchor names after a package value)** over the supplied project facts. It is not a datatype graph and not an Oracle
 * type resolver: a description stops at the first non-subtype declaration (record fields are traversed only along the
 * field path of an anchor such as `p.g_rec.f%TYPE`; a collection element never is), at a built-in name, at an anchor
 * this source-only resolver does not resolve, at `REF` syntax, at an unresolved lookup or at a cycle.
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
    writtenAnchor: ProjectWrittenAnchor?,
    fieldPath: List<ProjectIdentifier> = emptyList()
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

        /**
         * A field path that is not followed: a competing reading of the name exists (a shorter and a longer package
         * value, or a package value and another declaration of the same name), the value it starts from is not
         * identified uniquely, or the type reached does not prove the field (several fields of the name, an object
         * type whose attribute list is not fully known, a method, a type of unknown shape).
         */
        FIELD_PATH,

        /** A scope this resolver does not model (sibling field, parameter, a declaration of a package body's own level,
         * or body names not known for the name, non-value package declaration, unidentifiable reference holder, ambiguous
         * package specification, standalone name). */
        ENCLOSING_SCOPE,

        /** The value is declared after, or is, the reference in the same package specification. */
        DECLARED_LATER,

        /** The anchor names an object behind a database link (`t.c@link%TYPE`): never read against project sources. */
        REMOTE,

        /**
         * The anchor selects a field the type of the package value does not have: a record or object type without a
         * field or attribute of that name, a collection, or a scalar. Oracle rejects such an anchor. [steps] ends with
         * the type examined (the value or subtype for a scalar).
         */
        FIELD_NOT_FOUND
    }

    /**
     * The followed package values and subtypes, and the record or object types a field path went through, in traversal
     * order, each occurrence once. Runtime-unmodifiable.
     */
    val steps: List<ProjectDeclarationView> = immutableList(steps)

    /** The value steps of [steps], in order. Runtime-unmodifiable. */
    val values: List<ProjectPackageValueView> = immutableList(steps.filterIsInstance<ProjectPackageValueView>())

    /** The subtype steps of [steps], in order. Runtime-unmodifiable. */
    val subtypes: List<ProjectSubtypeView> = immutableList(steps.filterIsInstance<ProjectSubtypeView>())

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

    /**
     * The fields an enclosing anchor still selects from the type this description stops at, outermost first; empty when
     * no field path is pending. `p.g_cust.name%TYPE`, with `g_cust customer%ROWTYPE`, stops at the anchor
     * `customer%ROWTYPE` with the path `name`: the status then describes the record whose field is meant, not the
     * requested type. Always empty for [Status.DECLARED_TYPE] and [Status.BUILT_IN]. Runtime-unmodifiable.
     */
    val fieldPath: List<ProjectIdentifier> = immutableList(fieldPath)

    init {
        val targets = (declaration != null) to (builtInCategory != null)
        val noExtras = resolution == null && valueResolution == null && anchorStop == null
        // A stop at the written request: only a written-anchor query, before any step was followed, or a field of the
        // request's own field path that was not selected after its value was followed.
        val fieldStop = anchorStop == AnchorStop.FIELD_PATH || anchorStop == AnchorStop.FIELD_NOT_FOUND
        val atRequest = terminalReference == null && writtenAnchor != null && (steps.isEmpty() || fieldStop)
        require(fieldPath.isEmpty() || (status != Status.DECLARED_TYPE && status != Status.BUILT_IN)) { "A reached type has no pending field path" }
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
                        (atRequest && writtenAnchor.anchor == ProjectTypeReference.Anchor.TYPE &&
                            valueResolution.name == writtenAnchor.name))
            ) { "Invalid UNRESOLVED_VALUE description" }
            Status.ANCHORED_REFERENCE -> require(
                targets == (false to false) && resolution == null && valueResolution == null && anchorStop != null &&
                    (terminalReference?.kind == ProjectTypeReference.Kind.ANCHORED ||
                        (atRequest && writtenAnchor.anchor ==
                            if (anchorStop == AnchorStop.ROWTYPE) ProjectTypeReference.Anchor.ROWTYPE else ProjectTypeReference.Anchor.TYPE)) &&
                    // A remote anchor always stops as REMOTE, and only a remote anchor does.
                    (anchorStop == AnchorStop.REMOTE) == (terminalReference?.link?.isPresent == true)
            ) { "Invalid ANCHORED_REFERENCE description" }
            Status.REF_REFERENCE -> require(
                targets == (false to false) && noExtras && terminalReference?.kind == ProjectTypeReference.Kind.REF
            ) { "Invalid REF_REFERENCE description" }
            Status.CYCLE -> require(targets == (false to false) && noExtras && steps.isNotEmpty()) { "Invalid CYCLE description" }
        }
    }

    override fun toString(): String = "$status ${steps.map { it.qualifiedName }}" + if (fieldPath.isEmpty()) "" else " .${fieldPath.joinToString(".")}"

    internal companion object {
        @JvmSynthetic
        fun declared(steps: List<ProjectDeclarationView>, declaration: ProjectTypeDeclarationView, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.DECLARED_TYPE, steps, declaration, null, terminal, null, null, null, written)

        @JvmSynthetic
        fun builtIn(steps: List<ProjectDeclarationView>, category: ProjectBuiltInTypeCategory, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null) =
            ProjectTypeDescription(Status.BUILT_IN, steps, null, category, terminal, null, null, null, written)

        @JvmSynthetic
        fun unresolved(
            steps: List<ProjectDeclarationView>, resolution: ProjectNamedTypeResolution, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null,
            fieldPath: List<ProjectIdentifier> = emptyList()
        ) = ProjectTypeDescription(Status.UNRESOLVED_NAME, steps, null, null, terminal, resolution, null, null, written, fieldPath)

        @JvmSynthetic
        fun unresolvedValue(
            steps: List<ProjectDeclarationView>, resolution: ProjectValueResolution, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null,
            fieldPath: List<ProjectIdentifier> = emptyList()
        ) = ProjectTypeDescription(Status.UNRESOLVED_VALUE, steps, null, null, terminal, null, resolution, null, written, fieldPath)

        @JvmSynthetic
        fun anchorStopped(
            steps: List<ProjectDeclarationView>, reference: ProjectTypeReference?, stop: AnchorStop, written: ProjectWrittenAnchor? = null,
            fieldPath: List<ProjectIdentifier> = emptyList()
        ) = ProjectTypeDescription(Status.ANCHORED_REFERENCE, steps, null, null, reference, null, null, stop, written, fieldPath)

        @JvmSynthetic
        fun refStopped(
            steps: List<ProjectDeclarationView>, reference: ProjectTypeReference, written: ProjectWrittenAnchor? = null,
            fieldPath: List<ProjectIdentifier> = emptyList()
        ) = ProjectTypeDescription(Status.REF_REFERENCE, steps, null, null, reference, null, null, null, written, fieldPath)

        @JvmSynthetic
        fun cycle(
            steps: List<ProjectDeclarationView>, terminal: ProjectTypeReference?, written: ProjectWrittenAnchor? = null,
            fieldPath: List<ProjectIdentifier> = emptyList()
        ) = ProjectTypeDescription(Status.CYCLE, steps, null, null, terminal, null, null, null, written, fieldPath)
    }
}
