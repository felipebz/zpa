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
 * Bounded, read-only description of a type name or reference, obtained by following **package subtype aliases only**
 * through the supplied project facts. It is not a datatype graph and not an Oracle type resolver: a description stops
 * at the first non-subtype declaration (its record fields or collection element are not traversed), at a built-in
 * name, at an anchored (`%TYPE`/`%ROWTYPE`) or `REF` reference, at an unresolved lookup or at an alias cycle.
 * [Status.UNRESOLVED_NAME] says only that the supplied project facts do not identify the type uniquely and completely;
 * it is not proof that the source is invalid or that Oracle lacks the type.
 */
@ZpaExperimentalApi
class ProjectTypeDescription private constructor(
    val status: Status,
    subtypes: List<ProjectSubtypeView>,
    declaration: ProjectTypeDeclarationView?,
    builtInCategory: ProjectBuiltInTypeCategory?,
    terminalReference: ProjectTypeReference?,
    resolution: ProjectNamedTypeResolution?
) {
    enum class Status {
        /** A non-subtype type declaration was reached: [declaration] is present. */
        DECLARED_TYPE,

        /** A recognized built-in name was reached: [builtInCategory] is present. */
        BUILT_IN,

        /** A named lookup did not resolve uniquely and completely: [resolution] holds the existing result. */
        UNRESOLVED_NAME,

        /** Interpretation stopped at `%TYPE` or `%ROWTYPE`, which is not resolved here. */
        ANCHORED_REFERENCE,

        /** Interpretation stopped at Oracle `REF` datatype syntax. */
        REF_REFERENCE,

        /** An already followed subtype occurrence was reached again. */
        CYCLE
    }

    /** The subtype aliases that were followed, in traversal order, each occurrence once. Runtime-unmodifiable. */
    val subtypes: List<ProjectSubtypeView> = Collections.unmodifiableList(ArrayList(subtypes))

    /** Present only for [Status.DECLARED_TYPE]. */
    val declaration: Optional<ProjectTypeDeclarationView> = Optional.ofNullable(declaration)

    /** Present only for [Status.BUILT_IN]. */
    val builtInCategory: Optional<ProjectBuiltInTypeCategory> = Optional.ofNullable(builtInCategory)

    /**
     * The last retained reference that was examined, with its real source range: the supplied reference, or the base
     * reference of the last followed subtype. Empty when a name-only query stopped at the initial name (a name has no
     * source range and none is invented).
     */
    val terminalReference: Optional<ProjectTypeReference> = Optional.ofNullable(terminalReference)

    /** Present only for [Status.UNRESOLVED_NAME]: the existing named-type result of the step that did not resolve. */
    val resolution: Optional<ProjectNamedTypeResolution> = Optional.ofNullable(resolution)

    init {
        val shared = (declaration != null) to (builtInCategory != null)
        when (status) {
            Status.DECLARED_TYPE -> require(shared == (true to false) && resolution == null) { "Invalid DECLARED_TYPE description" }
            Status.BUILT_IN -> require(shared == (false to true) && resolution == null) { "Invalid BUILT_IN description" }
            Status.UNRESOLVED_NAME -> require(
                shared == (false to false) && resolution != null && resolution.status != ProjectNamedTypeResolution.Status.RESOLVED
            ) { "Invalid UNRESOLVED_NAME description" }
            Status.ANCHORED_REFERENCE -> require(
                shared == (false to false) && resolution == null && terminalReference?.kind == ProjectTypeReference.Kind.ANCHORED
            ) { "Invalid ANCHORED_REFERENCE description" }
            Status.REF_REFERENCE -> require(
                shared == (false to false) && resolution == null && terminalReference?.kind == ProjectTypeReference.Kind.REF
            ) { "Invalid REF_REFERENCE description" }
            Status.CYCLE -> require(shared == (false to false) && resolution == null && subtypes.isNotEmpty()) {
                "Invalid CYCLE description"
            }
        }
    }

    override fun toString(): String = "$status ${subtypes.map { it.qualifiedName }}"

    internal companion object {
        @JvmSynthetic
        fun declared(subtypes: List<ProjectSubtypeView>, declaration: ProjectTypeDeclarationView, terminal: ProjectTypeReference?) =
            ProjectTypeDescription(Status.DECLARED_TYPE, subtypes, declaration, null, terminal, null)

        @JvmSynthetic
        fun builtIn(subtypes: List<ProjectSubtypeView>, category: ProjectBuiltInTypeCategory, terminal: ProjectTypeReference?) =
            ProjectTypeDescription(Status.BUILT_IN, subtypes, null, category, terminal, null)

        @JvmSynthetic
        fun unresolved(subtypes: List<ProjectSubtypeView>, resolution: ProjectNamedTypeResolution, terminal: ProjectTypeReference?) =
            ProjectTypeDescription(Status.UNRESOLVED_NAME, subtypes, null, null, terminal, resolution)

        @JvmSynthetic
        fun stopped(status: Status, subtypes: List<ProjectSubtypeView>, reference: ProjectTypeReference) =
            ProjectTypeDescription(status, subtypes, null, null, reference, null)

        @JvmSynthetic
        fun cycle(subtypes: List<ProjectSubtypeView>, terminal: ProjectTypeReference?) =
            ProjectTypeDescription(Status.CYCLE, subtypes, null, null, terminal, null)
    }
}
