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
import com.felipebz.zpa.project.ProjectCallCandidate as InternalCallCandidate
import com.felipebz.zpa.project.ProjectParameter as InternalParameter

/**
 * The structural evaluation of one discovered candidate: kind and actual-to-formal association only, never datatypes.
 * Every parameter object is an element of [declaration]'s own [ProjectSubprogramView.parameters].
 */
@ZpaExperimentalApi
class ProjectCallCandidate private constructor(
    /** A package SPECIFICATION or STANDALONE procedure/function; ID and source range through it. */
    val declaration: ProjectSubprogramView,
    val applicability: Applicability,
    bindings: List<ProjectArgumentBinding>,
    omittedParameters: List<ProjectParameterView>,
    missingParameters: List<ProjectParameterView>,
    private val rejectedOrdinal: Int?,
    argumentCount: Int
) {
    enum class Applicability {
        APPLICABLE, WRONG_KIND, TOO_MANY_ARGUMENTS, UNKNOWN_LABEL,
        DUPLICATE_BINDING, MISSING_REQUIRED, INVALID_SIGNATURE
    }

    /** APPLICABLE only: one binding per actual, in written order. */
    val bindings: List<ProjectArgumentBinding> = Collections.unmodifiableList(ArrayList(bindings))

    /** APPLICABLE only: default-present formals bound to no actual, in formal order. */
    val omittedParameters: List<ProjectParameterView> = Collections.unmodifiableList(ArrayList(omittedParameters))

    /** MISSING_REQUIRED only: formals without default and without actual, in formal order. */
    val missingParameters: List<ProjectParameterView> = Collections.unmodifiableList(ArrayList(missingParameters))

    /** TOO_MANY_ARGUMENTS, UNKNOWN_LABEL or DUPLICATE_BINDING only: the first offending actual, one-based. */
    val rejectedArgumentOrdinal: OptionalInt
        get() = rejectedOrdinal?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    init {
        require(declaration.role == ProjectDeclarationRole.SPECIFICATION || declaration.role == ProjectDeclarationRole.STANDALONE) {
            "Call candidates are specification or standalone occurrences"
        }
        val applicable = applicability == Applicability.APPLICABLE
        require(applicable || (this.bindings.isEmpty() && this.omittedParameters.isEmpty())) {
            "Only APPLICABLE candidates carry bindings or omitted parameters"
        }
        require(this.missingParameters.isNotEmpty() == (applicability == Applicability.MISSING_REQUIRED)) {
            "Missing parameters are present exactly for MISSING_REQUIRED"
        }
        val rejecting = applicability == Applicability.TOO_MANY_ARGUMENTS ||
            applicability == Applicability.UNKNOWN_LABEL || applicability == Applicability.DUPLICATE_BINDING
        require((rejectedOrdinal != null) == rejecting && (rejectedOrdinal == null || rejectedOrdinal in 1..argumentCount)) {
            "A rejected ordinal is a valid actual ordinal, present exactly for a rejecting applicability"
        }
        require(!applicable || this.bindings.size == argumentCount) { "One binding per actual" }
        this.bindings.forEachIndexed { index, binding ->
            require(binding.argumentOrdinal == index + 1 && ownsParameter(binding.parameter)) {
                "Bindings are in written order and use the declaration's own formals"
            }
        }
        require(this.omittedParameters.all(::ownsParameter) && this.missingParameters.all(::ownsParameter)) {
            "Parameters are the declaration's own formals"
        }
    }

    private fun ownsParameter(parameter: ProjectParameterView): Boolean =
        declaration.parameters.getOrNull(parameter.ordinal - 1) === parameter

    override fun toString(): String = "$applicability ${declaration.id}"

    internal companion object {
        /**
         * Projects one already evaluated internal candidate onto [declaration], its public view. Internal formals are
         * replaced by the view's parameter at the same one-based ordinal; internal actuals by [arguments]' own objects.
         */
        @JvmSynthetic
        fun of(
            candidate: InternalCallCandidate,
            declaration: ProjectSubprogramView,
            arguments: ProjectCallArguments
        ): ProjectCallCandidate {
            fun formal(parameter: InternalParameter) = declaration.parameters[parameter.ordinal - 1]
            return ProjectCallCandidate(
                declaration,
                when (candidate.applicability) {
                    InternalCallCandidate.Applicability.APPLICABLE -> Applicability.APPLICABLE
                    InternalCallCandidate.Applicability.WRONG_KIND -> Applicability.WRONG_KIND
                    InternalCallCandidate.Applicability.TOO_MANY_ARGUMENTS -> Applicability.TOO_MANY_ARGUMENTS
                    InternalCallCandidate.Applicability.UNKNOWN_LABEL -> Applicability.UNKNOWN_LABEL
                    InternalCallCandidate.Applicability.DUPLICATE_BINDING -> Applicability.DUPLICATE_BINDING
                    InternalCallCandidate.Applicability.MISSING_REQUIRED -> Applicability.MISSING_REQUIRED
                    InternalCallCandidate.Applicability.INVALID_SIGNATURE -> Applicability.INVALID_SIGNATURE
                },
                candidate.bindings.map {
                    ProjectArgumentBinding.create(it.argumentOrdinal, arguments.arguments[it.argumentOrdinal - 1], formal(it.parameter))
                },
                candidate.omittedParameters.map(::formal),
                candidate.missingParameters.map(::formal),
                candidate.rejectedArgumentOrdinal,
                arguments.arguments.size
            )
        }
    }
}
