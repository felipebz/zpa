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
        val declaredType: DeclaredCompositeType? = null,
        /** An object type, the project's or the metadata's: the side's own type. */
        val objectType: ObjectTypeRef? = null,
        /** The side is of the schema's type `json`, or of the predefined JSON type: the database version decides. */
        val predefinedJsonAlternative: Boolean = false
    ) {
        /** This side as the predefined JSON type, when that is its other reading. */
        fun predefinedJsonReading(): Evidence =
            if (predefinedJsonAlternative) Evidence(ProjectDatatypeFamily.JSON, null, DatatypeFamilies.rankOfTypeName("JSON")) else this
    }

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
        val hierarchy = ObjectTypeHierarchy(snapshot, metadata)
        val formalEvidence = ArrayList<List<Evidence?>>()
        val evaluations = structural.candidates
            .filter { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE }
            .map { candidate ->
                val declaration = candidate.declaration
                val owner = (declaration as? ProjectPackageSubprogramView)?.packageOwner
                val inSpecification = declaration.role == ProjectDeclarationRole.SPECIFICATION
                val formals = arrayOfNulls<Evidence>(actuals.size)
                val undeclared: ((OracleIdentifier) -> Boolean)? = if (owner != null && inSpecification) { name -> !shadowed(owner.name, name) } else null
                // A formal's type name is read from the schema of the unit that declares it.
                val schema = snapshot.occurrenceOf(declaration.id)?.let(snapshot.projectIndex::effectiveSchema)?.let(ProjectIdentifier::wrap) ?: currentSchema
                val bindings = candidate.bindings.map { binding ->
                    val actual = actuals[binding.argumentOrdinal - 1]
                    val formal = formal(snapshot, binding.parameter.typeRef, owner, undeclared, metadata, currentSchema, shadowed, hierarchy.names, schema)
                    formals[binding.argumentOrdinal - 1] = formal
                    compare(actual, formal, hierarchy) { outcome, reason ->
                        ProjectBindingCompatibility.of(binding, outcome, actual.family, formal.family, reason)
                    }
                }
                formalEvidence += formals.toList()
                ProjectCandidateCompatibility.of(candidate, bindings)
            }
        return ProjectTypedCallResolution.of(structural, evaluations, preferred(actuals, evaluations, formalEvidence, hierarchy))
    }

    /**
     * Among several retained candidates, the one Oracle binds, or null. A candidate with a binding that depends on
     * PLSQL_IMPLICIT_CONVERSION_BOOL ([bindable]) is out when another needs no such conversion; a single remaining
     * candidate is the call whatever its other bindings. Among several, each must be proven to receive the call (each
     * binding compatible, or depending only on that setting, which never makes its candidate the preferred one), and
     * [select] picks.
     */
    internal fun preferred(
        actuals: List<Evidence>,
        evaluations: List<ProjectCandidateCompatibility>,
        formals: List<List<Evidence?>>,
        hierarchy: ObjectTypeHierarchy
    ): ProjectCallCandidate? {
        val retained = evaluations.indices.filter { evaluations[it].outcome == ProjectCandidateCompatibility.Outcome.RETAINED }
        if (retained.size < 2) return null
        val remaining = bindable(retained) { index -> evaluations[index].bindings.any { it.unknownReasonOrNull == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (remaining.size == 1) return evaluations[remaining.single()].candidate
        val proven = remaining.all { index ->
            evaluations[index].bindings.all {
                it.outcome == Outcome.COMPATIBLE || it.unknownReasonOrNull == UnknownReason.ENVIRONMENT_DEPENDENT
            }
        }
        if (!proven) return null
        val selected = select(actuals, remaining.map { formals[it] }, hierarchy) ?: return null
        return evaluations[remaining[selected]].candidate
    }

    /**
     * [OverloadRank.select] over the [formals] of each candidate (by argument index), with an actual of an object type
     * the project declares ranking the formals of that type and its supertypes, nearest first ([hierarchy]): the same
     * type is an exact match, and does not keep the other actuals from choosing.
     */
    internal fun select(actuals: List<Evidence>, formals: List<List<Evidence?>>, hierarchy: ObjectTypeHierarchy?): Int? {
        val keys = formals.map { candidate ->
            actuals.indices.map { index ->
                val actual = actuals[index]
                val formal = candidate.getOrNull(index)
                when {
                    actual.rank != null -> formal?.rank
                    actual.objectType != null && hierarchy != null -> formal?.objectType
                    else -> null
                }
            }
        }
        return OverloadRank.selectBy(actuals.size, keys) { index, key ->
            val actual = actuals[index]
            when (key) {
                is OverloadRank -> actual.rank?.let { OverloadRank.tier(it, key) }
                is ObjectTypeRef -> actual.objectType?.let { hierarchy?.distance(it, key) }
                else -> null
            }
        }
    }

    /**
     * The [retained] candidates Oracle can bind under PLSQL_IMPLICIT_CONVERSION_BOOL's default (FALSE), which rejects a
     * BOOLEAN actual for a numeric or character formal, and a numeric or character actual for a BOOLEAN formal: those
     * with no binding that [needsConversion], when some have none; all of them otherwise.
     */
    internal fun <T> bindable(retained: List<T>, needsConversion: (T) -> Boolean): List<T> =
        retained.filterNot(needsConversion).ifEmpty { retained }

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
        val hierarchy = ObjectTypeHierarchy(snapshot, metadata)
        val evidence = formals.map { types -> types.map { formal(snapshot, it, null, { true }, metadata, currentSchema, shadowed, hierarchy.names, currentSchema) } }
        val outcomes = evidence.map { types -> actuals.indices.map { compare(actuals[it], types[it], hierarchy) { outcome, reason -> outcome to reason } } }
        val retained = outcomes.indices.filter { candidate -> outcomes[candidate].none { it.first == Outcome.INCOMPATIBLE } }
        if (retained.size == 1) return retained.single()
        if (retained.size < 2) return null
        val remaining = bindable(retained) { candidate -> outcomes[candidate].any { it.second == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (remaining.size == 1) return remaining.single()
        val proven = remaining.all { candidate -> outcomes[candidate].all { it.first == Outcome.COMPATIBLE || it.second == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (!proven) return null
        return select(actuals, remaining.map { evidence[it] }, hierarchy)?.let(remaining::get)
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
                family != null -> Evidence(family, null, type.rank, type.compositeKind, type.declaredType, type.objectType, type.predefinedJsonAlternative)
                type.unclassified -> Evidence(null, UnknownReason.UNCLASSIFIED_TYPE)
                else -> Evidence(null, UnknownReason.ACTUAL_UNKNOWN)
            }
        }
    }

    /**
     * [evidence] without its declared or object type when that type is a declaration of another snapshot: identities of
     * two snapshots are never equal, so such a type would reject every formal.
     */
    private fun issued(snapshot: ProjectSnapshot, evidence: Evidence): Evidence {
        val declared = (evidence.declaredType as? DeclaredCompositeType.Project)?.id
        val objectType = (evidence.objectType as? ObjectTypeRef.Project)?.id
        val foreign = listOfNotNull(declared, objectType).any { snapshot.occurrenceOf(it) == null }
        if (!foreign) return evidence
        return Evidence(evidence.family, evidence.reason, evidence.rank, evidence.compositeKind, predefinedJsonAlternative = evidence.predefinedJsonAlternative)
    }

    /**
     * The evidence of a formal of type [reference], declared in the package [owner] (null at schema level).
     * [undeclared], when given, tells whether a one-segment name is declared by no member of the declaring scope: such a
     * name that the project does not describe names a schema object; null when the scope's declarations are not covered.
     * A name the project does not describe that reaches an object type of the metadata from [declaringSchema] ([names])
     * is a value of that type.
     */
    private fun formal(
        snapshot: ProjectSnapshot,
        reference: ProjectTypeReference,
        owner: ProjectQualifiedName?,
        undeclared: ((OracleIdentifier) -> Boolean)?,
        metadata: ProjectExternalMetadata,
        currentSchema: ProjectIdentifier?,
        shadowed: (QualifiedName, OracleIdentifier) -> Boolean,
        names: ObjectTypeNames,
        declaringSchema: ProjectIdentifier?
    ): Evidence {
        if (DatatypeFamilies.isRowtypeReference(reference)) return Evidence(ProjectDatatypeFamily.COMPOSITE, null, null, CompositeKind.ROW)
        val description = snapshot.describeExternalType(reference, owner, metadata, currentSchema)
        val family = DatatypeFamilies.ofDescription(description, null, reference)
        if (family == null) {
            (externalObjectType(reference, owner, undeclared, names, declaringSchema) ?: names.throughSubtypes(description, shadowed))?.let {
                return Evidence(ProjectDatatypeFamily.COMPOSITE, null, null, CompositeKind.SCHEMA_OBJECT, objectType = it)
            }
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
        return Evidence(
            family, null, rank, DatatypeFamilies.compositeKind(description), DatatypeFamilies.declaredCompositeType(description),
            DatatypeFamilies.objectType(description)?.let(ObjectTypeRef::Project), description.source.predefinedJsonAlternative
        )
    }

    /**
     * The object type of the metadata a formal's written type [reference] reaches, a name of one or two segments that
     * the project does not describe: a one-segment name only when no member of the declaring package [owner] may take
     * it ([undeclared]), and never a predefined scalar name.
     */
    private fun externalObjectType(
        reference: ProjectTypeReference,
        owner: ProjectQualifiedName?,
        undeclared: ((OracleIdentifier) -> Boolean)?,
        names: ObjectTypeNames,
        declaringSchema: ProjectIdentifier?
    ): ObjectTypeRef? {
        // A qualified name keeps its spelling as its written identity: only a one-segment name is checked against the predefined ones.
        if (reference.kind != ProjectTypeReference.Kind.NAMED) return null
        val segments = reference.name.name.segments
        if (segments.size == 1 && (owner != null && undeclared == null || !namesSchemaObject(reference, undeclared ?: { true }))) return null
        return names.reach(segments, declaringSchema)
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
     * One binding's outcome from its two sides, as [oneReading] gives it. When a side's `json` is the schema's type or
     * the predefined JSON type depending on the database version, both readings are compared, every such side read
     * the same way: the outcome they agree on, else unknown.
     */
    internal inline fun <R> compare(actual: Evidence, formal: Evidence, hierarchy: ObjectTypeHierarchy?, result: (Outcome, UnknownReason?) -> R): R {
        if (!actual.predefinedJsonAlternative && !formal.predefinedJsonAlternative) return oneReading(actual, formal, hierarchy, result)
        val (outcome, reason) = eitherJsonReading(actual, formal, hierarchy)
        return result(outcome, reason)
    }

    internal fun eitherJsonReading(actual: Evidence, formal: Evidence, hierarchy: ObjectTypeHierarchy?): Pair<Outcome, UnknownReason?> {
        val schema = oneReading(actual, formal, hierarchy) { outcome, reason -> outcome to reason }
        val predefined = oneReading(actual.predefinedJsonReading(), formal.predefinedJsonReading(), hierarchy) { outcome, reason -> outcome to reason }
        return if (schema == predefined) schema else Outcome.UNKNOWN to UnknownReason.UNSUPPORTED_PAIR
    }

    /**
     * One binding's outcome from its two sides: unknown when either side is, else the families' compatibility. Only
     * INCOMPATIBLE eliminates a candidate. Two object types the project declares are compared through [hierarchy].
     */
    internal inline fun <R> oneReading(actual: Evidence, formal: Evidence, hierarchy: ObjectTypeHierarchy?, result: (Outcome, UnknownReason?) -> R): R {
        val reason = actual.reason ?: formal.reason
        if (reason != null) return result(Outcome.UNKNOWN, reason)
        // The interval family does not tell DAY TO SECOND from YEAR TO MONTH; their exact types do: each rejects the
        // other with PLS-00306.
        if (actual.family == ProjectDatatypeFamily.INTERVAL && formal.family == ProjectDatatypeFamily.INTERVAL && actual.rank != null && formal.rank != null) {
            return result(if (actual.rank == formal.rank) Outcome.COMPATIBLE else Outcome.INCOMPATIBLE, null)
        }
        if (actual.family == ProjectDatatypeFamily.COMPOSITE && formal.family == ProjectDatatypeFamily.COMPOSITE) {
            compositeOutcome(actual, formal, hierarchy)?.let { return result(it, null) }
        }
        return when (DatatypeFamilies.compatibility(actual.family, formal.family)) {
            DatatypeCompatibility.COMPATIBLE -> result(Outcome.COMPATIBLE, null)
            DatatypeCompatibility.INCOMPATIBLE -> result(Outcome.INCOMPATIBLE, null)
            DatatypeCompatibility.ENVIRONMENT_DEPENDENT -> result(Outcome.UNKNOWN, UnknownReason.ENVIRONMENT_DEPENDENT)
            DatatypeCompatibility.UNKNOWN -> result(Outcome.UNKNOWN, UnknownReason.UNSUPPORTED_PAIR)
        }
    }

    /**
     * Two composites, when what they are decides it. Two composite types are compatible only when they are the same
     * type (PLS-00306 otherwise, even with the same fields or element):
     * - a value of a schema type (an object type, a collection or an opaque type created with CREATE TYPE, reached
     *   through synonyms and subtypes) goes to a formal of that type or, for an object type, of a supertype
     *   ([ObjectTypeHierarchy]);
     * - a formal of a record or collection type declared in a package or a block accepts only a value of that same
     *   declared type;
     * - a declared record or collection, and a `%ROWTYPE` row, are never a schema type zpa resolves nor a ref cursor;
     * - a ref cursor and a schema type reject each other, and a SYS_REFCURSOR formal takes any ref cursor.
     *
     * A `%ROWTYPE` against a declared record, or against another row, is compared field by field and not decided here.
     * Null when none of these applies, or a side's type is not known (an unresolved name, a dictionary package type).
     */
    internal fun compositeOutcome(actual: Evidence, formal: Evidence, hierarchy: ObjectTypeHierarchy?): Outcome? {
        if (actual.objectType != null && formal.objectType != null && hierarchy != null) {
            hierarchy.outcome(actual.objectType, formal.objectType)?.let { return it }
        }
        if (actual.declaredType != null && formal.declaredType != null) {
            return if (actual.declaredType == formal.declaredType) Outcome.COMPATIBLE else Outcome.INCOMPATIBLE
        }
        if (isRecordLike(actual) && isSchemaTypeOrCursor(formal) || isRecordLike(formal) && isSchemaTypeOrCursor(actual)) return Outcome.INCOMPATIBLE
        val actualKind = actual.compositeKind ?: return null
        val formalKind = formal.compositeKind ?: return null
        return when {
            actualKind == CompositeKind.REF_CURSOR && formalKind == CompositeKind.REF_CURSOR -> Outcome.COMPATIBLE
            setOf(actualKind, formalKind) == REF_CURSOR_AND_SCHEMA_TYPE -> Outcome.INCOMPATIBLE
            else -> null
        }
    }

    /** A record or collection declared in a package or a block, or a `%ROWTYPE` row. */
    private fun isRecordLike(evidence: Evidence) = evidence.declaredType != null || evidence.compositeKind == CompositeKind.ROW

    /** A schema type zpa resolves, or a ref cursor. */
    private fun isSchemaTypeOrCursor(evidence: Evidence) = evidence.objectType != null || evidence.compositeKind == CompositeKind.REF_CURSOR

    private val REF_CURSOR_AND_SCHEMA_TYPE = setOf(CompositeKind.REF_CURSOR, CompositeKind.SCHEMA_OBJECT)

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
