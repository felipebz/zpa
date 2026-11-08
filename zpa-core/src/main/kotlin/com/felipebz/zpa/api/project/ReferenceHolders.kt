//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ReferenceHolders.forEachRetained
import com.felipebz.zpa.project.*

/**
 * The declaration that retains a reference, with the lexical facts needed to apply package scope rules to an anchor:
 * the package owner (null for standalone declarations), the role, the names of siblings that may capture the first
 * segment (record fields, parameters) and the package specification unit that contains it.
 */
internal class ReferenceHolder(
    val fact: ProjectDeclaration,
    val owner: QualifiedName?,
    val role: DeclarationRole,
    val capturingNames: List<OracleIdentifier>,
    val unit: IndexedDeclaration?
)

/** Locates reference holders among the indexed facts of one file; never guesses when the evidence is not exact. */
@OptIn(ZpaExperimentalApi::class)
internal object ReferenceHolders {
    private class Retained(val reference: TypeRef, val capturing: List<OracleIdentifier>)

    /** The holder of [reference], only when exactly one retained reference of the file equals it in every attribute. */
    @JvmSynthetic
    fun find(index: ProjectSymbolIndex, reference: ProjectTypeReference): ReferenceHolder? {
        val range = reference.sourceRange
        if (range.fileId.isBlank()) return null
        val key = SourceRange(FileId(range.fileId), range.startLine, range.startColumn, range.endLine, range.endColumn)
        // Only a retained reference of the same range can match: the file's references are grouped by range once.
        val sameRange = index.retainedTypeRefs(key.fileId, ::byRange)[key] ?: return null
        var match: Pair<IndexedDeclaration, Int>? = null
        var count = 0
        for (candidate in sameRange) {
            if (matches(retainedAt(candidate.first.declaration, candidate.second).first, reference)) {
                count++
                match = candidate
            }
        }
        val (found, position) = match?.takeIf { count == 1 } ?: return null
        return holder(index, found, capturing(found.declaration, retainedAt(found.declaration, position).second))
    }

    /** The type reference [fact] retains at [position] (as [forEachRetained] orders them), with what may capture it. */
    private fun retainedAt(fact: ProjectDeclaration, position: Int): Pair<TypeRef, Capture> {
        var at = 0
        var found: Pair<TypeRef, Capture>? = null
        forEachRetained(fact) { typeRef, capture ->
            if (at == position) found = typeRef to capture
            at++
        }
        return checkNotNull(found) { "No retained reference at $position" }
    }

    /** Every type reference [occurrences] retain, as (occurrence, position), by source range, in occurrence order. */
    private fun byRange(occurrences: List<IndexedDeclaration>): Map<SourceRange, List<Pair<IndexedDeclaration, Int>>> {
        val result = HashMap<SourceRange, MutableList<Pair<IndexedDeclaration, Int>>>()
        for (occurrence in occurrences) {
            var at = 0
            forEachRetained(occurrence.declaration) { typeRef, _ ->
                result.getOrPut(typeRef.sourceRange) { ArrayList(1) } += occurrence to at
                at++
            }
        }
        return result
    }

    /** The holder of the declared type (or base) of an already identified value or subtype occurrence. */
    @JvmSynthetic
    fun of(index: ProjectSymbolIndex, occurrence: IndexedDeclaration): ReferenceHolder? {
        val retained = retained(occurrence.declaration).singleOrNull() ?: return null
        return holder(index, occurrence, retained.capturing)
    }

    private fun holder(index: ProjectSymbolIndex, occurrence: IndexedDeclaration, capturing: List<OracleIdentifier>): ReferenceHolder {
        val fact = occurrence.declaration
        val owner = ownerOf(fact)
        val unit = owner?.let { unitOf(index, fact, it) }
        return ReferenceHolder(fact, owner, fact.role, capturing, unit)
    }

    /** The package that declares [fact], or null for standalone declarations and packages themselves. */
    @JvmSynthetic
    fun ownerOf(fact: ProjectDeclaration): QualifiedName? = when (fact) {
        is PackageValueDeclaration -> fact.owner
        is PackageSubtypeDeclaration -> fact.owner
        is PackageTypeDeclaration -> fact.owner
        is PackageProcedureDeclaration -> fact.owner
        is PackageFunctionDeclaration -> fact.owner
        is PackageExceptionDeclaration -> fact.owner
        is PackageCursorDeclaration -> fact.owner
        else -> null
    }

    /**
     * The one package specification fact (of the same file and owner) whose range contains [fact]. A package fact is
     * indexed under its own qualified name, so only the occurrences of [owner] are examined.
     */
    @JvmSynthetic
    fun unitOf(index: ProjectSymbolIndex, fact: ProjectDeclaration, owner: QualifiedName): IndexedDeclaration? =
        index.findOccurrences(owner).singleOrNull {
            val unit = it.declaration
            unit is PackageDeclaration && unit.role == DeclarationRole.SPECIFICATION && unit.fileId == fact.fileId &&
                contains(unit.sourceRange, fact.sourceRange)
        }

    private fun contains(outer: SourceRange, inner: SourceRange): Boolean =
        (outer.startLine < inner.startLine || (outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn)) &&
            (inner.endLine < outer.endLine || (inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn))

    /** Visits a list by index: the lists are unmodifiable views, whose iterators would be allocated on every scan. */
    private inline fun <T> List<T>.forEachByIndex(action: (T) -> Unit) {
        for (i in indices) action(this[i])
    }

    /** The names that may capture the first segment of a retained reference. */
    private enum class Capture { NONE, FIELDS, PARAMETERS }

    /** Each type reference [fact] retains, in order, with the names that may capture its first segment. */
    private inline fun forEachRetained(fact: ProjectDeclaration, action: (TypeRef, Capture) -> Unit) {
        when (fact) {
            is PackageValueDeclaration -> action(fact.typeRef, Capture.NONE)
            is PackageSubtypeDeclaration -> action(fact.baseType, Capture.NONE)
            is PackageTypeDeclaration -> {
                fact.elementType?.let { action(it, Capture.NONE) }
                fact.recordFields.forEachByIndex { action(it.typeRef, Capture.FIELDS) }
            }
            is StandaloneTypeDeclaration -> fact.elementType?.let { action(it, Capture.NONE) }
            is PackageProcedureDeclaration -> fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
            is PackageFunctionDeclaration -> forEachInFunction(fact.parameters, fact.returnType, action)
            is StandaloneProcedureDeclaration -> fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
            is StandaloneFunctionDeclaration -> forEachInFunction(fact.parameters, fact.returnType, action)
            is PackageCursorDeclaration -> {
                fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
                fact.returnType?.let { action(it, Capture.PARAMETERS) }
            }
            else -> Unit
        }
    }

    /** A function's parameter types, then its return type; the parameters may capture each of them. */
    private inline fun forEachInFunction(parameters: List<ProjectParameter>, returnType: TypeRef, action: (TypeRef, Capture) -> Unit) {
        parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
        action(returnType, Capture.PARAMETERS)
    }

    private fun retained(fact: ProjectDeclaration): List<Retained> {
        val result = ArrayList<Retained>()
        forEachRetained(fact) { typeRef, capture -> result += Retained(typeRef, capturing(fact, capture)) }
        return result
    }

    private fun capturing(fact: ProjectDeclaration, capture: Capture): List<OracleIdentifier> = when (capture) {
        Capture.NONE -> emptyList()
        Capture.FIELDS -> (fact as PackageTypeDeclaration).recordFields.map { it.name }
        Capture.PARAMETERS -> when (fact) {
            is PackageProcedureDeclaration -> fact.parameters.map { it.name }
            is PackageFunctionDeclaration -> fact.parameters.map { it.name }
            is StandaloneProcedureDeclaration -> fact.parameters.map { it.name }
            is StandaloneFunctionDeclaration -> fact.parameters.map { it.name }
            is PackageCursorDeclaration -> fact.parameters.map { it.name }
            else -> emptyList()
        }
    }

    /** Whether [range] equals [public] in every field, as its public form would, without building that form. */
    private fun sameRange(range: SourceRange, public: ProjectSourceRange): Boolean =
        range.startLine == public.startLine && range.startColumn == public.startColumn && range.endLine == public.endLine &&
            range.endColumn == public.endColumn && range.fileId.value == public.fileId

    private fun matches(fact: TypeRef, reference: ProjectTypeReference): Boolean {
        if (!sameRange(fact.sourceRange, reference.sourceRange) || fact.name != reference.name.name) return false
        return when (fact) {
            is NamedTypeRef -> reference.kind == ProjectTypeReference.Kind.NAMED
            is RefTypeRef -> reference.kind == ProjectTypeReference.Kind.REF
            is AnchoredTypeRef -> reference.kind == ProjectTypeReference.Kind.ANCHORED && reference.anchor ==
                java.util.Optional.of(if (fact.anchor == TypeAnchor.TYPE) ProjectTypeReference.Anchor.TYPE else ProjectTypeReference.Anchor.ROWTYPE) &&
                fact.link == reference.link.map { it.name }.orElse(null)
        }
    }
}
