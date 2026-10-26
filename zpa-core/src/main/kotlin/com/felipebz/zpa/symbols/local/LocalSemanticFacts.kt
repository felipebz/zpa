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

import com.felipebz.zpa.project.ProjectTypeDeclaration
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange

/**
 * The local semantic resolution of one file, computed by the pipeline's pre-pass: the model, the references with their
 * segment-by-segment outcomes, and the calls. AST-detached.
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
    private val projectHeld: Set<LocalRangeKey> = emptySet(),
    /** Names in an object type body where the project's member lists disagree with the specification in this file. */
    val coverageMismatches: List<String> = emptyList(),
    /**
     * For each variable or exception name declared inside a package body whose specification is in another file, keyed
     * by the body region and the name's lookup form: the specification's variable or exception of that name, or
     * [LocalSpecificationValue.Unknown]. Absent when the project is not prepared.
     */
    val specificationValues: Map<Pair<LocalRegionId, String>, LocalSpecificationValue> = emptyMap(),
    /** See [LocalSpecificationCoverage.sqlSpecificationMembers]. */
    val sqlSpecificationMembers: Map<Pair<Int, Int>, LocalSpecificationMember> = emptyMap(),
    /** The names written as members of a value (after a call, an element access or a parenthesized expression). */
    val members: List<LocalMember> = emptyList()
) {
    /** Type names where the project resolved another declaration than the one the local side binds, as `line:column`. */
    val gateMismatches = ArrayList<String>()

    /** The usage evidence of the file's declarations, computed when first asked. */
    val usages: LocalUsages by lazy { LocalUsages(this) }

    /** Which declaration each of the file's declarations hides, computed when first asked. */
    val hiding: LocalHiding by lazy { LocalHiding(this) }

    /** The exceptions named in `RAISE` statements and handlers, computed when first asked. */
    val exceptions: LocalExceptions by lazy { LocalExceptions(this) }

    /** What the plain names in expressions bind, computed when first asked. */
    val names: LocalNames by lazy { LocalNames(this) }

    /** `SELF` members where the project result disagrees with the local member step, as `line:column`. */
    val selfMismatches = ArrayList<String>()

    /** Method calls where the project binding disagrees with the local call resolution, as `line:column`. */
    val methodCallMismatches = ArrayList<String>()

    /** Collection members where the project result disagrees with the local member step, as `line:column`. */
    val collectionMemberMismatches = ArrayList<String>()

    /**
     * What the local side decides for the first member of the qualified name starting at line, column, when that member
     * is on a local collection type: true for a collection method (or one the collection form rejects as a call), false
     * for an undeclared member, null when the local side does not decide it.
     */
    fun localCollectionMember(line: Int, column: Int): Boolean? {
        val index = byStart[Triple(false, line, column)]?.singleOrNull() ?: return null
        val step = resolutions[index].getOrNull(1) ?: return null
        return when (step) {
            is LocalResolution.CollectionMethod -> true
            is LocalResolution.WrongKind -> (model.declaration(step.declaration).details is LocalCollectionType).takeIf { it }
            LocalResolution.Undeclared -> false
            else -> null
        }
    }

    /** The local resolution of the call whose name starts at line, column; null when the local side has none. */
    fun localCall(line: Int, column: Int): LocalCallResolution? =
        calls.singleOrNull { it.reference.range.startLine == line && it.reference.range.startColumn == column }?.local

    /** The range of a local declaration, or of its specification heading when a body subprogram is linked to one. */
    fun declarationRange(declaration: LocalDeclarationId): LocalRangeKey {
        val specification = (model.declaration(declaration).details as? LocalSubprogram)?.specificationCounterpart ?: declaration
        return LocalRangeKey.of(model.declaration(specification).range)
    }

    /**
     * The declaration [outcome] names and whether that is certain: a binding or an erroneous use of one declaration is
     * certain; a provisional outcome counts only with exactly one candidate. Null for any other outcome. With several
     * overloads, the one the call resolution selects when [whole] is the name of the call.
     */
    fun declarationOf(reference: LocalReference, outcome: LocalResolution, whole: Boolean): Pair<LocalDeclarationId, Boolean>? =
        when (outcome) {
            is LocalResolution.Bound -> outcome.declaration to true
            is LocalResolution.WrongKind -> outcome.declaration to true
            is LocalResolution.DeclaredLater -> outcome.declaration to true
            is LocalResolution.OutOfScope -> outcome.declaration to true
            is LocalResolution.InstanceMemberInStatic -> outcome.declaration to true
            is LocalResolution.OverloadSet -> overload(reference, outcome.declarations, whole)?.let { it to true }
            is LocalResolution.LocalFunctionInSql -> overload(reference, outcome.declarations, whole)?.let { it to true }
            is LocalResolution.SqlColumnOrLocal -> outcome.declaration to false
            is LocalResolution.SqlBuiltinOrLocal -> outcome.declarations.singleOrNull()?.let { it to false }
            is LocalResolution.SpecificationCovered -> declarationOf(reference, outcome.outcome, whole)
            is LocalResolution.Incomplete -> declarationOf(reference, outcome.provisional, whole)?.let { it.first to false }
            is LocalResolution.Unsupported -> outcome.candidate?.let { it to false }
            else -> null
        }

    private fun overload(reference: LocalReference, declarations: List<LocalDeclarationId>, whole: Boolean): LocalDeclarationId? {
        declarations.singleOrNull()?.let { return it }
        if (!whole) return null
        val call = localCall(reference.range.startLine, reference.range.startColumn) ?: return null
        return call.selected.takeIf { call.status == LocalCallResolution.Status.RESOLVED }
    }

    /** The single non-type reference starting at line, column, with its outcomes; null when none or several start there. */
    private fun referenceAt(line: Int, column: Int): Int? = byStart[Triple(false, line, column)]?.singleOrNull()

    /**
     * The declaration the first segment of the name starting at line, column names, by [declarationOf]; null when no
     * single reference starts there or its outcome names no one declaration.
     */
    fun nameDeclaration(line: Int, column: Int): LocalDeclaration? {
        val index = referenceAt(line, column) ?: return null
        val reference = references[index]
        return declarationOf(reference, resolutions[index].first(), reference.segments.isEmpty())?.first?.let(model::declaration)
    }

    /**
     * The declaration the whole qualified name starting at line, column binds as a local name (`proc.v`, `label.v`,
     * `pkg.v` with the package in this file), by [declarationOf] on each segment: every segment before the last names a
     * label, subprogram or package. Null when it is not one: a member of a value (`v.f`, `self.a`), an
     * attribute, or a segment naming no one declaration.
     */
    fun qualifiedDeclaration(line: Int, column: Int): LocalDeclaration? {
        val index = referenceAt(line, column) ?: return null
        val reference = references[index]
        val steps = resolutions[index]
        if (reference.segments.isEmpty() || steps.size != reference.segments.size + 1 || reference.segments.any { it.isAttribute }) return null
        for (step in 0 until steps.size - 1) {
            val qualifier = declarationOf(reference, steps[step], false)?.first?.let(model::declaration) ?: return null
            if (qualifier.kind !in QUALIFIER_KINDS) return null
        }
        return declarationOf(reference, steps.last(), true)?.first?.let(model::declaration)
    }

    private val declaredTypes = HashMap<LocalDeclarationId, ProjectTypeDeclaration>()

    /** The local declaration whose own written datatype (or function result) is at a range. */
    private val typeOwners: Map<LocalRangeKey, LocalDeclarationId> by lazy {
        val owners = HashMap<LocalRangeKey, LocalDeclarationId>()
        for (declaration in model.declarations) {
            declaration.type?.let { owners.putIfAbsent(LocalRangeKey.of(it.range), declaration.id) }
            (declaration.details as? LocalSubprogram)?.returnType?.let { owners.putIfAbsent(LocalRangeKey.of(it.range), declaration.id) }
        }
        owners
    }

    /** Records the project type the datatype at [datatype] resolved to, for the declaration it directly types. */
    fun recordDeclaredType(datatype: SourceRange, type: ProjectTypeDeclaration) {
        typeOwners[LocalRangeKey.of(datatype)]?.let { declaredTypes[it] = type }
    }

    /** The project type [declaration] is directly declared with, when the project resolved it. */
    fun declaredType(declaration: LocalDeclaration): ProjectTypeDeclaration? = declaredTypes[declaration.id]

    /** The first segment's local outcome of the name starting at line, column, when a single non-type reference starts there. */
    fun firstOutcome(line: Int, column: Int): LocalResolution? =
        byStart[Triple(false, line, column)]?.singleOrNull()?.let { resolutions[it].first() }

    /** Whether [outcome] binds the name to a schema unit declared in this file at [range]. */
    fun bindsUnitAt(outcome: LocalResolution, range: LocalRangeKey): Boolean {
        val bound = outcome as? LocalResolution.Bound ?: return false
        val declaration = model.declaration(bound.declaration)
        return declaration.details is LocalUnit && LocalRangeKey.of(declaration.range) == range
    }

    private val byStart: Map<Triple<Boolean, Int, Int>, List<Int>> by lazy {
        val map = HashMap<Triple<Boolean, Int, Int>, MutableList<Int>>()
        for ((index, reference) in references.withIndex()) {
            map.getOrPut(Triple(isTypeName(reference), reference.range.startLine, reference.range.startColumn)) { ArrayList(1) } += index
        }
        map
    }

    /**
     * The type-name gate's decision for the type name starting at [range], from its local outcome only:
     * - [LocalPresence.DECLARED]: a local declaration decides the name. [LocalTypeGate.Held] when it is a binding to a
     *   declaration the project index also holds (project resolution runs and must resolve to it), otherwise
     *   [LocalTypeGate.Local] (no project resolution).
     * - [LocalPresence.UNCERTAIN] with exactly one candidate declaration ([declarationOf]): decided as a binding to that
     *   candidate would be. A provisional [LocalResolution.Duplicate] is decided as the certain one: every candidate is a
     *   local declaration, so [LocalTypeGate.Local].
     * - [LocalPresence.ABSENT], an uncertain outcome without a single candidate, or no single reference at [range]: not
     *   local, so project resolution runs ([notLocal]). The type names the collector does not gather are those of TREAT,
     *   IS OF, table and XMLTABLE columns and external parameter properties: they name SQL types, never a local PL/SQL
     *   type. A member of the specification named in a type position of the
     *   type's own body is the exception: it captures the position (PLS-00488), so it is [LocalTypeGate.Local].
     */
    fun localTypeGate(range: SourceRange): LocalTypeGate {
        // No reference: a written type the collector does not gather names a SQL type, never a local one.
        val index = byStart[Triple(true, range.startLine, range.startColumn)]?.singleOrNull() ?: return LocalTypeGate.NotLocal
        val reference = references[index]
        val outcome = decidingOutcome(reference, resolutions[index])
        return when (LocalPresence.of(outcome)) {
            LocalPresence.ABSENT ->
                if (LocalReferenceOutcomes.isSpecificationMember(outcome) && LocalTypeScope.enclosingTypeName(model, reference) != null) LocalTypeGate.Local
                else notLocal(reference)
            LocalPresence.DECLARED -> declared(((outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome) as? LocalResolution.Bound)
            LocalPresence.UNCERTAIN -> when {
                provisionalDuplicate(outcome) -> LocalTypeGate.Local
                else -> declarationOf(reference, outcome, false)?.let { declared(LocalResolution.Bound(it.first)) } ?: notLocal(reference)
            }
        }
    }

    private fun provisionalDuplicate(outcome: LocalResolution): Boolean =
        (((outcome as? LocalResolution.SpecificationCovered)?.outcome ?: outcome) as? LocalResolution.Incomplete)?.provisional is LocalResolution.Duplicate

    /** A local declaration decides: held by the project when [bound] binds one it holds. */
    private fun declared(bound: LocalResolution.Bound?): LocalTypeGate {
        val key = bound?.let { LocalRangeKey.of(model.declaration(it.declaration).range) }
        return if (key != null && key in projectHeld) LocalTypeGate.Held(key) else LocalTypeGate.Local
    }

    /** Not local: the enclosing object type's own name, or any other name for the project to resolve. */
    private fun notLocal(reference: LocalReference): LocalTypeGate =
        enclosingType(reference)?.let(LocalTypeGate::EnclosingType) ?: LocalTypeGate.NotLocal

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

    /**
     * For a first segment that the project decided is a member of the enclosing object type's specification (named alone,
     * the specification in another file), the type's written name; null otherwise.
     */
    fun enclosingTypeOfSpecificationMember(line: Int, column: Int): QualifiedName? {
        val index = byStart[Triple(false, line, column)]?.singleOrNull() ?: return null
        if (!LocalReferenceOutcomes.isSpecificationMember(resolutions[index].first())) return null
        return LocalTypeScope.enclosingTypeName(model, references[index])
    }

    /** A type name is decided by its whole name; a call or qualified name by its first segment. */
    private fun decidingOutcome(reference: LocalReference, steps: List<LocalResolution>): LocalResolution =
        if (isTypeName(reference) && !notLocal(steps.first())) steps.last() else steps.first()

    private fun notLocal(outcome: LocalResolution) =
        LocalReferenceOutcomes.isAbsent(outcome) || LocalReferenceOutcomes.isSpecificationMember(outcome)
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

/** The declarations that qualify a local name in a qualified reference. */
private val QUALIFIER_KINDS = setOf(
    LocalDeclarationKind.LABEL, LocalDeclarationKind.PROCEDURE, LocalDeclarationKind.FUNCTION,
    LocalDeclarationKind.PACKAGE, LocalDeclarationKind.PACKAGE_BODY
)

/** A written type name, one segment or qualified (whose first segment is a qualifier of a type position). */
internal fun isTypeName(reference: LocalReference) = reference.lastSegmentContext == LocalReferenceContext.TYPE

/** What a local outcome says about whether the name is declared in this file. */
internal enum class LocalPresence {
    /** Proven absent, by the file alone or with the project's coverage, or a member of the specification in another file. */
    ABSENT,

    /** A local declaration decides the name: it binds it, or it rejects the use. */
    DECLARED,

    /** Incomplete, unsupported, or a SQL name that may be a column, a relation path or a built-in. */
    UNCERTAIN;

    companion object {
        fun of(outcome: LocalResolution): LocalPresence {
            // Decided with the project's coverage: absent and a specification member are both absent from this file.
            val decided = (outcome as? LocalResolution.SpecificationCovered)?.outcome?.takeIf { it != LocalResolution.SpecificationMember }
                ?: if (outcome is LocalResolution.SpecificationCovered) LocalResolution.ProvenAbsent else outcome
            return when (decided) {
                is LocalResolution.Incomplete, is LocalResolution.Unsupported, is LocalResolution.SqlColumnOrLocal,
                LocalResolution.SqlRelation, is LocalResolution.LocalFunctionInSql, is LocalResolution.SqlBuiltinOrLocal -> UNCERTAIN
                LocalResolution.ProvenAbsent -> ABSENT
                else -> DECLARED
            }
        }
    }
}
