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
package com.felipebz.zpa.project

/**
 * Binds written actuals to one parameter list: kind, label uniqueness, positional then named notation, and defaults.
 * Nothing else: no datatypes, no ranking. The arguments' notation must be valid.
 */
internal object ParameterListBinding {
    class Result<P>(
        val applicability: ProjectCallCandidate.Applicability,
        /** APPLICABLE only: for each actual, in written order, the zero-based index of its formal. */
        val formalIndices: IntArray,
        /** APPLICABLE only: default-present formals bound to no actual, in formal order. */
        val omitted: List<P>,
        /** MISSING_REQUIRED only: formals without default and without actual, in formal order. */
        val missing: List<P>,
        /** TOO_MANY_ARGUMENTS, UNKNOWN_LABEL or DUPLICATE_BINDING only: the first offending actual, one-based. */
        val rejectedArgumentOrdinal: Int?
    )

    private val NONE = IntArray(0)

    fun <P> bind(
        declaredKind: ProjectCallKind,
        requestedKind: ProjectCallKind,
        parameters: List<P>,
        name: (P) -> OracleIdentifier,
        hasDefault: (P) -> Boolean,
        arguments: ProjectCallArguments
    ): Result<P> {
        fun rejected(applicability: ProjectCallCandidate.Applicability, ordinal: Int? = null) =
            Result<P>(applicability, NONE, emptyList(), emptyList(), ordinal)

        require(arguments.notation == ProjectCallArguments.Notation.VALID) { "The notation must be valid" }
        if (declaredKind != requestedKind) return rejected(ProjectCallCandidate.Applicability.WRONG_KIND)
        val byLabel = HashMap<OracleIdentifier, Int>(parameters.size)
        for ((index, parameter) in parameters.withIndex()) {
            if (byLabel.put(name(parameter), index) != null) return rejected(ProjectCallCandidate.Applicability.INVALID_SIGNATURE)
        }
        val positionalCount = arguments.positionalCount
        if (positionalCount > parameters.size) {
            return rejected(ProjectCallCandidate.Applicability.TOO_MANY_ARGUMENTS, parameters.size + 1)
        }
        val bound = BooleanArray(parameters.size)
        val formalIndices = IntArray(arguments.arguments.size)
        for (index in 0 until positionalCount) {
            bound[index] = true
            formalIndices[index] = index
        }
        for (index in positionalCount until arguments.arguments.size) {
            val label = requireNotNull(arguments.arguments[index].label)
            val formalIndex = byLabel[label] ?: return rejected(ProjectCallCandidate.Applicability.UNKNOWN_LABEL, index + 1)
            if (bound[formalIndex]) return rejected(ProjectCallCandidate.Applicability.DUPLICATE_BINDING, index + 1)
            bound[formalIndex] = true
            formalIndices[index] = formalIndex
        }
        val (omitted, missing) = parameters.filterIndexed { index, _ -> !bound[index] }.partition(hasDefault)
        if (missing.isNotEmpty()) {
            return Result(ProjectCallCandidate.Applicability.MISSING_REQUIRED, NONE, emptyList(), missing, null)
        }
        return Result(ProjectCallCandidate.Applicability.APPLICABLE, formalIndices, omitted, emptyList(), null)
    }
}
