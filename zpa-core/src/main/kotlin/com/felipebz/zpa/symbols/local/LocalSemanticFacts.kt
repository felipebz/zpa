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

import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange

/**
 * The local semantic resolution of one file, computed by the pipeline's pre-pass and consumed by nothing yet: the
 * model, the references with their segment-by-segment outcomes, the calls, and the comparison with the decisions the
 * legacy lexical lookup makes for the same names. AST-detached.
 */
internal class LocalSemanticFacts(
    val model: LocalSemanticModel,
    val references: List<LocalReference>,
    /**
     * For each reference, in the same order, its outcomes as [LocalResolver.resolveQualified] gives them, refined with
     * the project's specification coverage ([LocalSpecificationCoverage]) when the project is prepared.
     */
    val resolutions: List<List<LocalResolution>>,
    val calls: List<LocalCallAnalysis>,
    /**
     * The source ranges, in this file, of the declarations the project index holds (package specification members and
     * standalone units), as plain coordinates: a local binding to one of them is also a project declaration.
     */
    private val projectHeld: Set<LocalRangeKey> = emptySet()
) {
    /** Type names where the project resolved another declaration than the one the local side binds, as `line:column`. */
    val gateMismatches = ArrayList<String>()

    /** `SELF` members where the project result disagrees with the local member step, as `line:column`. */
    val selfMismatches = ArrayList<String>()

    /** Method calls where the project binding disagrees with the local call resolution, as `line:column`. */
    val methodCallMismatches = ArrayList<String>()

    /** The local resolution of the call whose name starts at line, column; null when the local side has none. */
    fun localCall(line: Int, column: Int): LocalCallResolution? =
        calls.singleOrNull { it.reference.range.startLine == line && it.reference.range.startColumn == column }?.local

    /** The range of a local declaration, or of its specification heading when a body subprogram is linked to one. */
    fun declarationRange(declaration: LocalDeclarationId): LocalRangeKey {
        val specification = (model.declaration(declaration).details as? LocalSubprogram)?.specificationCounterpart ?: declaration
        return LocalRangeKey.of(model.declaration(specification).range)
    }

    /** The first segment's local outcome of the name starting at line, column, when a single non-type reference starts there. */
    fun firstOutcome(line: Int, column: Int): LocalResolution? =
        byStart[Triple(false, line, column)]?.singleOrNull()?.let { resolutions[it].first() }

    /** Whether [outcome] binds the name to a schema unit declared in this file at [range]. */
    fun bindsUnitAt(outcome: LocalResolution, range: LocalRangeKey): Boolean {
        val bound = outcome as? LocalResolution.Bound ?: return false
        val declaration = model.declaration(bound.declaration)
        return declaration.details is LocalUnit && LocalRangeKey.of(declaration.range) == range
    }

    private val typeGates = ArrayList<LegacyGate>()
    private val firstSegments = ArrayList<LegacyGate>()

    /** The legacy type-name gate's decision for the type name starting at [range]: true when it found the name lexically. */
    fun recordTypeGate(range: SourceRange, lexical: Boolean) {
        typeGates += LegacyGate(range.startLine, range.startColumn, lexical)
    }

    /** The legacy lookup's hit for the first segment of a call or qualified name starting at line, column. */
    fun recordFirstSegment(line: Int, column: Int, lexical: Boolean) {
        firstSegments += LegacyGate(line, column, lexical)
    }

    private val byStart: Map<Triple<Boolean, Int, Int>, List<Int>> by lazy {
        val map = HashMap<Triple<Boolean, Int, Int>, MutableList<Int>>()
        for ((index, reference) in references.withIndex()) {
            map.getOrPut(Triple(isTypeName(reference), reference.range.startLine, reference.range.startColumn)) { ArrayList(1) } += index
        }
        map
    }

    /** The local outcome deciding the name of [kind] starting at line, column; null when no single reference starts there. */
    private fun localOutcome(kind: LocalShadowPair.Kind, line: Int, column: Int): LocalResolution? =
        byStart[Triple(kind == LocalShadowPair.Kind.TYPE_NAME, line, column)]?.singleOrNull()
            ?.let { decidingOutcome(references[it], resolutions[it]) }

    /**
     * The type-name gate's decision taken from the local outcome, paired as [differential] pairs it:
     * - [LocalTypeGate.Local]: a local declaration the project does not hold decides the name (no project resolution);
     * - [LocalTypeGate.Held]: the local binding is a declaration the project index also holds; project resolution runs,
     *   and must resolve to that declaration;
     * - [LocalTypeGate.NotLocal]: absent, absent with the project's coverage, or a member of the specification in another
     *   file;
     * - null when the local outcome is uncertain or no reference is paired: the legacy decision stands.
     */
    fun localTypeGate(range: SourceRange): LocalTypeGate? {
        val index = byStart[Triple(true, range.startLine, range.startColumn)]?.singleOrNull() ?: return null
        val outcome = decidingOutcome(references[index], resolutions[index])
        return when (LocalShadowPair.classify(false, outcome)) {
            LocalShadowPair.ShadowClass.SAME -> enclosingType(references[index])?.let(LocalTypeGate::EnclosingType) ?: LocalTypeGate.NotLocal
            LocalShadowPair.ShadowClass.LEGACY_ALLOWS -> {
                val bound = ((outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome) as? LocalResolution.Bound
                val key = bound?.let { LocalRangeKey.of(model.declaration(it.declaration).range) }
                if (key != null && key in projectHeld) LocalTypeGate.Held(key) else LocalTypeGate.Local
            }
            else -> null
        }
    }

    /**
     * The implicit `SELF` of the qualified name whose first segment starts at line, column, when that outcome is certain:
     * the enclosing type's written name and what the local side says of the first member.
     */
    fun implicitSelf(line: Int, column: Int): LocalSelfBase? {
        val index = byStart[Triple(false, line, column)]?.singleOrNull() ?: return null
        val steps = resolutions[index]
        val self = steps.first() as? LocalResolution.ImplicitSelf ?: return null
        val body = (model.declaration(self.method).details as LocalSubprogram).body ?: return null
        val type = model.parent(body)?.let { model.index.unit(it.id) } ?: return null
        val member = when (val step = steps.getOrNull(1)) {
            is LocalResolution.Bound -> model.declaration(step.declaration).takeIf { it.kind == LocalDeclarationKind.ATTRIBUTE }
                ?.let { LocalSelfMember.Attribute(LocalRangeKey.of(it.range)) } ?: LocalSelfMember.Other
            LocalResolution.Undeclared -> LocalSelfMember.Undeclared
            else -> LocalSelfMember.Other
        }
        return LocalSelfBase(type.unitName, member)
    }

    private fun enclosingType(reference: LocalReference): QualifiedName? =
        LocalTypeScope.enclosingTypeName(model, reference)
            ?.takeIf { isTypeName(reference) && reference.segments.isEmpty() && it.last == reference.name }

    /** Each recorded legacy decision paired with the local outcome for the same name, in recording order. */
    fun differential(): List<LocalShadowPair> {
        fun pair(kind: LocalShadowPair.Kind, gate: LegacyGate): LocalShadowPair {
            val outcome = localOutcome(kind, gate.line, gate.column)
            return LocalShadowPair(kind, gate.line, gate.column, gate.lexical, outcome, LocalShadowPair.classify(gate.lexical, outcome))
        }
        return typeGates.map { pair(LocalShadowPair.Kind.TYPE_NAME, it) } + firstSegments.map { pair(LocalShadowPair.Kind.FIRST_SEGMENT, it) }
    }

    /** A type name is decided by its whole name; a call or qualified name by its first segment. */
    private fun decidingOutcome(reference: LocalReference, steps: List<LocalResolution>): LocalResolution =
        if (isTypeName(reference) && !notLocal(steps.first())) steps.last() else steps.first()

    private fun notLocal(outcome: LocalResolution) =
        LocalReferenceOutcomes.isAbsent(outcome) || LocalReferenceOutcomes.isSpecificationMember(outcome)

    private class LegacyGate(val line: Int, val column: Int, val lexical: Boolean)
}

/** A source range as plain coordinates, detached from any file object. */
internal data class LocalRangeKey(val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int) {
    companion object {
        fun of(range: SourceRange) = LocalRangeKey(range.startLine, range.startColumn, range.endLine, range.endColumn)
    }
}

internal class LocalSelfBase(val typeName: QualifiedName, val member: LocalSelfMember)

/** The local outcome of the first member of `SELF`. */
internal sealed interface LocalSelfMember {
    /** An attribute declared in this file. */
    data class Attribute(val declaration: LocalRangeKey) : LocalSelfMember

    /** Undeclared, with no lookup gap. */
    data object Undeclared : LocalSelfMember

    data object Other : LocalSelfMember
}

/** The type-name gate's decision from the local outcome. */
internal sealed interface LocalTypeGate {
    data object Local : LocalTypeGate
    data object NotLocal : LocalTypeGate

    /** Not local: the enclosing object type's own name, which the project must resolve to that type. */
    data class EnclosingType(val name: QualifiedName) : LocalTypeGate
    data class Held(val declaration: LocalRangeKey) : LocalTypeGate
}

/** A written type name, one segment or qualified (whose first segment is a qualifier of a type position). */
internal fun isTypeName(reference: LocalReference) = reference.lastSegmentContext == LocalReferenceContext.TYPE

/** One legacy lexical decision and the local outcome for the same name. */
internal class LocalShadowPair(
    val kind: Kind,
    val line: Int,
    val column: Int,
    /** The legacy lookup found the name (its project resolution is suppressed). */
    val legacyLexical: Boolean,
    /** Null when no local reference starts there. */
    val local: LocalResolution?,
    val shadowClass: ShadowClass
) {
    enum class Kind {
        /** The project type resolution's lexical gate. */
        TYPE_NAME,

        /** The legacy symbol of the first segment of a call or a qualified name. */
        FIRST_SEGMENT
    }

    enum class ShadowClass {
        /** Both find the name locally, or both prove it absent. */
        SAME,

        /** Legacy finds the name; the local resolver proves it absent, so project evidence is suppressed today. */
        LEGACY_SUPPRESSES,

        /** Legacy finds nothing; the local resolver finds a local declaration, so project evidence is let through today. */
        LEGACY_ALLOWS,

        /** The local outcome is incomplete, unsupported or a SQL uncertainty. */
        LOCAL_UNCERTAIN,

        /** No local reference at that position. */
        UNPAIRED
    }

    override fun toString() = "$kind $line:$column legacy=${if (legacyLexical) "lexical" else "absent"} local=$local $shadowClass"

    companion object {
        fun classify(legacyLexical: Boolean, local: LocalResolution?): ShadowClass {
            // Decided with the project's coverage: absent and a specification member are both "not local".
            val decided = (local as? LocalResolution.SpecificationCovered)?.outcome?.takeIf { it != LocalResolution.SpecificationMember }
                ?: if (local is LocalResolution.SpecificationCovered) LocalResolution.ProvenAbsent else local
            val declares = when (decided) {
                null -> return ShadowClass.UNPAIRED
                is LocalResolution.Incomplete, is LocalResolution.Unsupported, is LocalResolution.SqlColumnOrLocal,
                LocalResolution.SqlRelation, is LocalResolution.LocalFunctionInSql, is LocalResolution.SqlBuiltinOrLocal ->
                    return ShadowClass.LOCAL_UNCERTAIN
                LocalResolution.ProvenAbsent -> false
                // A binding, or a local declaration that rejects the use: either way the name is local.
                else -> true
            }
            return when {
                legacyLexical == declares -> ShadowClass.SAME
                legacyLexical -> ShadowClass.LEGACY_SUPPRESSES
                else -> ShadowClass.LEGACY_ALLOWS
            }
        }
    }
}
