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

import java.util.*

/** A namespace assertion or a written name; no schema or lexical scope is inferred. */
internal class ProjectCallTarget private constructor(
    val form: Form,
    val name: QualifiedName,
    val packageName: QualifiedName?
) {
    enum class Form { PACKAGE_MEMBER, STANDALONE, WRITTEN }

    internal val memberPackageName: QualifiedName? = when (form) {
        Form.PACKAGE_MEMBER -> packageName
        Form.WRITTEN -> if (name.segments.size in 2..3) QualifiedName(name.segments.dropLast(1)) else null
        Form.STANDALONE -> null
    }
    internal val permitsStandalone: Boolean = form != Form.PACKAGE_MEMBER && name.segments.size <= 2

    companion object {
        fun packageMember(packageName: QualifiedName, name: OracleIdentifier) =
            ProjectCallTarget(Form.PACKAGE_MEMBER, packageName.append(name), packageName)
        fun standalone(name: QualifiedName) = ProjectCallTarget(Form.STANDALONE, name, null)
        fun written(name: QualifiedName) = ProjectCallTarget(Form.WRITTEN, name, null)
    }
}

internal enum class ProjectCallKind { PROCEDURE, FUNCTION }
internal enum class ProjectCallUncertainty { NAMESPACE_COMPETITION, DUPLICATE_SPECIFICATION, PARTIAL_DECLARATIONS }

internal class ProjectCallArgument private constructor(val label: OracleIdentifier?) {
    val isNamed: Boolean get() = label != null

    companion object {
        fun positional() = ProjectCallArgument(null)
        fun named(label: OracleIdentifier) = ProjectCallArgument(label)
    }
}

/** Notation errors are classified once, independently of project preparation and candidates. */
internal class ProjectCallArguments private constructor(arguments: Collection<ProjectCallArgument>) {
    val arguments: List<ProjectCallArgument> = immutableList(arguments)
    val notation: Notation
    val invalidArgumentOrdinal: Int?
    internal val positionalCount: Int

    enum class Notation { VALID, POSITIONAL_AFTER_NAMED, DUPLICATE_LABEL }

    init {
        var namedSeen = false
        val labels = HashSet<OracleIdentifier>()
        var error = Notation.VALID
        var offendingOrdinal: Int? = null
        var positional = 0
        for ((index, argument) in this.arguments.withIndex()) {
            val label = argument.label
            if (label == null) {
                if (namedSeen) {
                    error = Notation.POSITIONAL_AFTER_NAMED
                    offendingOrdinal = index + 1
                    break
                }
                positional++
            } else {
                namedSeen = true
                if (!labels.add(label)) {
                    error = Notation.DUPLICATE_LABEL
                    offendingOrdinal = index + 1
                    break
                }
            }
        }
        notation = error
        invalidArgumentOrdinal = offendingOrdinal
        positionalCount = positional
    }

    companion object {
        fun none() = ProjectCallArguments(emptyList())
        fun of(arguments: Collection<ProjectCallArgument>) = ProjectCallArguments(arguments)
    }
}


internal class ProjectCallCandidates(
    val status: Status,
    val target: ProjectCallTarget,
    candidates: Collection<IndexedDeclaration>,
    uncertainties: Set<ProjectCallUncertainty>,
    packageSpecifications: Collection<IndexedDeclaration>,
    failures: Collection<ProjectIndexPreparationFailure>,
    val packageEvidence: Boolean
) {
    enum class Status {
        FOUND, NOT_FOUND_IN_PROJECT, UNKNOWN_PACKAGE, AMBIGUOUS_TARGET,
        PARTIAL_DECLARATIONS, INCOMPLETE_INDEX, NOT_PREPARED
    }

    val candidates: List<IndexedDeclaration> = immutableList(candidates)
    val uncertainties: Set<ProjectCallUncertainty> = immutableCallUncertainties(uncertainties)
    val packageSpecifications: List<IndexedDeclaration> = immutableList(packageSpecifications)
    val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)

    init {
        if (status == Status.NOT_PREPARED) {
            require(this.candidates.isEmpty() && this.uncertainties.isEmpty() && this.packageSpecifications.isEmpty())
            require(this.failures.isEmpty() && !packageEvidence)
        } else {
            validateCallDiscovery(target, this.candidates, this.packageSpecifications, packageEvidence)
        }
        require(this.failures.isNotEmpty() == (status == Status.INCOMPLETE_INDEX))
    }
}

internal data class ProjectArgumentBinding(
    val argumentOrdinal: Int,
    val argument: ProjectCallArgument,
    val parameter: ProjectParameter
) {
    init {
        require(argumentOrdinal > 0)
        require(argument.label == null || argument.label == parameter.name)
    }
}

internal class ProjectCallCandidate(
    val declaration: IndexedDeclaration,
    val kind: ProjectCallKind,
    val arguments: ProjectCallArguments,
    val applicability: Applicability,
    bindings: Collection<ProjectArgumentBinding> = emptyList(),
    omittedParameters: Collection<ProjectParameter> = emptyList(),
    missingParameters: Collection<ProjectParameter> = emptyList(),
    val rejectedArgumentOrdinal: Int? = null
) {
    enum class Applicability {
        APPLICABLE, WRONG_KIND, TOO_MANY_ARGUMENTS, UNKNOWN_LABEL,
        DUPLICATE_BINDING, MISSING_REQUIRED, INVALID_SIGNATURE
    }

    val bindings: List<ProjectArgumentBinding> = immutableList(bindings)
    val omittedParameters: List<ProjectParameter> = immutableList(omittedParameters)
    val missingParameters: List<ProjectParameter> = immutableList(missingParameters)

    init {
        require(declaration.declaration.isPublicCallCandidate())
        require(arguments.notation == ProjectCallArguments.Notation.VALID)
        val parameters = declaration.declaration.callParameters()
        require(parameters.withIndex().all { (index, parameter) -> parameter.ordinal == index + 1 }) {
            "Formal parameter ordinals must be contiguous and one-based"
        }
        require(this.missingParameters.isNotEmpty() == (applicability == Applicability.MISSING_REQUIRED))
        require((rejectedArgumentOrdinal != null) == when (applicability) {
            Applicability.TOO_MANY_ARGUMENTS, Applicability.UNKNOWN_LABEL, Applicability.DUPLICATE_BINDING -> true
            else -> false
        })
        if (rejectedArgumentOrdinal != null) require(rejectedArgumentOrdinal in 1..arguments.arguments.size)
        if (applicability == Applicability.APPLICABLE) {
            require(this.bindings.size == arguments.arguments.size)
            val bound = BooleanArray(parameters.size)
            for ((index, binding) in this.bindings.withIndex()) {
                require(binding.argumentOrdinal == index + 1 && binding.argument === arguments.arguments[index])
                val formalIndex = binding.parameter.ordinal - 1
                require(formalIndex in parameters.indices && binding.parameter === parameters[formalIndex])
                require(!bound[formalIndex])
                bound[formalIndex] = true
            }
            var omittedIndex = 0
            for ((index, parameter) in parameters.withIndex()) {
                if (!bound[index]) {
                    require(omittedIndex < this.omittedParameters.size && this.omittedParameters[omittedIndex] === parameter)
                    omittedIndex++
                }
            }
            require(omittedIndex == this.omittedParameters.size)
        } else {
            require(this.bindings.isEmpty() && this.omittedParameters.isEmpty())
        }
    }
}

internal class ProjectCallResolution(
    val status: Status,
    val target: ProjectCallTarget,
    val kind: ProjectCallKind,
    val arguments: ProjectCallArguments,
    candidates: Collection<ProjectCallCandidate>,
    uncertainties: Set<ProjectCallUncertainty>,
    packageSpecifications: Collection<IndexedDeclaration>,
    val selected: ProjectCallCandidate?,
    failures: Collection<ProjectIndexPreparationFailure>,
    val packageEvidence: Boolean
) {
    enum class Status {
        RESOLVED, AMBIGUOUS, NO_APPLICABLE_CANDIDATE, WRONG_KIND, NOT_FOUND_IN_PROJECT,
        UNKNOWN_PACKAGE, AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS, INCOMPLETE_INDEX,
        INVALID_NOTATION, NOT_PREPARED
    }

    val candidates: List<ProjectCallCandidate> = immutableList(candidates)
    val uncertainties: Set<ProjectCallUncertainty> = immutableCallUncertainties(uncertainties)
    val packageSpecifications: List<IndexedDeclaration> = immutableList(packageSpecifications)
    val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)

    init {
        require((status == Status.INVALID_NOTATION) == (arguments.notation != ProjectCallArguments.Notation.VALID))
        require(this.failures.isNotEmpty() == (status == Status.INCOMPLETE_INDEX))
        require((selected != null) == (status == Status.RESOLVED))
        if (status == Status.NOT_PREPARED || status == Status.INVALID_NOTATION) {
            require(this.candidates.isEmpty() && this.uncertainties.isEmpty() && this.packageSpecifications.isEmpty())
            require(!packageEvidence)
        } else {
            require(this.candidates.all { it.arguments === arguments && it.kind == kind })
            if (selected != null) {
                require(this.candidates.any { it === selected } && selected.applicability == ProjectCallCandidate.Applicability.APPLICABLE)
            }
        }
    }
}

private fun immutableCallUncertainties(values: Set<ProjectCallUncertainty>): Set<ProjectCallUncertainty> =
    Collections.unmodifiableSet(EnumSet.noneOf(ProjectCallUncertainty::class.java).apply { addAll(values) })

internal fun ProjectDeclaration.isPublicCallCandidate(): Boolean = when (this) {
    is PackageSubprogramDeclaration -> role == DeclarationRole.SPECIFICATION
    is StandaloneProcedureDeclaration, is StandaloneFunctionDeclaration -> true
    else -> false
}

internal fun ProjectDeclaration.callKind(): ProjectCallKind = when (this) {
    is PackageProcedureDeclaration, is StandaloneProcedureDeclaration -> ProjectCallKind.PROCEDURE
    is PackageFunctionDeclaration, is StandaloneFunctionDeclaration -> ProjectCallKind.FUNCTION
    else -> error("Not a subprogram declaration")
}

internal fun ProjectDeclaration.callParameters(): List<ProjectParameter> = when (this) {
    is PackageSubprogramDeclaration -> parameters
    is StandaloneProcedureDeclaration -> parameters
    is StandaloneFunctionDeclaration -> parameters
    else -> error("Not a subprogram declaration")
}

/** The structural definition shared by zero-actual procedure and function binding. No exact-zero preference. */
internal fun List<ProjectParameter>.isCallableWithoutArguments(): Boolean = all { it.defaultPresent }

internal fun callUncertainties(
    target: ProjectCallTarget,
    specifications: List<IndexedDeclaration>,
    memberInPlay: Boolean,
    standaloneInPlay: Boolean
): Set<ProjectCallUncertainty> = EnumSet.noneOf(ProjectCallUncertainty::class.java).apply {
    if (memberInPlay && standaloneInPlay) add(ProjectCallUncertainty.NAMESPACE_COMPETITION)
    if (memberInPlay && specifications.size > 1) add(ProjectCallUncertainty.DUPLICATE_SPECIFICATION)
    if (memberInPlay && specifications.any {
            val coverage = (it.declaration as PackageDeclaration).callableCoverage
            coverage.unknown || target.name.last.lookupName in coverage.omittedNames
        }) add(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
}

internal fun callDiscoveryStatus(
    candidates: List<IndexedDeclaration>,
    uncertainties: Set<ProjectCallUncertainty>,
    specifications: List<IndexedDeclaration>,
    failures: List<ProjectIndexPreparationFailure>,
    memberInPlay: Boolean,
    standaloneInPlay: Boolean
): ProjectCallCandidates.Status = when {
    failures.isNotEmpty() -> ProjectCallCandidates.Status.INCOMPLETE_INDEX
    ProjectCallUncertainty.NAMESPACE_COMPETITION in uncertainties ||
        ProjectCallUncertainty.DUPLICATE_SPECIFICATION in uncertainties -> ProjectCallCandidates.Status.AMBIGUOUS_TARGET
    ProjectCallUncertainty.PARTIAL_DECLARATIONS in uncertainties -> ProjectCallCandidates.Status.PARTIAL_DECLARATIONS
    memberInPlay && !standaloneInPlay && specifications.isEmpty() -> ProjectCallCandidates.Status.UNKNOWN_PACKAGE
    candidates.isEmpty() -> ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT
    else -> ProjectCallCandidates.Status.FOUND
}

internal fun callResolutionStatus(
    discovery: ProjectCallCandidates.Status,
    candidates: List<ProjectCallCandidate>
): ProjectCallResolution.Status = when (discovery) {
    ProjectCallCandidates.Status.NOT_PREPARED -> ProjectCallResolution.Status.NOT_PREPARED
    ProjectCallCandidates.Status.INCOMPLETE_INDEX -> ProjectCallResolution.Status.INCOMPLETE_INDEX
    ProjectCallCandidates.Status.AMBIGUOUS_TARGET -> ProjectCallResolution.Status.AMBIGUOUS_TARGET
    ProjectCallCandidates.Status.PARTIAL_DECLARATIONS -> ProjectCallResolution.Status.PARTIAL_DECLARATIONS
    ProjectCallCandidates.Status.UNKNOWN_PACKAGE -> ProjectCallResolution.Status.UNKNOWN_PACKAGE
    ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT -> ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT
    ProjectCallCandidates.Status.FOUND -> when {
        candidates.all { it.applicability == ProjectCallCandidate.Applicability.WRONG_KIND } -> ProjectCallResolution.Status.WRONG_KIND
        else -> when (candidates.count { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }) {
            0 -> ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE
            1 -> ProjectCallResolution.Status.RESOLVED
            else -> ProjectCallResolution.Status.AMBIGUOUS
        }
    }
}

private fun validateCallDiscovery(
    target: ProjectCallTarget,
    candidates: List<IndexedDeclaration>,
    specifications: List<IndexedDeclaration>,
    packageEvidence: Boolean
) {
    val owner = target.memberPackageName
    require(specifications.all {
        val declaration = it.declaration
        declaration is PackageDeclaration && declaration.role == DeclarationRole.SPECIFICATION && declaration.name == owner
    })
    require(specifications.isEmpty() || packageEvidence)
    require(owner != null || !packageEvidence)
    var standaloneSeen = false
    var previousMember: IndexedDeclaration? = null
    var previousStandalone: IndexedDeclaration? = null
    val seen = HashSet<IndexedDeclaration>()
    for (candidate in candidates) {
        require(seen.add(candidate) && candidate.declaration.isPublicCallCandidate())
        val declaration = candidate.declaration
        if (declaration is PackageSubprogramDeclaration) {
            require(!standaloneSeen && specifications.isNotEmpty())
            require(declaration.owner == owner && declaration.name == target.name.last)
            require(previousMember == null || occurrenceBefore(previousMember, candidate))
            previousMember = candidate
        } else {
            require(target.permitsStandalone && declaration.qualifiedName() == target.name)
            standaloneSeen = true
            require(previousStandalone == null || occurrenceBefore(previousStandalone, candidate))
            previousStandalone = candidate
        }
    }
    var previousSpecification: IndexedDeclaration? = null
    for (specification in specifications) {
        require(previousSpecification == null || occurrenceBefore(previousSpecification, specification))
        previousSpecification = specification
    }
}

private fun occurrenceBefore(first: IndexedDeclaration, second: IndexedDeclaration): Boolean =
    first.fileId.value < second.fileId.value || (first.fileId == second.fileId && first.ordinal < second.ordinal)
