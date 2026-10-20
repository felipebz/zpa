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

import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.symbols.local.LocalDeclarationKind.*
import java.util.*
import com.felipebz.zpa.symbols.local.LocalReferenceContext as Ctx

/**
 * Looks one [LocalReference] up in the regions of its [LocalSemanticModel]. Pure and deterministic.
 *
 * - The innermost region of the chain (stopping before the file region) that declares the name with any kind decides;
 *   a wrong kind or a later declaration stops the search.
 * - A package body decides together with its linked specification; specification items are visible from the whole body.
 * - A forward declaration and its body, a specification heading and its implementation, and a forward cursor and its
 *   single definition are one entity. Callables give an overload set; other multiple entities, or two bodies of one
 *   heading, are a duplicate.
 * - A declaration is earlier when its range ends before the reference starts, except for a subprogram or unit seen
 *   from inside itself, specification items seen from their body, and labels.
 * - File declarations never bind plain names; top-level block labels decide inside their block, and enclosing unit
 *   names bind as qualifiers.
 * - Every region an outcome relies on must be complete. The file region counts only when the reference is in no region
 *   or the chain ends at a top-level block. [LocalResolution.Unsupported] is never wrapped.
 */
internal object LocalResolver {

    private enum class Fit { APPLICABLE, WRONG, UNSUPPORTED }

    private val VALUES = EnumSet.of(VARIABLE, PARAMETER, CURSOR_PARAMETER, LOOP_INDEX)
    private val TYPES = EnumSet.of(SUBTYPE, RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE)
    private val CALLABLES = EnumSet.of(PROCEDURE, FUNCTION)
    private val QUALIFYING_UNITS = EnumSet.of(PACKAGE, PACKAGE_BODY, PROCEDURE, FUNCTION)

    fun resolve(model: LocalSemanticModel, reference: LocalReference): LocalResolution {
        if (reference.inParameterDefault) return LocalResolution.Unsupported(LocalUnsupportedConstruct.PARAMETER_DEFAULT)
        val chain = chain(model, reference.region)
        if (chain.any { it.kind == LocalRegionKind.OBJECT_TYPE || it.kind == LocalRegionKind.TYPE_BODY }) {
            return LocalResolution.Unsupported(LocalUnsupportedConstruct.OBJECT_TYPE_MEMBER)
        }
        if (chain.any { it.kind == LocalRegionKind.SQL_WITH }) {
            return LocalResolution.Unsupported(LocalUnsupportedConstruct.SQL_WITH_SUBPROGRAM)
        }
        val relied = ArrayList<LocalRegion>()
        val gaps = EnumSet.noneOf(LocalLookupGap::class.java)
        var outcome: LocalResolution? = null
        for (region in chain) {
            val scope = scopeOf(model, region)
            relied += scope.regions
            if (region.kind == LocalRegionKind.PACKAGE_BODY && scope.specification == null) gaps += LocalLookupGap.SPECIFICATION_NOT_IN_FILE
            val candidates = scope.declarations.filter { it.name == reference.name }
            if (candidates.isNotEmpty()) {
                outcome = decide(model, reference, scope, candidates, chain)
                break
            }
        }
        if (outcome == null) {
            val candidates = fileCandidates(model, reference, chain)
            if (reliesOnFileRegion(chain)) relied += model.root
            outcome = if (candidates.isEmpty()) LocalResolution.ProvenAbsent
            else decide(model, reference, Scope(listOf(model.root), candidates, null, null), candidates, chain)
        }
        if (reference.context == Ctx.SQL_VALUE) outcome = sqlOutcome(outcome, gaps)
        // An unmodeled construct is reported as such, whatever the regions' verdicts.
        if (outcome is LocalResolution.Unsupported) return outcome
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        relied.forEach { reasons += it.completeness.reasons }
        return if (reasons.isEmpty() && gaps.isEmpty()) outcome else LocalResolution.Incomplete(reasons, gaps, outcome)
    }

    /**
     * Whether the file step depends on the file region's own text: only when the reference is in no region, or when the
     * chain ends at a top-level block, whose labels are file declarations written outside the block's range (hidden
     * text next to them touches only the file region). A CREATE unit's region covers the whole unit, its own name
     * included, so nothing the file step reads for it lies outside the chain.
     */
    private fun reliesOnFileRegion(chain: List<LocalRegion>) =
        chain.isEmpty() || chain.last().kind == LocalRegionKind.ANONYMOUS_BLOCK

    /** The regions from [region] outward, without the file region. */
    private fun chain(model: LocalSemanticModel, region: LocalRegionId): List<LocalRegion> {
        val chain = ArrayList<LocalRegion>()
        var current: LocalRegion? = model.region(region)
        while (current != null && current.kind != LocalRegionKind.FILE) {
            chain += current
            current = model.parent(current.id)
        }
        return chain
    }

    /** A region as a lookup sees it: a linked package body together with its specification. */
    private class Scope(
        val regions: List<LocalRegion>,
        val declarations: List<LocalDeclaration>,
        /** The linked specification region of a package body. */
        val specification: LocalRegionId?,
        val body: LocalRegion?
    )

    private fun scopeOf(model: LocalSemanticModel, region: LocalRegion): Scope {
        if (region.kind != LocalRegionKind.PACKAGE_BODY) return Scope(listOf(region), model.declarationsOf(region.id), null, null)
        val unit = model.declarations.firstOrNull { (it.details as? LocalUnit)?.region == region.id }?.details as? LocalUnit
        val specification = unit?.counterpart ?: return Scope(listOf(region), model.declarationsOf(region.id), null, region)
        return Scope(
            listOf(region, model.region(specification)),
            model.declarationsOf(region.id) + model.declarationsOf(specification),
            specification,
            region
        )
    }

    /** File-region declarations a reference may see: enclosing top-level labels, and enclosing unit names as qualifiers. */
    private fun fileCandidates(model: LocalSemanticModel, reference: LocalReference, chain: List<LocalRegion>): List<LocalDeclaration> {
        val onChain = chain.mapTo(HashSet()) { it.id }
        return model.declarationsOf(model.root.id).filter { declaration ->
            declaration.name == reference.name && when (val details = declaration.details) {
                is LocalLabel -> details.labeled in onChain
                is LocalUnit -> reference.context == Ctx.QUALIFIER && declaration.kind in QUALIFYING_UNITS && details.region in onChain
                is LocalSubprogram -> reference.context == Ctx.QUALIFIER && details.body in onChain
                else -> false
            }
        }
    }

    private fun decide(
        model: LocalSemanticModel,
        reference: LocalReference,
        scope: Scope,
        candidates: List<LocalDeclaration>,
        chain: List<LocalRegion>
    ): LocalResolution {
        val entities = entities(candidates)
        val earlier = entities.map { isEarlier(it, reference, scope, chain) }
        if (entities.size > 1) {
            if (entities.any { !isCallable(it) } || hasTwoBodiesOfOneHeading(entities)) {
                return LocalResolution.Duplicate(entities.map(::representative))
            }
            return when {
                earlier.none { it } -> LocalResolution.DeclaredLater(representative(entities.first()))
                earlier.any { !it } -> LocalResolution.Unsupported(LocalUnsupportedConstruct.OVERLOADS_PARTLY_LATER)
                else -> callables(entities, reference.context, chain)
            }
        }
        val entity = entities.single()
        val declaration = model.declaration(representative(entity))
        if (!earlier.single()) return LocalResolution.DeclaredLater(declaration.id)
        if (isCallable(entity)) return callables(entities, reference.context, chain)
        return when (fit(declaration.kind, reference.context)) {
            Fit.APPLICABLE -> LocalResolution.Bound(declaration.id)
            Fit.WRONG -> LocalResolution.WrongKind(declaration.id)
            Fit.UNSUPPORTED -> LocalResolution.Unsupported(LocalUnsupportedConstruct.KIND_IN_CONTEXT, declaration.kind, reference.context)
        }
    }

    /**
     * Callables decide: an overload set, unless no callable of the set can be used here (KC). As a qualifier, the
     * subprogram whose body encloses the reference is the one named (NS11).
     */
    private fun callables(entities: List<List<LocalDeclaration>>, context: Ctx, chain: List<LocalRegion>): LocalResolution {
        val representatives = entities.map(::representative)
        if (context == Ctx.QUALIFIER) {
            val enclosing = entities.firstOrNull { entity -> entity.any { encloses(it, chain) } }
            if (enclosing != null) return LocalResolution.Bound(representative(enclosing))
        }
        val kinds = entities.mapTo(EnumSet.noneOf(LocalDeclarationKind::class.java)) { it.first().kind }
        val wrong = when (context) {
            Ctx.QUALIFIER -> false
            Ctx.CALL_STATEMENT -> PROCEDURE !in kinds // PLS-00221
            Ctx.VALUE, Ctx.SQL_VALUE, Ctx.CALL_EXPRESSION -> FUNCTION !in kinds // PLS-00222
            Ctx.TYPE, Ctx.ANCHOR_TYPE, Ctx.ANCHOR_ROWTYPE, Ctx.EXCEPTION -> true // PLS-00488, PLS-00206, PLS-00310, PLS-00485…
        }
        return if (wrong) LocalResolution.WrongKind(representatives.first()) else LocalResolution.OverloadSet(representatives)
    }

    /**
     * Whether a declaration of [kind] can be what [context] names. Every WRONG pair was observed as a compile error
     * that did not fall back to an outer declaration (KC matrix, LT07_D, LT04_A, SP06_C, SP07_E, NS04_D); every
     * APPLICABLE pair compiled.
     */
    private fun fit(kind: LocalDeclarationKind, context: Ctx): Fit = when (context) {
        Ctx.QUALIFIER -> Fit.APPLICABLE
        Ctx.VALUE, Ctx.SQL_VALUE -> when {
            kind in VALUES -> Fit.APPLICABLE
            kind == EXCEPTION || kind == CURSOR || kind == LABEL || kind in TYPES -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        Ctx.CALL_STATEMENT -> when {
            kind == CURSOR_PARAMETER -> Fit.UNSUPPORTED
            kind in VALUES || kind in TYPES || kind == EXCEPTION || kind == CURSOR || kind == LABEL -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // Element access, a subtype conversion (ST_A) or a constructor: whether the call fits is typing.
        Ctx.CALL_EXPRESSION -> when (kind) {
            VARIABLE, PARAMETER, LOOP_INDEX, SUBTYPE, RECORD_TYPE, COLLECTION_TYPE -> Fit.APPLICABLE
            EXCEPTION, CURSOR, LABEL, REF_CURSOR_TYPE -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        Ctx.TYPE -> when {
            kind in TYPES -> Fit.APPLICABLE
            kind == VARIABLE || kind == PARAMETER || kind == LOOP_INDEX || kind == LABEL || kind == EXCEPTION || kind == CURSOR -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // A parameter or loop index cannot anchor %TYPE (KC, PLS-00206); a constant can (AN_A).
        Ctx.ANCHOR_TYPE -> when (kind) {
            VARIABLE -> Fit.APPLICABLE
            PARAMETER, LOOP_INDEX, LABEL, EXCEPTION, CURSOR, SUBTYPE, RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // A variable or parameter anchors %ROWTYPE only when it is a cursor variable (AN_B, AN_C; LT04_A): typing.
        Ctx.ANCHOR_ROWTYPE -> when (kind) {
            CURSOR -> Fit.APPLICABLE
            LOOP_INDEX, LABEL, EXCEPTION, SUBTYPE, RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        Ctx.EXCEPTION -> when {
            kind == EXCEPTION -> Fit.APPLICABLE
            kind in VALUES && kind != CURSOR_PARAMETER || kind in TYPES || kind == CURSOR || kind == LABEL -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
    }

    /** In SQL a column of the same name wins over a local (AD06): a single local is only one of two readings. */
    private fun sqlOutcome(outcome: LocalResolution, gaps: EnumSet<LocalLookupGap>): LocalResolution = when (outcome) {
        is LocalResolution.Bound -> LocalResolution.SqlColumnOrLocal(outcome.declaration)
        is LocalResolution.OverloadSet -> outcome.declarations.singleOrNull()?.let { LocalResolution.SqlColumnOrLocal(it) }
            ?: LocalResolution.Unsupported(LocalUnsupportedConstruct.SQL_NAME_WITHOUT_SINGLE_DECLARATION)
        LocalResolution.ProvenAbsent -> {
            gaps += LocalLookupGap.SQL_COLUMN_UNKNOWN
            outcome
        }
        is LocalResolution.Unsupported -> outcome
        else -> LocalResolution.Unsupported(LocalUnsupportedConstruct.SQL_NAME_WITHOUT_SINGLE_DECLARATION)
    }

    /** Candidates grouped into entities, each sorted by id, in order of their first member. */
    private fun entities(candidates: List<LocalDeclaration>): List<List<LocalDeclaration>> {
        val byId = candidates.associateBy { it.id }
        val parent = HashMap<LocalDeclarationId, LocalDeclarationId>()
        fun find(id: LocalDeclarationId): LocalDeclarationId {
            var current = id
            while (true) current = parent[current]?.takeIf { it != current } ?: return current
        }
        fun union(a: LocalDeclarationId, b: LocalDeclarationId?) {
            if (b == null || b !in byId) return
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) if (ra.ordinal < rb.ordinal) parent[rb] = ra else parent[ra] = rb
        }
        for (candidate in candidates) {
            val details = candidate.details as? LocalSubprogram ?: continue
            union(candidate.id, details.counterpart)
            union(candidate.id, details.specificationCounterpart)
        }
        val cursors = candidates.filter { it.kind == CURSOR }
        val definition = cursors.filter { !(it.details as LocalCursor).isForward }.singleOrNull()
        if (definition != null) cursors.forEach { union(it.id, definition.id) }
        return candidates.groupBy { find(it.id) }.values.map { members -> members.sortedBy { it.id.ordinal } }
            .sortedBy { it.first().id.ordinal }
    }

    private fun isCallable(entity: List<LocalDeclaration>) = entity.all { it.kind in CALLABLES }

    /** Two subprogram bodies with one heading are not overloads but one subprogram defined twice (SP05_C, PLS-00305). */
    private fun hasTwoBodiesOfOneHeading(entities: List<List<LocalDeclaration>>): Boolean {
        val keys = HashSet<String>()
        return entities.flatten().any { declaration ->
            val subprogram = declaration.details as? LocalSubprogram
            subprogram?.form == LocalSubprogramForm.BODY &&
                LocalDeclarationLinker.signatureKey(declaration.kind, subprogram)?.let { !keys.add(it) } == true
        }
    }

    /** The member with a body (or the cursor definition) when there is one, else the earliest member. */
    private fun representative(entity: List<LocalDeclaration>): LocalDeclarationId =
        (entity.firstOrNull { hasBody(it) } ?: entity.first()).id

    private fun hasBody(declaration: LocalDeclaration) = when (val details = declaration.details) {
        is LocalSubprogram -> details.form == LocalSubprogramForm.BODY
        is LocalCursor -> !details.isForward
        else -> false
    }

    private fun isEarlier(entity: List<LocalDeclaration>, reference: LocalReference, scope: Scope, chain: List<LocalRegion>): Boolean {
        val hasDefinition = entity.any { it.kind == CURSOR && !(it.details as LocalCursor).isForward }
        return entity.any { member ->
            when {
                member.kind == LABEL -> true
                scope.specification != null && member.region == scope.specification -> true
                encloses(member, chain) -> true
                hasDefinition && (member.details as? LocalCursor)?.isForward == true -> false
                else -> endsBefore(member.range, reference.range)
            }
        }
    }

    /** Whether [declaration] is a subprogram or unit whose region is on [chain]: its name is visible inside it. */
    private fun encloses(declaration: LocalDeclaration, chain: List<LocalRegion>): Boolean {
        val region = when (val details = declaration.details) {
            is LocalSubprogram -> details.body
            is LocalUnit -> details.region
            else -> null
        } ?: return false
        return chain.any { it.id == region }
    }

    private fun endsBefore(declaration: SourceRange, reference: SourceRange) =
        declaration.endLine < reference.startLine ||
            declaration.endLine == reference.startLine && declaration.endColumn <= reference.startColumn
}
