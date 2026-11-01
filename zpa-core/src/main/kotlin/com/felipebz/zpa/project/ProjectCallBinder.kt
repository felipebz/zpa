//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
