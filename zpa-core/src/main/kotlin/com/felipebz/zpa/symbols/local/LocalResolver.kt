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

import com.felipebz.zpa.project.OracleIdentifier
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
 * - A type body decides together with its specification and the attributes of its supertypes in the file. Inside a
 *   member method or constructor the implicit `SELF` is declared in the method's region; a static method has none, and
 *   rejects the attributes and member methods it finds.
 * - In SQL a qualifier naming a relation of the statement is SQL; a local function called in SQL is reported as such.
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
    private val LABEL_TARGETS = EnumSet.of(Ctx.GOTO_TARGET, Ctx.LOOP_TARGET)
    val SELF: OracleIdentifier = OracleIdentifier.fromSource("SELF")

    fun resolve(model: LocalSemanticModel, reference: LocalReference): LocalResolution {
        if (reference.inParameterDefault) return LocalResolution.Unsupported(LocalUnsupportedConstruct.PARAMETER_DEFAULT)
        // A relation of the SQL statement qualifies a column or object path, whatever the local regions declare.
        if (reference.context == Ctx.QUALIFIER && reference.sql?.qualifies(reference.name, reference.segments) == true) {
            return LocalResolution.SqlRelation
        }
        val chain = chain(model, reference.region)
        if (chain.any { it.kind == LocalRegionKind.SQL_WITH }) {
            return LocalResolution.Unsupported(LocalUnsupportedConstruct.SQL_WITH_SUBPROGRAM)
        }
        // In a specification only type names are observed: attributes and methods capture them.
        if (chain.any { it.kind == LocalRegionKind.OBJECT_TYPE } && reference.context != Ctx.TYPE) {
            return LocalResolution.Unsupported(LocalUnsupportedConstruct.OBJECT_TYPE_SPECIFICATION)
        }
        val method = LocalTypeScope.method(model, chain)
        val relied = ArrayList<LocalRegion>()
        val gaps = EnumSet.noneOf(LocalLookupGap::class.java)
        var outcome: LocalResolution? = null
        for (region in chain) {
            val scope = scopeOf(model, region)
            relied += scope.regions
            val candidates = scope.named(reference.name).filter { LocalTypeScope.capturesIn(it, reference.context) }
            gaps += if (candidates.isEmpty()) scope.gaps + scope.absenceGaps else scope.gaps
            val implicitSelf = method != null && region.id == method.region && reference.name == SELF &&
                (method.declaration.details as LocalSubprogram).implicitSelf != null
            if (implicitSelf) {
                outcome = when {
                    // A local named SELF collides with the implicit parameter (PLS-00410).
                    candidates.isNotEmpty() -> LocalResolution.Duplicate(candidates.map { it.id })
                    reference.context == Ctx.VALUE || reference.context == Ctx.QUALIFIER -> LocalResolution.ImplicitSelf(method.declaration.id)
                    else -> LocalResolution.Unsupported(LocalUnsupportedConstruct.KIND_IN_CONTEXT, null, reference.context)
                }
                break
            }
            if (candidates.isNotEmpty()) {
                outcome = decide(model, reference, scope, candidates, chain)
                if (region.kind == LocalRegionKind.TYPE_BODY && method != null) outcome = LocalTypeScope.rejectAcrossInstance(model, outcome, method.isStatic)
                break
            }
        }
        if (outcome == null) {
            val candidates = fileCandidates(model, reference, chain)
            if (reliesOnFileRegion(chain)) relied += model.root
            outcome = if (candidates.isEmpty()) {
                when {
                    // Labels are never schema objects: a target nothing declares is an error, not an absence.
                    reference.context in LABEL_TARGETS -> LocalResolution.Undeclared
                    // A static method has no SELF (PLS-00201).
                    method?.isStatic == true && reference.name == SELF -> LocalResolution.Undeclared
                    else -> LocalResolution.ProvenAbsent
                }
            } else decide(model, reference, Scope(model, listOf(model.root), null), candidates, chain)
        }
        if (reference.context == Ctx.SQL_VALUE) outcome = sqlOutcome(outcome, gaps)
        // An unmodeled construct is reported as such, whatever the regions' verdicts.
        if (outcome is LocalResolution.Unsupported) return outcome
        val reasons = EnumSet.noneOf(LocalIncompleteness::class.java)
        relied.forEach { reasons += it.completeness.reasons }
        return if (reasons.isEmpty() && gaps.isEmpty()) outcome else LocalResolution.Incomplete(reasons, gaps, outcome)
    }

    /**
     * The outcome of each segment of a qualified [reference], up to the first that stops (see
     * [LocalQualifiedResolver]); a single outcome for a single segment.
     */
    fun resolveQualified(model: LocalSemanticModel, reference: LocalReference): List<LocalResolution> =
        LocalQualifiedResolver.resolve(model, reference)

    /**
     * Whether the file step depends on the file region's own text: only when the reference is in no region, or when the
     * chain ends at a top-level block, whose labels are file declarations written outside the block's range (hidden
     * text next to them touches only the file region). A CREATE unit's region covers the whole unit, its own name
     * included, so nothing the file step reads for it lies outside the chain.
     */
    private fun reliesOnFileRegion(chain: List<LocalRegion>) =
        chain.isEmpty() || chain.last().kind == LocalRegionKind.ANONYMOUS_BLOCK

    /** The regions from [region] outward, without the file region. */
    fun chain(model: LocalSemanticModel, region: LocalRegionId): List<LocalRegion> {
        val chain = ArrayList<LocalRegion>()
        var current: LocalRegion? = model.region(region)
        while (current != null && current.kind != LocalRegionKind.FILE) {
            chain += current
            current = model.parent(current.id)
        }
        return chain
    }

    /**
     * A region as a lookup sees it: a linked package body together with its specification, a type body or
     * specification together with its specification and supertypes.
     */
    class Scope(
        private val model: LocalSemanticModel,
        val regions: List<LocalRegion>,
        /** The linked specification region of a package or type body. */
        val specification: LocalRegionId?,
        /** Regions whose declarations are visible throughout the scope, whatever their order. */
        val earlierRegions: Set<LocalRegionId> = setOfNotNull(specification),
        val gaps: Set<LocalLookupGap> = emptySet(),
        /** Gaps that matter only when the scope does not declare the name. */
        val absenceGaps: Set<LocalLookupGap> = emptySet(),
        /** Regions of which only the attributes are visible (supertypes seen unqualified). */
        private val attributesOnly: Set<LocalRegionId> = emptySet(),
        /**
         * Body definitions of a specification in another file, in the order of its headings: visible throughout the
         * scope, and ordered first, as the headings they implement would be with the specification in the file.
         */
        val definitions: List<LocalDeclarationId> = emptyList()
    ) {
        fun named(name: OracleIdentifier): List<LocalDeclaration> = regions.flatMap { region ->
            val found = model.index.named(region.id, name)
            if (region.id in attributesOnly) found.filter { it.kind == ATTRIBUTE } else found
        }
    }

    fun scopeOf(model: LocalSemanticModel, region: LocalRegion): Scope = when (region.kind) {
        LocalRegionKind.PACKAGE_BODY -> LocalTypeScope.unitOf(model, region.id)?.counterpart?.let { Scope(model, listOf(region, model.region(it)), it) }
            ?: Scope(model, listOf(region), null, gaps = setOf(LocalLookupGap.SPECIFICATION_NOT_IN_FILE))
        LocalRegionKind.TYPE_BODY, LocalRegionKind.OBJECT_TYPE -> LocalTypeScope.of(model, region, supertypeMethods = false)
        else -> Scope(model, listOf(region), null)
    }

    /**
     * The decision of a package [body] whose specification is in another file, where [definitions] are the body's
     * definitions of every specification heading of the name, in heading order: the decision with the specification in
     * the file, where each heading and its definition are one entity visible throughout the body. A [member] follows the
     * package's own name (a member step: every kind, no SQL reading). Null when the body does not declare the name.
     */
    fun decideInBody(
        model: LocalSemanticModel,
        reference: LocalReference,
        body: LocalRegion,
        definitions: List<LocalDeclarationId>,
        member: Boolean
    ): LocalResolution? {
        val scope = Scope(model, listOf(body), null, definitions = definitions)
        val named = scope.named(reference.name)
        val candidates = if (member) named else named.filter { LocalTypeScope.capturesIn(it, reference.context) }
        if (candidates.isEmpty()) return null
        val outcome = decide(model, reference, scope, candidates, chain(model, reference.region))
        return if (!member && reference.context == Ctx.SQL_VALUE) sqlOutcome(outcome, EnumSet.noneOf(LocalLookupGap::class.java)) else outcome
    }

    /** File-region declarations a reference may see: enclosing top-level labels, and enclosing unit names as qualifiers. */
    private fun fileCandidates(model: LocalSemanticModel, reference: LocalReference, chain: List<LocalRegion>): List<LocalDeclaration> {
        val onChain = chain.mapTo(HashSet()) { it.id }
        return model.index.named(model.root.id, reference.name).filter { declaration ->
            when (val details = declaration.details) {
                is LocalLabel -> details.labeled in onChain
                is LocalUnit -> reference.context == Ctx.QUALIFIER && declaration.kind in QUALIFYING_UNITS && details.region in onChain
                is LocalSubprogram -> reference.context == Ctx.QUALIFIER && details.body in onChain
                else -> false
            }
        }
    }

    fun decide(
        model: LocalSemanticModel,
        reference: LocalReference,
        scope: Scope,
        candidates: List<LocalDeclaration>,
        chain: List<LocalRegion>
    ): LocalResolution {
        val entities = entities(candidates, scope.definitions)
        val earlier = entities.map { isEarlier(it, reference, scope, chain) }
        if (entities.size > 1) {
            if (entities.any { !isCallable(it) } || hasTwoBodiesOfOneHeading(entities)) {
                return LocalResolution.Duplicate(entities.map(::representative))
            }
            return when {
                earlier.none { it } -> LocalResolution.DeclaredLater(representative(entities.first()))
                earlier.any { !it } -> LocalResolution.Unsupported(LocalUnsupportedConstruct.OVERLOADS_PARTLY_LATER)
                else -> callables(model, entities, reference.context, chain)
            }
        }
        val entity = entities.single()
        val declaration = model.declaration(representative(entity))
        if (!earlier.single()) return LocalResolution.DeclaredLater(declaration.id)
        if (isCallable(entity)) return callables(model, entities, reference.context, chain)
        if (reference.context == Ctx.SQL_CALL) LocalSqlScope.calledValue(declaration)?.let { return it }
        if (declaration.kind == LABEL && reference.context in LABEL_TARGETS) return labelTarget(model, declaration, reference)
        return when (fit(declaration.kind, reference.context)) {
            Fit.APPLICABLE -> LocalResolution.Bound(declaration.id)
            Fit.WRONG -> LocalResolution.WrongKind(declaration.id)
            Fit.UNSUPPORTED -> LocalResolution.Unsupported(LocalUnsupportedConstruct.KIND_IN_CONTEXT, declaration.kind, reference.context)
        }
    }

    /**
     * Callables decide: an overload set, unless no callable of the set can be used here. As a qualifier, the
     * subprogram whose body encloses the reference is the one named.
     */
    private fun callables(model: LocalSemanticModel, entities: List<List<LocalDeclaration>>, context: Ctx, chain: List<LocalRegion>): LocalResolution {
        val representatives = entities.map(::representative)
        if (context == Ctx.QUALIFIER) {
            val enclosing = entities.firstOrNull { entity -> entity.any { encloses(it, chain) } }
            if (enclosing != null) return LocalResolution.Bound(representative(enclosing))
        }
        val kinds = entities.mapTo(EnumSet.noneOf(LocalDeclarationKind::class.java)) { it.first().kind }
        val fit = when (context) {
            Ctx.QUALIFIER -> Fit.APPLICABLE
            Ctx.CALL_STATEMENT -> if (PROCEDURE in kinds) Fit.APPLICABLE else Fit.WRONG // PLS-00221
            Ctx.VALUE, Ctx.SQL_VALUE, Ctx.CALL_EXPRESSION -> if (FUNCTION in kinds) Fit.APPLICABLE else Fit.WRONG // PLS-00222
            Ctx.SQL_CALL, Ctx.INSERT_VALUE -> return LocalSqlScope.callables(model, entities, representatives, context)
            Ctx.TYPE, Ctx.ANCHOR_TYPE, Ctx.ANCHOR_ROWTYPE, Ctx.EXCEPTION -> Fit.WRONG // PLS-00488, PLS-00206, PLS-00310, PLS-00485…
            Ctx.CURSOR_OPERAND -> if (FUNCTION in kinds) Fit.UNSUPPORTED else Fit.WRONG // PLS-00456
            Ctx.FOR_CONTROL -> if (FUNCTION in kinds) Fit.APPLICABLE else Fit.UNSUPPORTED // a value iterated once
            Ctx.OPEN_FOR_OPERAND, Ctx.GOTO_TARGET, Ctx.LOOP_TARGET -> Fit.UNSUPPORTED
        }
        return when (fit) {
            Fit.APPLICABLE -> LocalResolution.OverloadSet(representatives)
            Fit.WRONG -> LocalResolution.WrongKind(representatives.first())
            Fit.UNSUPPORTED -> LocalResolution.Unsupported(LocalUnsupportedConstruct.KIND_IN_CONTEXT, kinds.first(), context)
        }
    }

    /**
     * No branch leaves a subprogram (PLS-00374, PLS-00375). `EXIT`/`CONTINUE` must name a loop (PLS-00373) that
     * encloses the statement (PLS-00374). A `GOTO` cannot enter a loop, a handler or an `IF`/`CASE` branch from outside
     * it, so it cannot cross branches either, nor branch from a handler into its own block (PLS-00375).
     */
    private fun labelTarget(model: LocalSemanticModel, label: LocalDeclaration, reference: LocalReference): LocalResolution {
        val details = label.details as LocalLabel
        // Control never leaves a subprogram: a label beyond one is out of reach.
        val crossed = chain(model, reference.region).takeWhile { it.id != label.region }
        if (crossed.any { it.kind == LocalRegionKind.SUBPROGRAM }) return LocalResolution.OutOfScope(label.id)
        if (reference.context == Ctx.LOOP_TARGET) {
            if (!details.isLoop) return LocalResolution.WrongKind(label.id)
            return if (contains(details.statement, reference.range)) LocalResolution.Bound(label.id) else LocalResolution.OutOfScope(label.id)
        }
        details.compound?.let { if (!contains(it, reference.range)) return LocalResolution.OutOfScope(label.id) }
        val handler = reference.handler
        if (handler != null && !contains(handler, details.statement) &&
            LocalReferenceCollector.innermostRegion(model, handler) == label.region
        ) return LocalResolution.OutOfScope(label.id)
        return LocalResolution.Bound(label.id)
    }

    private fun contains(outer: SourceRange, inner: SourceRange) =
        (outer.startLine < inner.startLine || outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn) &&
            (inner.endLine < outer.endLine || inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn)

    /**
     * Whether a declaration of [kind] can be what [context] names. WRONG pairs fail at the use on Oracle without falling
     * back to an outer declaration; APPLICABLE pairs compile.
     */
    private fun fit(kind: LocalDeclarationKind, context: Ctx): Fit = when (context) {
        Ctx.QUALIFIER -> Fit.APPLICABLE
        // An attribute is a value in a member method, and captures type names (PLS-00488).
        Ctx.VALUE, Ctx.SQL_VALUE, Ctx.INSERT_VALUE -> when {
            kind in VALUES || kind == ATTRIBUTE -> Fit.APPLICABLE
            kind == EXCEPTION || kind == CURSOR || kind == LABEL || kind in TYPES -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        Ctx.CALL_STATEMENT -> when {
            kind == CURSOR_PARAMETER -> Fit.UNSUPPORTED
            kind in VALUES || kind in TYPES || kind == EXCEPTION || kind == CURSOR || kind == LABEL -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // Element access, a subtype conversion or a constructor: whether the call fits is typing.
        Ctx.CALL_EXPRESSION -> when (kind) {
            VARIABLE, PARAMETER, LOOP_INDEX, SUBTYPE, RECORD_TYPE, COLLECTION_TYPE -> Fit.APPLICABLE
            EXCEPTION, CURSOR, LABEL, REF_CURSOR_TYPE -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        Ctx.TYPE -> when {
            kind in TYPES -> Fit.APPLICABLE
            kind == ATTRIBUTE || kind == VARIABLE || kind == PARAMETER || kind == LOOP_INDEX || kind == LABEL || kind == EXCEPTION || kind == CURSOR -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // A parameter or loop index cannot anchor %TYPE (PLS-00206); a constant can.
        Ctx.ANCHOR_TYPE -> when (kind) {
            VARIABLE -> Fit.APPLICABLE
            PARAMETER, LOOP_INDEX, LABEL, EXCEPTION, CURSOR, SUBTYPE, RECORD_TYPE, COLLECTION_TYPE, REF_CURSOR_TYPE -> Fit.WRONG
            else -> Fit.UNSUPPORTED
        }
        // A variable or parameter anchors %ROWTYPE only when it is a cursor variable: typing.
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
        // A variable or parameter is a cursor variable or not, which is typing.
        Ctx.CURSOR_OPERAND -> when (kind) {
            CURSOR -> Fit.APPLICABLE
            EXCEPTION, LABEL, LOOP_INDEX, SUBTYPE, RECORD_TYPE -> Fit.WRONG // PLS-00456, PLS-00330
            else -> Fit.UNSUPPORTED
        }
        Ctx.OPEN_FOR_OPERAND -> if (kind == CURSOR) Fit.WRONG else Fit.UNSUPPORTED // PLS-00382
        // A cursor, or a value iterated once; a weak cursor variable fails on its type.
        Ctx.FOR_CONTROL -> when (kind) {
            CURSOR, VARIABLE, PARAMETER -> Fit.APPLICABLE
            EXCEPTION, SUBTYPE -> Fit.WRONG // PLS-00320, PLS-00330
            else -> Fit.UNSUPPORTED
        }
        Ctx.SQL_CALL -> Fit.UNSUPPORTED // variables and parameters are decided before; other kinds are not observed
        // Labels are decided by the label-target rule; any other kind there is not observed.
        Ctx.GOTO_TARGET, Ctx.LOOP_TARGET -> Fit.UNSUPPORTED
    }

    /** In SQL a column of the same name wins over a local: a single local is only one of two readings. */
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

    /**
     * Candidates grouped into entities, each sorted by id, in order of their first member; entities holding one of
     * [definitions] come first, in that order.
     */
    private fun entities(candidates: List<LocalDeclaration>, definitions: List<LocalDeclarationId>): List<List<LocalDeclaration>> {
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
        val definition = cursors.singleOrNull { !(it.details as LocalCursor).isForward }
        if (definition != null) cursors.forEach { union(it.id, definition.id) }
        val grouped = candidates.groupBy { find(it.id) }.values.map { members -> members.sortedBy { it.id.ordinal } }
        if (definitions.isEmpty()) return grouped.sortedBy { it.first().id.ordinal }
        fun rank(entity: List<LocalDeclaration>) = entity.minOf { member -> definitions.indexOf(member.id).takeIf { it >= 0 } ?: definitions.size }
        return grouped.sortedWith(compareBy({ rank(it) }, { it.first().id.ordinal }))
    }

    private fun isCallable(entity: List<LocalDeclaration>) = entity.all { it.kind in CALLABLES }

    /** Two subprogram bodies with one heading are not overloads but one subprogram defined twice (PLS-00305). */
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
                member.region in scope.earlierRegions || member.id in scope.definitions -> true
                encloses(member, chain) -> true
                hasDefinition && (member.details as? LocalCursor)?.isForward == true -> false
                else -> endsBefore(member.range, reference.range)
            }
        }
    }

    /** Whether [declaration] is a subprogram, unit or cursor whose region is on [chain]: its name is visible inside it. */
    private fun encloses(declaration: LocalDeclaration, chain: List<LocalRegion>): Boolean = when (val details = declaration.details) {
        is LocalSubprogram -> details.body
        is LocalUnit -> details.region
        is LocalCursor -> details.parameters
        else -> null
    }.let { region -> region != null && chain.any { it.id == region } }

    private fun endsBefore(declaration: SourceRange, reference: SourceRange) =
        declaration.endLine < reference.startLine ||
            declaration.endLine == reference.startLine && declaration.endColumn <= reference.startColumn
}
