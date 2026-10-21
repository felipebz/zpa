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
        val declaration = occurrence.declaration
        val parameters = declaration.callParameters()
        val result = ParameterListBinding.bind(declaration.callKind(), kind, parameters, ProjectParameter::name, ProjectParameter::defaultPresent, arguments)
        if (result.applicability != ProjectCallCandidate.Applicability.APPLICABLE) {
            return ProjectCallCandidate(
                occurrence, kind, arguments, result.applicability,
                missingParameters = result.missing, rejectedArgumentOrdinal = result.rejectedArgumentOrdinal
            )
        }
        val bindings = arguments.arguments.mapIndexed { index, argument ->
            ProjectArgumentBinding(index + 1, argument, parameters[result.formalIndices[index]])
        }
        return ProjectCallCandidate(
            occurrence, kind, arguments, ProjectCallCandidate.Applicability.APPLICABLE,
            bindings = bindings, omittedParameters = result.omitted
        )
    }
}
