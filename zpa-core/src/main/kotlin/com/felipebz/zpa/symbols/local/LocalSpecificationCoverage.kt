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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.project.*
import java.util.*

/** A declaration of a package specification in another project file: its kind and where it starts. Plain data. */
internal class LocalSpecificationMember(val kind: LocalDeclarationKind, val line: Int, val column: Int, val fileId: String)

/**
 * Decides lookups that stopped only at a package body's missing specification, with the project's member-name
 * coverage of that specification when it is in another project file. A step over the resolver's outcomes: the
 * resolver itself never sees the project.
 *
 * A lookup qualifies when its only gap is the missing specification, no region it relied on is incomplete, the body is
 * a package body, and the project holds exactly one specification occurrence for the package name written in the
 * body's CREATE. With the coverage's status for the name (every specification member precedes a body reference):
 *
 * | Status | The body declares the name | Outcome |
 * |---|---|---|
 * | NOT_DECLARED | no | absent |
 * | NOT_DECLARED | yes | the body's own decision |
 * | DECLARED_BEFORE | no | [LocalResolution.SpecificationMember] |
 * | DECLARED_BEFORE | yes | the decision with the specification in the file, when the project proves each heading's definition |
 * | REJECTED or UNKNOWN | either | the gap stays |
 *
 * A name both declare is decided only when every specification member of the name is a subprogram heading the
 * project fully knows (no omitted heading, no conditional compilation, no parse gap) and each heading has exactly one
 * body definition with the same heading. Like the specification itself, those definitions are visible throughout the
 * body; the body's other declarations keep the order rule. An overload set only partly declared before the reference
 * stops the resolver without a gap, and is decided the same way.
 *
 * Every decision is wrapped in [LocalResolution.SpecificationCovered], so a consumer can tell it from one the file
 * proved alone. Absence is still the only outcome after which a caller looks past the local scope: an absent name is
 * one neither the body nor the specification declares.
 *
 * An unqualified name in SQL also carries the column gap, and is decided as with the specification in the file, wrapped:
 * when the specification does not declare it, the column gap over an absent name, or [LocalResolution.SqlColumnOrLocal]
 * for a declaration of the body; when only the specification declares it, once, the column gap over
 * [LocalResolution.SpecificationMember], described in [sqlSpecificationMembers]. Otherwise its gaps stay.
 *
 * For a qualified name, the first segment is decided this way; when it is the package's own name, the second segment
 * (a member of the body and specification) too, where "absent" is the member lookup's own `Undeclared`. Later
 * segments inherit the first segments' gap only, and carry the covered decision.
 */
internal class LocalSpecificationCoverage private constructor(
    private val model: LocalSemanticModel,
    private val index: ProjectSymbolIndex
) {
    private val specifications = HashMap<LocalRegionId, Optional<IndexedDeclaration>>()

    fun refine(reference: LocalReference, steps: List<LocalResolution>): List<LocalResolution> {
        (steps.first() as? LocalResolution.Unsupported)?.let { return partlyLater(reference, it)?.let(::listOf) ?: steps }
        val first = steps.first() as? LocalResolution.Incomplete ?: return steps
        typeBody(reference, first)?.let { return listOf(LocalResolution.SpecificationCovered(it)) }
        sqlName(reference, first)?.let { return listOf(LocalResolution.SpecificationCovered(it)) }
        if (!onlySpecificationGap(first)) return steps
        val (body, specification) = bodyWithSpecification(reference) ?: return steps
        val decided = decide(specification, body, reference, first.provisional, LocalResolution.ProvenAbsent, member = false) ?: return steps
        // Absent or a specification member: nothing after it is read locally.
        val final = decided == LocalResolution.ProvenAbsent || decided == LocalResolution.SpecificationMember
        // A body decision other than the provisional one: the later segments were read after the provisional outcome.
        if (!final && decided != first.provisional && hasMembers(reference)) return steps
        // An unmodeled construct is reported as such, as the resolver reports it with the specification in the file.
        if (decided is LocalResolution.Unsupported) return listOf(decided)
        if (final || steps.size == 1) return listOf(LocalResolution.SpecificationCovered(decided))
        return withLaterSteps(decided, body, specification, reference, steps)
    }

    /** The decided first segment [decided] followed by the later steps, a member of the package's own name decided too. */
    private fun withLaterSteps(
        decided: LocalResolution,
        body: LocalRegion,
        specification: IndexedDeclaration,
        reference: LocalReference,
        steps: List<LocalResolution>
    ): List<LocalResolution> {
        val result = arrayListOf<LocalResolution>(LocalResolution.SpecificationCovered(decided))
        var rest = steps.drop(1)
        // The package's own name as qualifier: the next segment is a member of the body and its specification.
        if (isOwnUnit(decided, body)) {
            val decidedMember = ownUnitMember(specification, body, reference, rest) ?: return result + rest
            if (decidedMember is LocalResolution.Unsupported) return result + decidedMember
            result += LocalResolution.SpecificationCovered(decidedMember)
            if (decidedMember == LocalResolution.SpecificationMember) return result
            rest = rest.drop(1)
        }
        for (step in rest) {
            result += if (step is LocalResolution.Incomplete && onlySpecificationGap(step)) LocalResolution.SpecificationCovered(step.provisional) else step
        }
        return result
    }

    /**
     * The member after the package's own name, whose resolver outcome is the first of [rest]; null keeps it. A member
     * decided differently from its provisional outcome is kept only when nothing was read after it.
     */
    private fun ownUnitMember(specification: IndexedDeclaration, body: LocalRegion, reference: LocalReference, rest: List<LocalResolution>): LocalResolution? {
        val memberReference = LocalQualifiedResolver.member(reference, 0)
        val member = rest.first()
        return when {
            member is LocalResolution.Incomplete && onlySpecificationGap(member) ->
                decide(specification, body, memberReference, member.provisional, LocalResolution.Undeclared, member = true)
                    ?.takeIf { it == member.provisional || rest.size == 1 }
            isPartlyLater(member) && rest.size == 1 -> sharedName(specification, body, memberReference, member = true)
            else -> null
        }
    }

    /**
     * A one-segment name that stopped at an overload set only partly declared before it, in a package body whose
     * specification is in another file: the resolver reached the body (no inner region declares the name) through
     * complete regions, so the only unknown is which of the body's subprograms the specification makes visible.
     */
    private fun partlyLater(reference: LocalReference, outcome: LocalResolution.Unsupported): LocalResolution? {
        if (!isPartlyLater(outcome) || hasMembers(reference)) return null
        val chain = LocalResolver.chain(model, reference.region)
        val body = packageBody(chain) ?: return null
        val bodyIndex = chain.indexOf(body)
        if (LocalTypeScope.unitOf(model, body.id)?.counterpart != null) return null
        val inner = chain.subList(0, bodyIndex)
        if (inner.any { region -> LocalResolver.scopeOf(model, region).named(reference.name).any { LocalTypeScope.capturesIn(it, reference.context) } }) return null
        if (chain.subList(0, bodyIndex + 1).any { it.completeness.reasons.isNotEmpty() }) return null
        val specification = specification(body) ?: return null
        if (PackageMemberNames.at(index, specification, reference.name, null).status != MemberNameStatus.DECLARED_BEFORE) return null
        return sharedName(specification, body, reference, member = false)?.takeIf { it !is LocalResolution.Unsupported }
            ?.let(LocalResolution::SpecificationCovered)
    }

    /** The package body enclosing [reference] with the one specification the project holds for it. */
    private fun bodyWithSpecification(reference: LocalReference): Pair<LocalRegion, IndexedDeclaration>? {
        val body = packageBody(LocalResolver.chain(model, reference.region)) ?: return null
        return specification(body)?.let { body to it }
    }

    /** The package body enclosing [chain], outside any object type. */
    private fun packageBody(chain: List<LocalRegion>): LocalRegion? =
        if (chain.any { it.kind == LocalRegionKind.TYPE_BODY || it.kind == LocalRegionKind.OBJECT_TYPE }) null
        else chain.firstOrNull { it.kind == LocalRegionKind.PACKAGE_BODY }

    private fun isPartlyLater(outcome: LocalResolution) =
        (outcome as? LocalResolution.Unsupported)?.construct == LocalUnsupportedConstruct.OVERLOADS_PARTLY_LATER

    private fun hasMembers(reference: LocalReference) = reference.segments.firstOrNull()?.isAttribute == false

    /** Names decided in an object type body or specification by the project: `name:line:column` where it disagrees. */
    val mismatches = ArrayList<String>()

    /**
     * A first segment in an object type body or specification that stopped only because the specification or a
     * supertype is not in the file, decided from the project's attribute and method lists of the type and its `UNDER`
     * chain:
     * - neither an attribute nor a method (constructors aside, which capture nothing): absent;
     * - an attribute or a method: a specification member. In a type position it captures the name (PLS-00488).
     *
     * Null keeps the gap: a list of the chain not fully known, an attribute or member method named alone in a static
     * method (rejected, which the local rules decide), or a disagreement with the specification in this file.
     */
    private fun typeBody(reference: LocalReference, outcome: LocalResolution.Incomplete): LocalResolution? {
        if (outcome.provisional != LocalResolution.ProvenAbsent || outcome.regionReasons.isNotEmpty() ||
            !TYPE_GAPS.containsAll(outcome.lookupReasons)) return null
        val chain = LocalResolver.chain(model, reference.region)
        val typeRegion = chain.firstOrNull { it.kind == LocalRegionKind.TYPE_BODY || it.kind == LocalRegionKind.OBJECT_TYPE } ?: return null
        val unitName = LocalTypeScope.unitOf(model, typeRegion.id)?.unitName ?: return null
        val types = projectChain(unitName) ?: return null
        val name = reference.name
        fun StandaloneTypeDeclaration.attribute() = attributes.any { it.name == name }
        fun StandaloneTypeDeclaration.methods() = methods.filter { it.name == name && it.methodKind != ProjectMethodKind.CONSTRUCTOR }
        if (types.none { it.attribute() || it.methods().isNotEmpty() }) return LocalResolution.ProvenAbsent
        // The specification is in this file and the local side found nothing of the name in it.
        if (LocalLookupGap.SPECIFICATION_NOT_IN_FILE !in outcome.lookupReasons && (types.first().attribute() || types.first().methods().isNotEmpty())) {
            mismatches += "$name:${reference.range.startLine}:${reference.range.startColumn}"
            return null
        }
        val instanceMember = types.any { it.attribute() || it.methods().any { m -> m.methodKind != ProjectMethodKind.STATIC } }
        if (instanceMember && reference.context != LocalReferenceContext.TYPE && LocalTypeScope.method(model, chain)?.isStatic == true) return null
        return LocalResolution.SpecificationMember
    }

    /** The project's object type [typeName] and its supertypes, nearest first, when every list is complete and unaltered. */
    private fun projectChain(typeName: QualifiedName): List<StandaloneTypeDeclaration>? {
        val types = ArrayList<StandaloneTypeDeclaration>()
        var name: QualifiedName? = typeName
        while (name != null) {
            val type = index.findOccurrences(name).map { it.declaration }.singleOrNull() as? StandaloneTypeDeclaration ?: return null
            if (type in types || type.shape != ProjectTypeShape.OBJECT || !type.attributesComplete || !type.methodsComplete) return null
            if (index.alteredTypes.attributesOf(type.name) || index.alteredTypes.methodsOf(type.name)) return null
            types += type
            name = type.supertype?.name
        }
        return types
    }

    /** The specification member that an unqualified SQL name may bind, by the name's position, when it is the only reading besides a column. */
    val sqlSpecificationMembers = HashMap<Pair<Int, Int>, LocalSpecificationMember>()

    /** An unqualified SQL name decided as with the specification in the file; null keeps the gaps. */
    private fun sqlName(reference: LocalReference, outcome: LocalResolution.Incomplete): LocalResolution? {
        if (reference.segments.isNotEmpty() || outcome.regionReasons.isNotEmpty()) return null
        val provisional = outcome.provisional
        // With a declaration of the body found, the column gap is already in the provisional outcome.
        val sql = outcome.lookupReasons == SQL_GAPS ||
            outcome.lookupReasons == setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE) && provisional is LocalResolution.SqlColumnOrLocal
        if (!sql) return null
        val body = packageBody(LocalResolver.chain(model, reference.region)) ?: return null
        val members = specificationMembers(body, reference.name) ?: return null
        return when {
            members.isEmpty() && provisional == LocalResolution.ProvenAbsent -> columnOr(LocalResolution.ProvenAbsent)
            members.isEmpty() && provisional is LocalResolution.SqlColumnOrLocal ->
                provisional.takeIf { declaredByBody(LocalResolution.Bound(it.declaration), body) }
            provisional == LocalResolution.ProvenAbsent -> specificationMember(reference, members.singleOrNull() ?: return null)
            else -> null
        }
    }

    private fun specificationMember(reference: LocalReference, member: ProjectDeclaration): LocalResolution? {
        val kind = when (member) {
            is PackageValueDeclaration -> LocalDeclarationKind.VARIABLE
            is PackageExceptionDeclaration -> LocalDeclarationKind.EXCEPTION
            is PackageCursorDeclaration -> LocalDeclarationKind.CURSOR
            is PackageFunctionDeclaration -> LocalDeclarationKind.FUNCTION
            is PackageProcedureDeclaration -> LocalDeclarationKind.PROCEDURE
            is PackageTypeDeclaration, is PackageSubtypeDeclaration -> LocalDeclarationKind.SUBTYPE
            else -> return null
        }
        sqlSpecificationMembers[reference.range.startLine to reference.range.startColumn] =
            LocalSpecificationMember(kind, member.sourceRange.startLine, member.sourceRange.startColumn, member.fileId.value)
        return columnOr(LocalResolution.SpecificationMember)
    }

    private fun columnOr(outcome: LocalResolution) =
        LocalResolution.Incomplete(emptySet(), EnumSet.of(LocalLookupGap.SQL_COLUMN_UNKNOWN), outcome)

    private fun onlySpecificationGap(outcome: LocalResolution.Incomplete) =
        outcome.regionReasons.isEmpty() && outcome.lookupReasons == setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE)

    /**
     * The decision for [reference]'s name, whose resolver outcome without the gap is [provisional]; null keeps the gap.
     * A [member] follows the package's own name.
     */
    private fun decide(
        specification: IndexedDeclaration,
        body: LocalRegion,
        reference: LocalReference,
        provisional: LocalResolution,
        absent: LocalResolution,
        member: Boolean
    ): LocalResolution? = when (PackageMemberNames.at(index, specification, reference.name, null).status) {
        MemberNameStatus.NOT_DECLARED -> provisional.takeIf { it == absent || declaredByBody(it, body) }
        MemberNameStatus.DECLARED_BEFORE ->
            if (provisional == absent) LocalResolution.SpecificationMember else sharedName(specification, body, reference, member)
        MemberNameStatus.REJECTED, MemberNameStatus.UNKNOWN -> null
    }

    /** A name the body and its specification both declare, decided as with the specification in the file; null keeps the gap. */
    private fun sharedName(specification: IndexedDeclaration, body: LocalRegion, reference: LocalReference, member: Boolean): LocalResolution? {
        val definitions = definitions(specification, body, reference.name) ?: return null
        val decided = LocalResolver.decideInBody(model, reference, body, definitions, member) ?: return null
        return if (member) LocalQualifiedResolver.memberOutcome(model, decided) else decided
    }

    /**
     * The body's definition of each specification heading of [name], in heading order. Null unless every member of the
     * name is a retained, unconditional subprogram heading of a specification whose members are all known, and each
     * heading has exactly one definition in the body with the same heading (Oracle's PLS-00323 otherwise).
     */
    private fun definitions(specification: IndexedDeclaration, body: LocalRegion, name: OracleIdentifier): List<LocalDeclarationId>? =
        matches(specification, body, name)?.map { it.second }

    private val defaults = HashMap<LocalDeclarationId, Optional<List<Boolean>>>()

    /**
     * For a body subprogram [definition] that implements a heading of its specification in another file, whether each
     * parameter has a default in that heading: inside the body, Oracle applies the specification's defaults.
     */
    fun specificationDefaults(definition: LocalDeclarationId): List<Boolean>? = defaults.getOrPut(definition) {
        val declaration = model.declaration(definition)
        val body = model.region(declaration.region)
        val specification = if (body.kind == LocalRegionKind.PACKAGE_BODY) specification(body) else null
        val heading = specification?.let { matches(it, body, declaration.name) }?.firstOrNull { it.second == definition }?.first
        Optional.ofNullable(heading?.parameters?.map { it.defaultPresent })
    }.orElse(null)

    /** Each specification heading of [name] with its body definition, in heading order; see [definitions]. */
    private fun matches(
        specification: IndexedDeclaration,
        body: LocalRegion,
        name: OracleIdentifier
    ): List<Pair<PackageSubprogramDeclaration, LocalDeclarationId>>? {
        val spec = specification.declaration as PackageDeclaration
        val names = spec.memberNames ?: return null
        val coverage = spec.callableCoverage
        if (!names.complete || coverage.unknown || name.lookupName in coverage.omittedNames || names.unretained.any { it.name == name }) return null
        val headings = PackageMemberNames.members(index, specification, name)
            .sortedWith(compareBy({ it.sourceRange.startLine }, { it.sourceRange.startColumn }))
        if (headings.isEmpty() || headings.any { it !is PackageSubprogramDeclaration || !names.isUnconditional(it.sourceRange) }) return null
        val subprograms = headings.map { it as PackageSubprogramDeclaration }
        val keys = subprograms.map(LocalDeclarationLinker::signatureKey)
        if (keys.toSet().size != keys.size) return null
        val implementations = model.index.named(body.id, name)
            .filter { (it.details as? LocalSubprogram)?.form == LocalSubprogramForm.BODY }
            .groupBy { LocalDeclarationLinker.signatureKey(it.kind, it.details as LocalSubprogram) }
        return subprograms.zip(keys) { heading, key -> heading to (implementations[key]?.singleOrNull()?.id ?: return null) }
    }

    /** A decision made by declarations of the body region, or by the body's own unit name. */
    private fun declaredByBody(outcome: LocalResolution, body: LocalRegion): Boolean {
        val ids = when (outcome) {
            is LocalResolution.Bound -> listOf(outcome.declaration)
            is LocalResolution.OverloadSet -> outcome.declarations
            is LocalResolution.WrongKind -> listOf(outcome.declaration)
            is LocalResolution.DeclaredLater -> listOf(outcome.declaration)
            is LocalResolution.Duplicate -> outcome.declarations
            else -> return false
        }
        return ids.all { id -> model.declaration(id).let { it.region == body.id || isOwnUnit(LocalResolution.Bound(id), body) } }
    }

    private fun isOwnUnit(outcome: LocalResolution, body: LocalRegion): Boolean =
        ((outcome as? LocalResolution.Bound)?.let { model.declaration(it.declaration).details } as? LocalUnit)?.region == body.id

    /**
     * For each variable or exception declared inside (not directly in) a package body whose specification is in another
     * file: the specification's variable or exception of the same name, [LocalSpecificationValue.Unknown] when the
     * project does not prove it, nothing when the specification declares no such value. Plain data, AST- and index-free.
     */
    fun specificationValues(): Map<Pair<LocalRegionId, String>, LocalSpecificationValue> {
        val result = HashMap<Pair<LocalRegionId, String>, LocalSpecificationValue>()
        for (declaration in model.declarations) {
            if (declaration.kind != LocalDeclarationKind.VARIABLE && declaration.kind != LocalDeclarationKind.EXCEPTION) continue
            for (body in bodiesWithoutSpecification(declaration.region)) {
                result.getOrPut(body.id to declaration.name.lookupName) { specificationValue(body, declaration.name) }
            }
        }
        return result
    }

    /** The package bodies strictly enclosing [region] whose specification is not in the file. */
    private fun bodiesWithoutSpecification(region: LocalRegionId): List<LocalRegion> {
        val bodies = ArrayList<LocalRegion>()
        var current = model.parent(region)
        while (current != null) {
            if (current.kind == LocalRegionKind.PACKAGE_BODY && LocalTypeScope.unitOf(model, current.id)?.counterpart == null) bodies += current
            current = model.parent(current.id)
        }
        return bodies
    }

    private fun specificationValue(body: LocalRegion, name: OracleIdentifier): LocalSpecificationValue {
        val members = specificationMembers(body, name) ?: return LocalSpecificationValue.Unknown
        val value = members.firstOrNull { it is PackageValueDeclaration || it is PackageExceptionDeclaration } ?: return LocalSpecificationValue.None
        return LocalSpecificationValue.Declared(value.sourceRange.startLine, value.sourceRange.startColumn, value.fileId.value)
    }

    /**
     * The declarations named [name] in the specification, in another file, of the package [body], when the project
     * proves the list: empty when the specification does not declare the name. Null when it is not proven (no single
     * specification, an unretained member of the name, conditional members, or incomplete coverage).
     */
    private fun specificationMembers(body: LocalRegion, name: OracleIdentifier): List<ProjectDeclaration>? {
        val specification = specification(body) ?: return null
        val spec = specification.declaration as PackageDeclaration
        if (spec.memberNames?.unretained.orEmpty().any { it.name == name }) return null
        return when (PackageMemberNames.at(index, specification, name, null).status) {
            MemberNameStatus.NOT_DECLARED -> emptyList()
            MemberNameStatus.DECLARED_BEFORE -> PackageMemberNames.members(index, specification, name)
            MemberNameStatus.REJECTED, MemberNameStatus.UNKNOWN -> null
        }
    }

    /** The one specification occurrence the project holds for the body's package, or null. */
    private fun specification(body: LocalRegion): IndexedDeclaration? = specifications.getOrPut(body.id) {
        val name = LocalTypeScope.unitOf(model, body.id)?.unitName
        Optional.ofNullable(name?.let { index.findOccurrences(it) }?.singleOrNull {
            val declaration = it.declaration
            declaration is PackageDeclaration && declaration.role == DeclarationRole.SPECIFICATION
        })
    }.orElse(null)

    companion object {
        private val TYPE_GAPS = EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SUPERTYPE_NOT_IN_FILE)
        private val SQL_GAPS = EnumSet.of(LocalLookupGap.SPECIFICATION_NOT_IN_FILE, LocalLookupGap.SQL_COLUMN_UNKNOWN)

        /** The refinement over [snapshot]'s project, or null when the project is not prepared. */
        fun of(model: LocalSemanticModel, snapshot: ProjectSnapshot): LocalSpecificationCoverage? =
            if (snapshot.preparationState == ProjectPreparationState.NOT_PREPARED) null
            else LocalSpecificationCoverage(model, snapshot.projectIndex)
    }
}

/**
 * The outcomes of a file's references, computed once: the resolver's steps, refined with the specification coverage
 * when the project is prepared. The facts and the forwarded calls read the same values.
 */
internal class LocalReferenceOutcomes(private val model: LocalSemanticModel, private val coverage: LocalSpecificationCoverage?) {
    private val steps = IdentityHashMap<LocalReference, List<LocalResolution>>()

    fun steps(reference: LocalReference): List<LocalResolution> = steps.getOrPut(reference) {
        val resolved = LocalResolver.resolveQualified(model, reference)
        coverage?.refine(reference, resolved) ?: resolved
    }

    fun first(reference: LocalReference): LocalResolution = steps(reference).first()

    /** See [LocalSpecificationCoverage.specificationDefaults]; null without a prepared project. */
    fun specificationDefaults(definition: LocalDeclarationId): List<Boolean>? = coverage?.specificationDefaults(definition)

    companion object {
        /** An outcome after which the caller looks past the local scope: proven by the file, or with project coverage. */
        fun isAbsent(outcome: LocalResolution) =
            outcome == LocalResolution.ProvenAbsent || (outcome as? LocalResolution.SpecificationCovered)?.outcome == LocalResolution.ProvenAbsent

        fun isSpecificationMember(outcome: LocalResolution) =
            (outcome as? LocalResolution.SpecificationCovered)?.outcome == LocalResolution.SpecificationMember
    }
}

/** What a package specification in another file declares for a name, as plain data. */
internal sealed interface LocalSpecificationValue {
    /** A variable or exception of the specification, at its name's position in [fileId]. */
    data class Declared(val line: Int, val column: Int, val fileId: String) : LocalSpecificationValue

    /** The specification declares no variable or exception of the name. */
    object None : LocalSpecificationValue

    /** The project does not prove what the specification declares. */
    object Unknown : LocalSpecificationValue
}
