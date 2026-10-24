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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.AnchoredTypeRef
import com.felipebz.zpa.project.SourceRange
import java.util.*
import com.felipebz.zpa.symbols.local.LocalReferenceContext as Ctx

/**
 * Resolves a qualified name segment by segment. The first segment is [LocalResolver.resolve]; only its
 * [LocalResolution.ProvenAbsent] lets a caller try the whole name elsewhere. Otherwise each further segment is a member
 * of the entity bound so far, with no fallback. The result has one outcome per segment up to the first that stops; a
 * `%` attribute ends it.
 *
 * Members by entity: a label, subprogram, cursor with parameters or unit gives its own region, reachable only from
 * inside it; a value typed by a local record type gives a field, by a local collection type a method of its form;
 * other datatypes are typing; an exception or a type name takes no member. Each step relies on the regions it reads.
 */
internal object LocalQualifiedResolver {

    private val CALLABLES = EnumSet.of(LocalDeclarationKind.FUNCTION, LocalDeclarationKind.PROCEDURE)

    private val ARRAY_METHODS = setOf("COUNT", "FIRST", "LAST", "PRIOR", "NEXT", "EXISTS", "DELETE", "LIMIT")

    /** Methods of nested tables and varrays only: on an associative array they fail (PLS-00306). */
    private val SIZING_METHODS = setOf("EXTEND", "TRIM")

    /** A member step before completeness: its outcome, the regions it read and its own lookup gaps. */
    private class Step(
        val outcome: LocalResolution,
        val regions: List<LocalRegion> = emptyList(),
        val gaps: Set<LocalLookupGap> = emptySet(),
        val reasons: Set<LocalIncompleteness> = emptySet()
    )

    fun resolve(model: LocalSemanticModel, reference: LocalReference): List<LocalResolution> {
        val first = LocalResolver.resolve(model, reference)
        val steps = arrayListOf(first)
        val members = reference.segments.takeWhile { !it.isAttribute }
        if (members.isEmpty()) return steps
        val chain = LocalResolver.chain(model, reference.region)
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        val gaps = EnumSet.noneOf(LocalLookupGap::class.java)
        var current = first
        if (current is LocalResolution.Incomplete) {
            reasons += current.regionReasons
            gaps += current.lookupReasons
            current = current.provisional
        }
        for (index in members.indices) {
            val member = member(reference, index)
            val step = step(model, current, member, chain) ?: break
            if (step.outcome is LocalResolution.Unsupported) {
                steps += step.outcome
                break
            }
            step.regions.forEach { reasons += it.completeness.reasons }
            reasons += step.reasons
            gaps += step.gaps
            val outcome = memberOutcome(model, step.outcome)
            steps += if (reasons.isEmpty() && gaps.isEmpty()) outcome else LocalResolution.Incomplete(reasons, gaps, outcome)
            if (step.outcome !is LocalResolution.Bound && step.outcome !is LocalResolution.Field) break
            current = step.outcome
        }
        return steps
    }

    /** The member segment [index] of [reference] (attributes excluded) as a reference of its own. */
    fun member(reference: LocalReference, index: Int): LocalReference {
        val members = reference.segments.takeWhile { !it.isAttribute }
        val context = if (index == members.lastIndex) reference.lastSegmentContext else Ctx.QUALIFIER
        return LocalReference(members[index].name, reference.range, context, reference.region, emptyList(), context, false, reference.handler)
    }

    /** A qualified name is never a SQL built-in: the stored functions or the value it names bind. */
    fun memberOutcome(model: LocalSemanticModel, outcome: LocalResolution): LocalResolution =
        (outcome as? LocalResolution.SqlBuiltinOrLocal)?.let { builtinOrLocal ->
            val declarations = builtinOrLocal.declarations
            if (model.declaration(declarations.first()).kind in CALLABLES) LocalResolution.OverloadSet(declarations)
            else LocalResolution.Bound(declarations.single())
        } ?: outcome

    /** The member step after [qualifier], or null when [qualifier] takes no member step (it already stopped). */
    private fun step(model: LocalSemanticModel, qualifier: LocalResolution, member: LocalReference, chain: List<LocalRegion>): Step? =
        when (qualifier) {
            // A subprogram name outside its body (PLS-00225).
            is LocalResolution.OverloadSet -> Step(LocalResolution.OutOfScope(qualifier.declarations.first()))
            is LocalResolution.Field -> {
                val field = (model.declaration(qualifier.recordType).details as LocalRecordType).fields[qualifier.ordinal]
                typed(model, field.type, member)
            }
            is LocalResolution.Bound -> bound(model, model.declaration(qualifier.declaration), member, chain)
            is LocalResolution.ImplicitSelf -> self(model, (model.declaration(qualifier.method).details as LocalSubprogram).body!!, member, chain)
            else -> null
        }

    private fun bound(model: LocalSemanticModel, declaration: LocalDeclaration, member: LocalReference, chain: List<LocalRegion>): Step {
        val onChain = chain.mapTo(HashSet()) { it.id }
        fun regionOrOutOfScope(region: LocalRegionId?): Step =
            if (region != null && region in onChain) inRegion(model, model.region(region), member, chain)
            else Step(LocalResolution.OutOfScope(declaration.id))
        // An explicit SELF parameter of a method (`SELF IN OUT t` of a constructor) is the implicit one written out.
        val region = model.region(declaration.region)
        if (declaration.kind == LocalDeclarationKind.PARAMETER && declaration.name == LocalResolver.SELF &&
            model.parent(region.id)?.kind == LocalRegionKind.TYPE_BODY
        ) return self(model, region.id, member, chain)
        return when (val details = declaration.details) {
            is LocalLabel -> if (details.labeled == null) unsupported(LocalUnsupportedConstruct.MEMBER_OF_STATEMENT_LABEL)
            else regionOrOutOfScope(details.labeled)
            is LocalSubprogram -> regionOrOutOfScope(details.body)
            is LocalUnit -> regionOrOutOfScope(details.region)
            is LocalCursor -> when {
                details.parameters != null -> regionOrOutOfScope(details.parameters)
                contains(declaration.range, member.range) -> unsupported(LocalUnsupportedConstruct.CURSOR_MEMBER_INSIDE_CURSOR)
                else -> Step(LocalResolution.OutOfScope(declaration.id))
            }
            else -> when (declaration.kind) {
                LocalDeclarationKind.VARIABLE, LocalDeclarationKind.PARAMETER, LocalDeclarationKind.CURSOR_PARAMETER ->
                    typed(model, declaration.type, member)
                LocalDeclarationKind.EXCEPTION, LocalDeclarationKind.SUBTYPE, LocalDeclarationKind.RECORD_TYPE,
                LocalDeclarationKind.COLLECTION_TYPE, LocalDeclarationKind.REF_CURSOR_TYPE -> Step(LocalResolution.WrongKind(declaration.id))
                else -> unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING)
            }
        }
    }

    /** A member of a region a qualifier names: its declarations (with a linked specification), with the order rule. */
    private fun inRegion(model: LocalSemanticModel, region: LocalRegion, member: LocalReference, chain: List<LocalRegion>): Step {
        val scope = LocalResolver.scopeOf(model, region)
        val gaps = scope.gaps
        val candidates = scope.named(member.name)
        val outcome = if (candidates.isEmpty()) LocalResolution.Undeclared else LocalResolver.decide(model, member, scope, candidates, chain)
        return Step(outcome, scope.regions, gaps)
    }

    /**
     * A member of `SELF` in the method whose region is [method]: an attribute or method of the type, the supertypes'
     * included (missing: PLS-00302).
     */
    private fun self(model: LocalSemanticModel, method: LocalRegionId, member: LocalReference, chain: List<LocalRegion>): Step {
        val scope = LocalTypeScope.of(model, model.parent(method)!!, supertypeMethods = true)
        val candidates = scope.named(member.name).filter {
            it.kind == LocalDeclarationKind.ATTRIBUTE || it.kind in CALLABLES
        }
        val outcome = when {
            candidates.isEmpty() -> LocalResolution.Undeclared
            // A static method cannot be invoked on an instance (PLS-00587).
            candidates.all { (it.details as? LocalSubprogram)?.methodKind == LocalMethodKind.STATIC } -> LocalResolution.WrongKind(candidates.first().id)
            else -> LocalResolver.decide(model, member, scope, candidates, chain)
        }
        // A supertype outside the file may declare the name, or further overloads of a method (attributes cannot repeat).
        val reliesOnSupertypes = candidates.isEmpty() || candidates.any { it.kind in CALLABLES }
        return Step(outcome, scope.regions, if (reliesOnSupertypes) scope.gaps + scope.absenceGaps else scope.gaps)
    }

    /** A member of a value whose declared type is [type]: a record field or a collection method of a local type. */
    private fun typed(model: LocalSemanticModel, type: LocalTypeUse?, member: LocalReference): Step {
        val typeRef = type?.typeRef ?: return unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING)
        if (typeRef is AnchoredTypeRef || typeRef.name.segments.size != 1) return unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING)
        val lookup = LocalReference(typeRef.name.segments.single(), type.range, Ctx.TYPE, type.lookupRegion, emptyList(), Ctx.TYPE, false, null)
        var resolved = LocalResolver.resolve(model, lookup)
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        val gaps = EnumSet.noneOf(LocalLookupGap::class.java)
        if (resolved is LocalResolution.Incomplete) {
            reasons += resolved.regionReasons
            gaps += resolved.lookupReasons
            resolved = resolved.provisional
        }
        val declaration = (resolved as? LocalResolution.Bound)?.let { model.declaration(it.declaration) }
            ?: return unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING)
        val outcome = when (val details = declaration.details) {
            is LocalRecordType -> {
                val ordinal = details.fields.indexOfFirst { it.name == member.name }
                if (ordinal < 0) LocalResolution.Undeclared else LocalResolution.Field(declaration.id, ordinal)
            }
            is LocalCollectionType -> when (member.name.lookupName) {
                in ARRAY_METHODS -> LocalResolution.CollectionMethod(declaration.id)
                in SIZING_METHODS -> if (details.form == LocalCollectionForm.ASSOCIATIVE_ARRAY) LocalResolution.WrongKind(declaration.id)
                else LocalResolution.CollectionMethod(declaration.id)
                else -> LocalResolution.Undeclared // PLS-00302, for every collection form
            }
            else -> return unsupported(LocalUnsupportedConstruct.MEMBER_NEEDS_TYPING)
        }
        return Step(outcome, emptyList(), gaps, reasons)
    }

    private fun unsupported(construct: LocalUnsupportedConstruct) = Step(LocalResolution.Unsupported(construct))

    private fun contains(outer: SourceRange, inner: SourceRange) =
        (outer.startLine < inner.startLine || outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn) &&
            (inner.endLine < outer.endLine || inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn)
}
