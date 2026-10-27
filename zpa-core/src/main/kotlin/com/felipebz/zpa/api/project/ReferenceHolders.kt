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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
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
        val fileId = reference.sourceRange.fileId
        if (fileId.isBlank()) return null
        var match: IndexedDeclaration? = null
        var position = -1
        var count = 0
        for (occurrence in index.occurrencesFor(FileId(fileId))) {
            var at = 0
            forEachRetained(occurrence.declaration) { typeRef, _ ->
                if (matches(typeRef, reference)) {
                    count++
                    match = occurrence
                    position = at
                }
                at++
            }
        }
        val found = match?.takeIf { count == 1 } ?: return null
        return holder(index, found, retained(found.declaration)[position].capturing)
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
            is PackageFunctionDeclaration -> {
                fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
                action(fact.returnType, Capture.PARAMETERS)
            }
            is StandaloneProcedureDeclaration -> fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
            is StandaloneFunctionDeclaration -> {
                fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
                action(fact.returnType, Capture.PARAMETERS)
            }
            is PackageCursorDeclaration -> {
                fact.parameters.forEachByIndex { action(it.typeRef, Capture.PARAMETERS) }
                fact.returnType?.let { action(it, Capture.PARAMETERS) }
            }
            else -> Unit
        }
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
