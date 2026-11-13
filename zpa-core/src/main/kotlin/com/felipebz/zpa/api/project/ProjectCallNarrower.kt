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
        val compositeKind: CompositeKind? = null,
        val declaredType: DeclaredCompositeType? = null
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
        val actuals = types.map { issued(snapshot, actual(it, shadowed)) }
        val formalRanks = ArrayList<List<OverloadRank?>>()
        val evaluations = structural.candidates
            .filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
            .map { candidate ->
                val declaration = candidate.declaration
                val owner = (declaration as? ProjectPackageSubprogramView)?.packageOwner
                val inSpecification = declaration.role == ProjectDeclarationRole.SPECIFICATION
                val ranks = arrayOfNulls<OverloadRank>(actuals.size)
                val undeclared: ((OracleIdentifier) -> Boolean)? = if (owner != null && inSpecification) { name -> !shadowed(owner.name, name) } else null
                val bindings = candidate.bindings.map { binding ->
                    val actual = actuals[binding.argumentOrdinal - 1]
                    val formal = formal(snapshot, binding.parameter.typeRef, owner, undeclared, metadata, currentSchema, shadowed)
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
     * (with the setting on or off, such a binding never makes its candidate the preferred one).
     */
    internal fun preferred(actuals: List<Evidence>, evaluations: List<ProjectCandidateCompatibility>, formalRanks: List<List<OverloadRank?>>): ProjectCallCandidate? {
        val retained = evaluations.indices.filter { evaluations[it].outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
        if (retained.size < 2) return null
        val proven = retained.all { index ->
            evaluations[index].bindings.all {
                it.outcome == Outcome.COMPATIBLE || it.unknownReasonOrNull == UnknownReason.ENVIRONMENT_DEPENDENT
            }
        }
        if (!proven) return null
        val selected = OverloadRank.select(actuals.map { it.rank }, retained.map { formalRanks[it] }) ?: return null
        return evaluations[retained[selected]].candidate
    }

    /**
     * Among the [formals] of candidates that all fit a call structurally (for each, the formal type of each actual, in
     * written order), the index of the one Oracle binds by the actuals' [argumentTypes], compared and preferred as in
     * [narrow]: the only candidate no actual is proven incompatible with, or the one [preferred] picks. Null otherwise.
     * The formals are those of an object type's methods, named at schema level: an undeclared one-segment name that is
     * not a predefined scalar type names a schema object, and no package shadows a predefined name there.
     */
    internal fun selectByDatatypes(
        snapshot: ProjectSnapshot,
        argumentTypes: List<ProjectArgumentType>,
        formals: List<List<ProjectTypeReference>>,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?
    ): Int? {
        val shadowed = shadowCheck(snapshot)
        val actuals = argumentTypes.map { issued(snapshot, actual(it, shadowed)) }
        val evidence = formals.map { types -> types.map { formal(snapshot, it, null, { true }, metadata, currentSchema, shadowed) } }
        val outcomes = evidence.map { types -> actuals.indices.map { compare(actuals[it], types[it]) { outcome, reason -> outcome to reason } } }
        val retained = outcomes.indices.filter { candidate -> outcomes[candidate].none { it.first == Outcome.INCOMPATIBLE } }
        if (retained.size == 1) return retained.single()
        if (retained.size < 2) return null
        val proven = retained.all { candidate -> outcomes[candidate].all { it.first == Outcome.COMPATIBLE || it.second == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (!proven) return null
        return OverloadRank.select(actuals.map { it.rank }, retained.map { candidate -> evidence[candidate].map { it.rank } })?.let(retained::get)
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
            val family = type.familyOrNull
            val scope = type.shadowScope
            val name = type.shadowName
            when {
                family != null && scope != null && name != null && shadowed(scope, name) -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
                family != null -> Evidence(family, null, type.rank, type.compositeKind, type.declaredType)
                type.unclassified -> Evidence(null, UnknownReason.UNCLASSIFIED_TYPE)
                else -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
            }
        }
    }

    /**
     * [evidence] without its declared type when that type is a declaration of another snapshot: identities of two
     * snapshots are never equal, so such a type would reject every formal.
     */
    private fun issued(snapshot: ProjectSnapshot, evidence: Evidence): Evidence {
        val type = evidence.declaredType as? DeclaredCompositeType.Project ?: return evidence
        if (snapshot.occurrenceOf(type.id) != null) return evidence
        return Evidence(evidence.family, evidence.reason, evidence.rank, evidence.compositeKind)
    }

    /**
     * The evidence of a formal of type [reference], declared in the package [owner] (null at schema level).
     * [undeclared], when given, tells whether a one-segment name is declared by no member of the declaring scope: such a
     * name that the project does not describe names a schema object; null when the scope's declarations are not covered.
     */
    private fun formal(
        snapshot: ProjectSnapshot,
        reference: ProjectTypeReference,
        owner: ProjectQualifiedName?,
        undeclared: ((OracleIdentifier) -> Boolean)?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?,
        shadowed: (QualifiedName, OracleIdentifier) -> Boolean
    ): Evidence {
        if (DatatypeFamilies.isRowtypeReference(reference)) return Evidence(ProjectDatatypeFamily.COMPOSITE, null)
        val description = snapshot.describeExternalType(reference, owner, metadata, currentSchema)
        val family = DatatypeFamilies.ofDescription(description, null, reference)
        if (family == null) {
            if (undeclared != null && namesSchemaObject(reference, undeclared)) {
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
            ?: DatatypeFamilies.externalRank(description)
        return Evidence(family, null, rank, DatatypeFamilies.compositeKind(description), DatatypeFamilies.declaredCompositeType(description))
    }

    /**
     * Whether the formal type [reference] names a schema object (a type created with CREATE TYPE, or a synonym of one):
     * an unquoted one-segment name that the declaring scope does not declare ([undeclared]) and that names no predefined
     * scalar type of STANDARD or DBMS_STANDARD.
     * SYS_REFCURSOR and the collection ORA_NAME_LIST_T are predefined but not scalar. Such a formal (an object type, a nested table, a varray, a private synonym of a type, ANYDATA, JSON_ELEMENT_T,
     * SYS_REFCURSOR) rejects every scalar actual with PLS-00306, so it classifies as composite.
     */
    private fun namesSchemaObject(reference: ProjectTypeReference, undeclared: (OracleIdentifier) -> Boolean): Boolean {
        if (reference.kind != ProjectTypeReference.Kind.NAMED || reference.writtenIdentity != null) return false
        val name = reference.name.name.segments.singleOrNull() ?: return false
        if (!DatatypeFamilies.isNonScalarTypeName(name)) return false
        return undeclared(name)
    }

    /**
     * One binding's outcome from its two sides: unknown when either side is, else the families' compatibility. Only
     * INCOMPATIBLE eliminates a candidate.
     */
    internal inline fun <R> compare(actual: Evidence, formal: Evidence, result: (Outcome, UnknownReason?) -> R): R {
        val reason = actual.reason ?: formal.reason
        if (reason != null) return result(Outcome.UNKNOWN, reason)
        // The interval family does not tell DAY TO SECOND from YEAR TO MONTH; their exact types do: each rejects the
        // other with PLS-00306.
        if (actual.family == ProjectDatatypeFamily.INTERVAL && formal.family == ProjectDatatypeFamily.INTERVAL && actual.rank != null && formal.rank != null) {
            return result(if (actual.rank == formal.rank) Outcome.COMPATIBLE else Outcome.INCOMPATIBLE, null)
        }
        // Composites: a formal of a declared record or collection type accepts only a value of that same declared type
        // (another one with the same fields or element is rejected); a ref cursor and a schema object type reject each
        // other; a SYS_REFCURSOR formal takes any ref cursor.
        if (actual.family == ProjectDatatypeFamily.COMPOSITE && formal.family == ProjectDatatypeFamily.COMPOSITE) {
            if (actual.declaredType != null && formal.declaredType != null) {
                return result(if (actual.declaredType == formal.declaredType) Outcome.COMPATIBLE else Outcome.INCOMPATIBLE, null)
            }
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
     * A package member named like a predefined type shadows it inside the package (a package
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
