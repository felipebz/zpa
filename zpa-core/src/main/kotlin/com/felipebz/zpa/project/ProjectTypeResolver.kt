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
package com.felipebz.zpa.project

/**
 * Resolves only named references against the frozen project declaration index. This result
 * intentionally remains in the project model and is not adapted to PlSqlDatatype yet.
 */
class ProjectTypeResolver(
    private val projectAnalysisContext: ProjectAnalysisContext
) {
    fun resolve(
        reference: NamedTypeRef,
        lookupContext: ProjectTypeLookupContext = ProjectTypeLookupContext()
    ): ProjectTypeResolution = when (val state = projectAnalysisContext.state) {
        ProjectAnalysisContext.State.NotPrepared -> ProjectTypeResolution.NotPrepared(reference)
        is ProjectAnalysisContext.State.Prepared -> {
            val index = state.result.index
            val owner = ProjectTypeNameLookup.owner(reference.name, lookupContext.packageOwner)
            val candidates = index.findTypes(owner, reference.name.last).ifEmpty {
                if (reference.name.segments.size == 1 && owner != null && packageDoesNotDeclare(index, owner, reference)) {
                    index.findTypes(null, reference.name.last)
                } else {
                    emptyList()
                }
            }
            if (state.result.failures.isNotEmpty()) {
                ProjectTypeResolution.IncompleteIndex(reference, candidates, state.result.failures)
            } else {
                when (candidates.size) {
                    0 -> ProjectTypeResolution.NotFoundInProject(reference)
                    1 -> ProjectTypeResolution.Resolved(reference, candidates.single())
                    else -> ProjectTypeResolution.Ambiguous(reference, candidates)
                }
            }
        }
    }

    /**
     * A one-segment name inside a package reaches a schema type only when the package declares nothing of that name:
     * the project holds one specification of the package and its member names are complete without it. Declarations of
     * the package body are the local side's.
     */
    private fun packageDoesNotDeclare(index: ProjectSymbolIndex, owner: QualifiedName, reference: NamedTypeRef): Boolean {
        val specification = index.findOccurrences(owner).singleOrNull {
            (it.declaration as? PackageDeclaration)?.role == DeclarationRole.SPECIFICATION
        } ?: return false
        val range = specification.declaration.sourceRange
        val inside = reference.sourceRange.takeIf { it.fileId == range.fileId && contains(range, it) }
        return PackageMemberNames.at(index, specification, reference.name.last, inside).status == MemberNameStatus.NOT_DECLARED
    }

    private fun contains(outer: SourceRange, inner: SourceRange): Boolean =
        (outer.startLine < inner.startLine || (outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn)) &&
            (inner.endLine < outer.endLine || (inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn))
}

/** Name/context rules shared by the reference-based resolver and the public name-based query. */
internal object ProjectTypeNameLookup {
    /** One-segment names use only the explicit package context; qualified names use their own prefix. */
    fun owner(name: QualifiedName, packageOwner: QualifiedName?): QualifiedName? =
        if (name.segments.size == 1) {
            packageOwner
        } else {
            name.segments.dropLast(1).takeIf { it.isNotEmpty() }?.let(::QualifiedName)
        }
}

/** Context that can be derived from the current AST without guessing database namespaces. */
data class ProjectTypeLookupContext(
    val packageOwner: QualifiedName? = null
)

sealed interface ProjectTypeResolution {
    val reference: NamedTypeRef

    data class Resolved(
        override val reference: NamedTypeRef,
        val declaration: ProjectTypeDeclaration
    ) : ProjectTypeResolution

    class Ambiguous(
        override val reference: NamedTypeRef,
        candidates: Collection<ProjectTypeDeclaration>
    ) : ProjectTypeResolution {
        val candidates: List<ProjectTypeDeclaration> = immutableList(candidates)

        init {
            require(candidates.size > 1) { "Ambiguous resolution requires multiple candidates" }
        }

        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            reference == other.reference && candidates == other.candidates

        override fun hashCode(): Int = 31 * reference.hashCode() + candidates.hashCode()

        override fun toString(): String = "Ambiguous($reference, $candidates)"
    }

    data class NotFoundInProject(
        override val reference: NamedTypeRef
    ) : ProjectTypeResolution

    class IncompleteIndex(
        override val reference: NamedTypeRef,
        knownCandidates: Collection<ProjectTypeDeclaration>,
        failures: Collection<ProjectIndexPreparationFailure>
    ) : ProjectTypeResolution {
        val knownCandidates: List<ProjectTypeDeclaration> = immutableList(knownCandidates)
        val failures: List<ProjectIndexPreparationFailure> = immutableList(failures)

        init {
            require(failures.isNotEmpty()) { "An incomplete result requires preparation failures" }
        }

        override fun equals(other: Any?): Boolean = other is IncompleteIndex &&
            reference == other.reference && knownCandidates == other.knownCandidates && failures == other.failures

        override fun hashCode(): Int = 31 * (31 * reference.hashCode() + knownCandidates.hashCode()) + failures.hashCode()

        override fun toString(): String = "IncompleteIndex($reference, $knownCandidates, $failures)"
    }

    data class NotPrepared(
        override val reference: NamedTypeRef
    ) : ProjectTypeResolution
}
