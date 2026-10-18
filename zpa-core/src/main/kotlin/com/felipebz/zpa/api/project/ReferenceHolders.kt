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
        val matches = ArrayList<Pair<IndexedDeclaration, Retained>>()
        for (occurrence in index.occurrencesFor(FileId(fileId))) {
            for (retained in retained(occurrence.declaration)) {
                if (matches(retained.reference, reference)) matches += occurrence to retained
            }
        }
        val match = matches.singleOrNull() ?: return null
        return holder(index, match.first, match.second.capturing)
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

    private fun retained(fact: ProjectDeclaration): List<Retained> = when (fact) {
        is PackageValueDeclaration -> listOf(Retained(fact.typeRef, emptyList()))
        is PackageSubtypeDeclaration -> listOf(Retained(fact.baseType, emptyList()))
        is PackageTypeDeclaration -> listOfNotNull(fact.elementType?.let { Retained(it, emptyList()) }) +
            fact.recordFields.map { Retained(it.typeRef, fact.recordFields.map { sibling -> sibling.name }) }
        is StandaloneTypeDeclaration -> listOfNotNull(fact.elementType?.let { Retained(it, emptyList()) })
        is PackageProcedureDeclaration -> parameters(fact.parameters)
        is PackageFunctionDeclaration -> parameters(fact.parameters) + Retained(fact.returnType, fact.parameters.map { it.name })
        is StandaloneProcedureDeclaration -> parameters(fact.parameters)
        is StandaloneFunctionDeclaration -> parameters(fact.parameters) + Retained(fact.returnType, fact.parameters.map { it.name })
        is PackageCursorDeclaration -> parameters(fact.parameters) +
            listOfNotNull(fact.returnType?.let { Retained(it, fact.parameters.map { p -> p.name }) })
        else -> emptyList()
    }

    private fun parameters(parameters: List<ProjectParameter>) =
        parameters.map { Retained(it.typeRef, parameters.map { sibling -> sibling.name }) }

    private fun matches(fact: TypeRef, reference: ProjectTypeReference): Boolean {
        if (fact.sourceRange.toPublic() != reference.sourceRange || fact.name != reference.name.name) return false
        return when (fact) {
            is NamedTypeRef -> reference.kind == ProjectTypeReference.Kind.NAMED
            is RefTypeRef -> reference.kind == ProjectTypeReference.Kind.REF
            is AnchoredTypeRef -> reference.kind == ProjectTypeReference.Kind.ANCHORED && reference.anchor ==
                java.util.Optional.of(if (fact.anchor == TypeAnchor.TYPE) ProjectTypeReference.Anchor.TYPE else ProjectTypeReference.Anchor.ROWTYPE) &&
                fact.link == reference.link.map { it.name }.orElse(null)
        }
    }
}
