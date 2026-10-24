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
 * | DECLARED_BEFORE | yes | the gap stays: overloads across specification and body form one set |
 * | REJECTED or UNKNOWN | either | the gap stays |
 *
 * Every decision is wrapped in [LocalResolution.SpecificationCovered], so a consumer can tell it from one the file
 * proved alone. Absence is still the only outcome after which a caller looks past the local scope: an absent name is
 * one neither the body nor the specification declares.
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
        val first = steps.first() as? LocalResolution.Incomplete ?: return steps
        typeBody(reference, first)?.let { return listOf(LocalResolution.SpecificationCovered(it)) }
        if (!onlySpecificationGap(first)) return steps
        val chain = LocalResolver.chain(model, reference.region)
        if (chain.any { it.kind == LocalRegionKind.TYPE_BODY || it.kind == LocalRegionKind.OBJECT_TYPE }) return steps
        val body = chain.firstOrNull { it.kind == LocalRegionKind.PACKAGE_BODY } ?: return steps
        val specification = specification(body) ?: return steps
        val decided = decide(specification, body, reference.name, first.provisional, LocalResolution.ProvenAbsent) ?: return steps
        if (decided == LocalResolution.ProvenAbsent || decided == LocalResolution.SpecificationMember || steps.size == 1) {
            return listOf(LocalResolution.SpecificationCovered(decided))
        }
        val result = arrayListOf<LocalResolution>(LocalResolution.SpecificationCovered(decided))
        var rest = steps.drop(1)
        // The package's own name as qualifier: the next segment is a member of the body and its specification.
        if (isOwnUnit(decided, body)) {
            val member = rest.first() as? LocalResolution.Incomplete
            val decidedMember = member?.takeIf(::onlySpecificationGap)
                ?.let { decide(specification, body, reference.segments.first().name, it.provisional, LocalResolution.Undeclared) }
                ?: return result + rest
            result += LocalResolution.SpecificationCovered(decidedMember)
            if (decidedMember == LocalResolution.SpecificationMember) return result
            rest = rest.drop(1)
        }
        for (step in rest) {
            result += if (step is LocalResolution.Incomplete && onlySpecificationGap(step)) LocalResolution.SpecificationCovered(step.provisional) else step
        }
        return result
    }

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

    private fun onlySpecificationGap(outcome: LocalResolution.Incomplete) =
        outcome.regionReasons.isEmpty() && outcome.lookupReasons == setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE)

    /** The decision for [name], whose resolver outcome without the gap is [provisional]; null keeps the gap. */
    private fun decide(
        specification: IndexedDeclaration,
        body: LocalRegion,
        name: OracleIdentifier,
        provisional: LocalResolution,
        absent: LocalResolution
    ): LocalResolution? = when (PackageMemberNames.at(index, specification, name, null).status) {
        MemberNameStatus.NOT_DECLARED -> provisional.takeIf { it == absent || declaredByBody(it, body) }
        MemberNameStatus.DECLARED_BEFORE -> LocalResolution.SpecificationMember.takeIf { provisional == absent }
        MemberNameStatus.REJECTED, MemberNameStatus.UNKNOWN -> null
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

    companion object {
        /** An outcome after which the caller looks past the local scope: proven by the file, or with project coverage. */
        fun isAbsent(outcome: LocalResolution) =
            outcome == LocalResolution.ProvenAbsent || (outcome as? LocalResolution.SpecificationCovered)?.outcome == LocalResolution.ProvenAbsent

        fun isSpecificationMember(outcome: LocalResolution) =
            (outcome as? LocalResolution.SpecificationCovered)?.outcome == LocalResolution.SpecificationMember
    }
}
