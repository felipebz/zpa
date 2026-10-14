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

/** Structural applicability only: defaults and labels, never datatype ranking or actual expressions. */
internal class ProjectCallBinder(context: ProjectAnalysisContext) {
    private val finder = ProjectCallCandidateFinder(context)

    fun resolve(
        target: ProjectCallTarget,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments
    ): ProjectCallResolution {
        if (arguments.notation != ProjectCallArguments.Notation.VALID) {
            return ProjectCallResolution(
                ProjectCallResolution.Status.INVALID_NOTATION, target, kind, arguments,
                emptyList(), emptySet(), emptyList(), null, emptyList(), false
            )
        }
        val discovered = finder.find(target)
        val candidates = discovered.candidates.map { evaluate(it, kind, arguments) }
        val status = callResolutionStatus(discovered.status, candidates)
        val selected = if (status == ProjectCallResolution.Status.RESOLVED) {
            candidates.single { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
        } else null
        return ProjectCallResolution(
            status, target, kind, arguments, candidates, discovered.uncertainties,
            discovered.packageSpecifications, selected, discovered.failures, discovered.packageEvidence
        )
    }

    private fun evaluate(
        occurrence: IndexedDeclaration,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments
    ): ProjectCallCandidate {
        fun rejected(applicability: ProjectCallCandidate.Applicability, ordinal: Int? = null) =
            ProjectCallCandidate(occurrence, kind, arguments, applicability, rejectedArgumentOrdinal = ordinal)

        val declaration = occurrence.declaration
        if (declaration.callKind() != kind) return rejected(ProjectCallCandidate.Applicability.WRONG_KIND)
        val parameters = declaration.callParameters()
        val byLabel = HashMap<OracleIdentifier, Int>(parameters.size)
        for ((index, parameter) in parameters.withIndex()) {
            if (byLabel.put(parameter.name, index) != null) {
                return rejected(ProjectCallCandidate.Applicability.INVALID_SIGNATURE)
            }
        }
        if (arguments.arguments.isEmpty()) {
            return if (parameters.isCallableWithoutArguments()) {
                ProjectCallCandidate(
                    occurrence, kind, arguments, ProjectCallCandidate.Applicability.APPLICABLE,
                    omittedParameters = parameters
                )
            } else {
                ProjectCallCandidate(
                    occurrence, kind, arguments, ProjectCallCandidate.Applicability.MISSING_REQUIRED,
                    missingParameters = parameters.filter { !it.defaultPresent }
                )
            }
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
            val formalIndex = byLabel[label]
                ?: return rejected(ProjectCallCandidate.Applicability.UNKNOWN_LABEL, index + 1)
            if (bound[formalIndex]) return rejected(ProjectCallCandidate.Applicability.DUPLICATE_BINDING, index + 1)
            bound[formalIndex] = true
            formalIndices[index] = formalIndex
        }
        val (omitted, missing) = parameters.filterIndexed { index, _ -> !bound[index] }.partition { it.defaultPresent }
        if (missing.isNotEmpty()) {
            return ProjectCallCandidate(
                occurrence, kind, arguments, ProjectCallCandidate.Applicability.MISSING_REQUIRED,
                missingParameters = missing
            )
        }
        val bindings = arguments.arguments.mapIndexed { index, argument ->
            ProjectArgumentBinding(index + 1, argument, parameters[formalIndices[index]])
        }
        return ProjectCallCandidate(
            occurrence, kind, arguments, ProjectCallCandidate.Applicability.APPLICABLE,
            bindings = bindings, omittedParameters = omitted
        )
    }
}
