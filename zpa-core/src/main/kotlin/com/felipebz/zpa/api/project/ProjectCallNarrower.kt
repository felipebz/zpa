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
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.UnknownReason
import com.felipebz.zpa.project.*

/**
 * Datatype narrowing over an existing structural resolution. Candidates and bindings are the structural objects;
 * formal types come from [ProjectSnapshot.describeExternalType] and [DatatypeFamilies]. Nothing is discovered, bound,
 * ranked or cached beyond one call.
 */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectCallNarrower {
    private class Evidence(val family: ProjectDatatypeFamily?, val reason: UnknownReason?)

    @JvmSynthetic
    fun narrow(
        snapshot: ProjectSnapshot,
        structural: ProjectCallResolution,
        argumentTypes: List<ProjectArgumentType>,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): ProjectTypedCallResolution {
        val types = ArrayList<ProjectArgumentType>(argumentTypes.size)
        for (type in argumentTypes as List<ProjectArgumentType?>) types += requireNotNull(type) { "Argument types must not contain null" }
        require(types.size == structural.arguments.arguments.size) {
            "Expected ${structural.arguments.arguments.size} argument types, got ${types.size}"
        }
        for (candidate in structural.candidates) {
            require(snapshot.occurrenceOf(candidate.declaration.id) != null) { "The call resolution was not issued by this snapshot" }
        }
        val shadowCache = HashMap<Pair<QualifiedName, OracleIdentifier>, Boolean>()
        fun shadowed(scope: QualifiedName, name: OracleIdentifier) =
            shadowCache.getOrPut(scope to name) { possiblyShadowed(snapshot, scope, name) }

        val actuals = types.map { actual(it, ::shadowed) }
        val evaluations = structural.candidates
            .filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
            .map { candidate ->
                val owner = (candidate.declaration as? ProjectPackageSubprogramView)?.packageOwner
                val bindings = candidate.bindings.map { binding ->
                    val actual = actuals[binding.argumentOrdinal - 1]
                    val formal = formal(snapshot, binding.parameter, owner, metadata, currentSchema, ::shadowed)
                    compare(binding, actual, formal)
                }
                ProjectCandidateCompatibility.of(candidate, bindings)
            }
        return ProjectTypedCallResolution.of(structural, evaluations)
    }

    private fun actual(type: ProjectArgumentType, shadowed: (QualifiedName, OracleIdentifier) -> Boolean): Evidence = when (type.kind) {
        ProjectArgumentType.Kind.UNKNOWN -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
        ProjectArgumentType.Kind.NULL_LITERAL -> Evidence(null, UnknownReason.NULL_LITERAL)
        else -> {
            val family = type.family.orElse(null)
            val scope = type.shadowScope
            val name = type.shadowName
            when {
                family != null && scope != null && name != null && shadowed(scope, name) -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
                family != null -> Evidence(family, null)
                type.unclassified -> Evidence(null, UnknownReason.UNCLASSIFIED_TYPE)
                else -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
            }
        }
    }

    private fun formal(
        snapshot: ProjectSnapshot,
        parameter: ProjectParameterView,
        owner: ProjectQualifiedName?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?,
        shadowed: (QualifiedName, OracleIdentifier) -> Boolean
    ): Evidence {
        val reference = parameter.typeRef
        if (DatatypeFamilies.isRowtypeReference(reference)) return Evidence(ProjectDatatypeFamily.COMPOSITE, null)
        val description = snapshot.describeExternalType(reference, owner, metadata, currentSchema)
        val family = DatatypeFamilies.ofDescription(description, null, reference)
        if (family == null) {
            return Evidence(null, if (DatatypeFamilies.isConclusive(description)) UnknownReason.UNCLASSIFIED_TYPE else UnknownReason.FORMAL_INCONCLUSIVE)
        }
        val builtIn = DatatypeFamilies.builtInName(description, null)
        val scope = builtIn?.let { DatatypeFamilies.builtInScope(description, owner?.name) }
        if (builtIn != null && scope != null && shadowed(scope, builtIn.name.segments.single())) {
            return Evidence(null, UnknownReason.FORMAL_INCONCLUSIVE)
        }
        return Evidence(family, null)
    }

    private fun compare(binding: ProjectArgumentBinding, actual: Evidence, formal: Evidence): ProjectBindingCompatibility {
        val reason = actual.reason ?: formal.reason
        if (reason != null) return ProjectBindingCompatibility.of(binding, Outcome.UNKNOWN, actual.family, formal.family, reason)
        return when (DatatypeFamilies.compatibility(actual.family, formal.family)) {
            DatatypeCompatibility.COMPATIBLE -> ProjectBindingCompatibility.of(binding, Outcome.COMPATIBLE, actual.family, formal.family, null)
            DatatypeCompatibility.INCOMPATIBLE -> ProjectBindingCompatibility.of(binding, Outcome.INCOMPATIBLE, actual.family, formal.family, null)
            DatatypeCompatibility.ENVIRONMENT_DEPENDENT ->
                ProjectBindingCompatibility.of(binding, Outcome.UNKNOWN, actual.family, formal.family, UnknownReason.ENVIRONMENT_DEPENDENT)
            DatatypeCompatibility.UNKNOWN ->
                ProjectBindingCompatibility.of(binding, Outcome.UNKNOWN, actual.family, formal.family, UnknownReason.UNSUPPORTED_PAIR)
        }
    }

    /**
     * A package member named like a predefined type shadows it inside the package (verified on Oracle 26ai: a package
     * `SUBTYPE date IS NUMBER` makes a formal `x date` a NUMBER). The predefined meaning is kept only when the
     * package has exactly one specification occurrence whose complete name coverage shows no member with that name.
     * Schema-level types do not shadow predefined names (a schema type STRING leaves a formal `x string` VARCHAR2).
     */
    private fun possiblyShadowed(snapshot: ProjectSnapshot, scope: QualifiedName, name: OracleIdentifier): Boolean {
        val index = snapshot.projectIndex
        val unit = index.findOccurrences(scope).singleOrNull {
            val d = it.declaration
            d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION
        } ?: return true
        return PackageMemberNames.at(index, unit, name, null).status != MemberNameStatus.NOT_DECLARED
    }
}
