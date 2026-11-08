//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    /** The family of one side of a binding, or why it is unknown, and its exact type when known. */
    internal class Evidence(
        val family: ProjectDatatypeFamily?,
        val reason: UnknownReason?,
        val rank: OverloadRank? = null,
        val compositeKind: CompositeKind? = null
    )

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
        val shadowed = shadowCheck(snapshot)
        val actuals = types.map { actual(it, shadowed) }
        val formalRanks = ArrayList<List<OverloadRank?>>()
        val evaluations = structural.candidates
            .filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
            .map { candidate ->
                val declaration = candidate.declaration
                val owner = (declaration as? ProjectPackageSubprogramView)?.packageOwner
                val inSpecification = declaration.role == ProjectDeclarationRole.SPECIFICATION
                val ranks = arrayOfNulls<OverloadRank>(actuals.size)
                val bindings = candidate.bindings.map { binding ->
                    val actual = actuals[binding.argumentOrdinal - 1]
                    val formal = formal(snapshot, binding.parameter, owner, inSpecification, metadata, currentSchema, shadowed)
                    ranks[binding.argumentOrdinal - 1] = formal.rank
                    compare(actual, formal) { outcome, reason ->
                        ProjectBindingCompatibility.of(binding, outcome, actual.family, formal.family, reason)
                    }
                }
                formalRanks += ranks.toList()
                ProjectCandidateCompatibility.of(candidate, bindings)
            }
        return ProjectTypedCallResolution.of(structural, evaluations, preferred(actuals, evaluations, formalRanks))
    }

    /**
     * Among several retained candidates, the one Oracle binds ([OverloadRank.select]), or null. Every retained candidate
     * must be proven to receive the call: each binding compatible, or depending only on PLSQL_IMPLICIT_CONVERSION_BOOL
     * (measured with the setting on and off: such a binding never makes its candidate the preferred one).
     */
    internal fun preferred(actuals: List<Evidence>, evaluations: List<ProjectCandidateCompatibility>, formalRanks: List<List<OverloadRank?>>): ProjectCallCandidate? {
        val retained = evaluations.indices.filter { evaluations[it].outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
        if (retained.size < 2) return null
        val proven = retained.all { index ->
            evaluations[index].bindings.all {
                it.outcome == Outcome.COMPATIBLE || it.unknownReason.orElse(null) == UnknownReason.ENVIRONMENT_DEPENDENT
            }
        }
        if (!proven) return null
        val selected = OverloadRank.select(actuals.map { it.rank }, retained.map { formalRanks[it] }) ?: return null
        return evaluations[retained[selected]].candidate
    }

    /** Whether a package member may shadow a predefined type name, answered once per scope and name. */
    internal fun shadowCheck(snapshot: ProjectSnapshot): (QualifiedName, OracleIdentifier) -> Boolean {
        val cache = HashMap<Pair<QualifiedName, OracleIdentifier>, Boolean>()
        return { scope, name -> cache.getOrPut(scope to name) { possiblyShadowed(snapshot, scope, name) } }
    }

    internal fun actual(type: ProjectArgumentType, shadowed: (QualifiedName, OracleIdentifier) -> Boolean): Evidence = when (type.kind) {
        ProjectArgumentType.Kind.UNKNOWN -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
        ProjectArgumentType.Kind.NULL_LITERAL -> Evidence(null, UnknownReason.NULL_LITERAL)
        else -> {
            val family = type.family.orElse(null)
            val scope = type.shadowScope
            val name = type.shadowName
            when {
                family != null && scope != null && name != null && shadowed(scope, name) -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
                family != null -> Evidence(family, null, type.rank, type.compositeKind)
                type.unclassified -> Evidence(null, UnknownReason.UNCLASSIFIED_TYPE)
                else -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
            }
        }
    }

    private fun formal(
        snapshot: ProjectSnapshot,
        parameter: ProjectParameterView,
        owner: ProjectQualifiedName?,
        inSpecification: Boolean,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?,
        shadowed: (QualifiedName, OracleIdentifier) -> Boolean
    ): Evidence {
        val reference = parameter.typeRef
        if (DatatypeFamilies.isRowtypeReference(reference)) return Evidence(ProjectDatatypeFamily.COMPOSITE, null)
        val description = snapshot.describeExternalType(reference, owner, metadata, currentSchema)
        val family = DatatypeFamilies.ofDescription(description, null, reference)
        if (family == null) {
            if (owner != null && inSpecification && namesSchemaObject(reference, owner, shadowed)) {
                return Evidence(ProjectDatatypeFamily.COMPOSITE, null, null, DatatypeFamilies.undeclaredTypeKind(reference.name.name.last))
            }
            return Evidence(null, if (DatatypeFamilies.isConclusive(description)) UnknownReason.UNCLASSIFIED_TYPE else UnknownReason.FORMAL_INCONCLUSIVE)
        }
        val builtIn = DatatypeFamilies.builtInName(description, null)
        val scope = builtIn?.let { DatatypeFamilies.builtInScope(description, owner?.name) }
        if (builtIn != null && scope != null && shadowed(scope, builtIn.name.segments.single())) {
            return Evidence(null, UnknownReason.FORMAL_INCONCLUSIVE)
        }
        val rank = builtIn?.let { OverloadRank.of(it.name, description.source.terminalReference.map { ref -> ref.writtenIdentity }.orElse(null)) }
        return Evidence(family, null, rank, DatatypeFamilies.compositeKind(description))
    }

    /**
     * Whether the formal type [reference] of a heading in the specification of the package [owner] names a schema
     * object (a type created with CREATE TYPE, or a synonym of one): an unquoted one-segment name that no member of the
     * package declares (complete coverage) and that names no predefined scalar type of STANDARD or DBMS_STANDARD.
     * SYS_REFCURSOR and the collection ORA_NAME_LIST_T are predefined but not scalar. Verified on Oracle: such a
     * formal (an object type, a nested table, a varray, a private synonym of a type, ANYDATA, JSON_ELEMENT_T,
     * SYS_REFCURSOR) rejects every scalar actual with PLS-00306, so it classifies as composite.
     */
    private fun namesSchemaObject(reference: ProjectTypeReference, owner: ProjectQualifiedName, shadowed: (QualifiedName, OracleIdentifier) -> Boolean): Boolean {
        if (reference.kind != ProjectTypeReference.Kind.NAMED || reference.writtenIdentity != null) return false
        val name = reference.name.name.segments.singleOrNull() ?: return false
        if (!DatatypeFamilies.isNonScalarTypeName(name)) return false
        return !shadowed(owner.name, name)
    }

    /**
     * One binding's outcome from its two sides: unknown when either side is, else the families' compatibility. Only
     * INCOMPATIBLE eliminates a candidate.
     */
    internal inline fun <R> compare(actual: Evidence, formal: Evidence, result: (Outcome, UnknownReason?) -> R): R {
        val reason = actual.reason ?: formal.reason
        if (reason != null) return result(Outcome.UNKNOWN, reason)
        // The interval family does not tell DAY TO SECOND from YEAR TO MONTH; their exact types do (Oracle: each
        // rejects the other with PLS-00306).
        if (actual.family == ProjectDatatypeFamily.INTERVAL && formal.family == ProjectDatatypeFamily.INTERVAL && actual.rank != null && formal.rank != null) {
            return result(if (actual.rank == formal.rank) Outcome.COMPATIBLE else Outcome.INCOMPATIBLE, null)
        }
        // Composites: a ref cursor and a schema object type reject each other; a SYS_REFCURSOR formal takes any ref cursor.
        if (actual.family == ProjectDatatypeFamily.COMPOSITE && formal.family == ProjectDatatypeFamily.COMPOSITE) {
            val kinds = setOfNotNull(actual.compositeKind, formal.compositeKind)
            if (actual.compositeKind != null && formal.compositeKind != null && kinds.size == 2) return result(Outcome.INCOMPATIBLE, null)
            if (kinds == setOf(CompositeKind.REF_CURSOR) && actual.compositeKind != null && formal.compositeKind != null) return result(Outcome.COMPATIBLE, null)
        }
        return when (DatatypeFamilies.compatibility(actual.family, formal.family)) {
            DatatypeCompatibility.COMPATIBLE -> result(Outcome.COMPATIBLE, null)
            DatatypeCompatibility.INCOMPATIBLE -> result(Outcome.INCOMPATIBLE, null)
            DatatypeCompatibility.ENVIRONMENT_DEPENDENT -> result(Outcome.UNKNOWN, UnknownReason.ENVIRONMENT_DEPENDENT)
            DatatypeCompatibility.UNKNOWN -> result(Outcome.UNKNOWN, UnknownReason.UNSUPPORTED_PAIR)
        }
    }

    /**
     * A package member named like a predefined type shadows it inside the package (verified on Oracle: a package
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
