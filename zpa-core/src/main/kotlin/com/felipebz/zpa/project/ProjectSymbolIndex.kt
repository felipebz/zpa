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

import java.util.concurrent.ConcurrentHashMap

/** Collects immutable per-file facts and freezes them into a deterministic index. */
class ProjectSymbolIndexBuilder {
    private val declarationsByFile = ConcurrentHashMap<FileId, List<ProjectDeclaration>>()

    fun add(fileId: FileId, declarations: Collection<ProjectDeclaration>) {
        require(declarations.all { it.fileId == fileId }) {
            "All declarations added for a file must have the same FileId"
        }
        val copy = immutableList(declarations)
        require(declarationsByFile.putIfAbsent(fileId, copy) == null) {
            "Declarations for $fileId were added more than once"
        }
    }

    fun build(): ProjectSymbolIndex {
        val declarations = declarationsByFile.entries
            .sortedBy { it.key.value }
            .flatMap { it.value }
            .sortedWith(ProjectSymbolIndex.declarationComparator)
        return ProjectSymbolIndex(declarationsByFile.keys.toList().sortedBy { it.value }, declarations)
    }
}

/** Immutable project-wide declaration registry. It does not perform semantic resolution. */
class ProjectSymbolIndex internal constructor(
    fileIds: List<FileId>,
    declarations: List<ProjectDeclaration>
) {
    val fileIds: List<FileId> = immutableList(fileIds)
    val declarations: List<ProjectDeclaration> = immutableList(declarations)

    private val packagesByName = this.declarations
        .filterIsInstance<PackageDeclaration>()
        .groupBy { it.name }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    private val typesByOwnerAndName = this.declarations
        .filterIsInstance<ProjectTypeDeclaration>()
        .groupBy { declaration -> typeKey(declaration) }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    private val subprogramsByOwnerAndName = this.declarations
        .filterIsInstance<PackageSubprogramDeclaration>()
        .groupBy { it.owner to it.name }
        .mapValues { (_, values) -> immutableList(values.sortedWith(declarationComparator)) }

    fun findPackages(name: QualifiedName): List<PackageDeclaration> = packagesByName[name].orEmpty()

    fun findTypes(owner: QualifiedName?, name: OracleIdentifier): List<ProjectTypeDeclaration> =
        typesByOwnerAndName[owner to name].orEmpty()

    fun findSubprograms(owner: QualifiedName, name: OracleIdentifier): List<PackageSubprogramDeclaration> =
        subprogramsByOwnerAndName[owner to name].orEmpty().filterIsInstance<PackageSubprogramDeclaration>()

    fun findDeclarations(name: QualifiedName): List<ProjectDeclaration> = declarations.filter { declaration ->
        when (declaration) {
            is PackageDeclaration -> declaration.name == name
            is StandaloneTypeDeclaration -> declaration.name == name
            is PackageTypeDeclaration -> declaration.owner.append(declaration.name) == name
            is PackageSubtypeDeclaration -> declaration.owner.append(declaration.name) == name
            is PackageSubprogramDeclaration -> declaration.owner.append(declaration.name) == name
        }
    }

    companion object {
        internal val declarationComparator = compareBy<ProjectDeclaration>(
            { it.fileId.value },
            { it.sourceRange.startLine },
            { it.sourceRange.startColumn },
            { it.kind.ordinal },
            { declarationOrderingKey(it) }
        )

        internal fun empty() = ProjectSymbolIndex(emptyList(), emptyList())

        private fun typeKey(declaration: ProjectTypeDeclaration): Pair<QualifiedName?, OracleIdentifier> =
            declaration.qualifiedName.segments.dropLast(1).takeIf { it.isNotEmpty() }?.let(::QualifiedName) to
                declaration.qualifiedName.last

        /** Semantic identity. It deliberately excludes comparison-only metadata. */
        private fun declarationIdentityKey(declaration: ProjectDeclaration): String = when (declaration) {
            is PackageDeclaration -> declaration.name.lookupKey()
            is StandaloneTypeDeclaration -> declaration.name.lookupKey()
            is PackageTypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}"
            is PackageSubtypeDeclaration -> "${declaration.owner.lookupKey()}.${declaration.name.lookupName}:${declaration.baseType.structuralKey()}"
            is PackageSubprogramDeclaration -> declaration.overloadIdentity().toString()
        }

        /** Stable tie-breaker only; source spelling and rule metadata do not define identity. */
        private fun declarationOrderingKey(declaration: ProjectDeclaration): String =
            declarationIdentityKey(declaration) + ":" + declaration.role + ":" + when (declaration) {
                is PackageDeclaration -> ""
                is StandaloneTypeDeclaration -> declaration.shape.toString()
                is PackageTypeDeclaration -> declaration.shape.toString() + declaration.recordFields.joinToString("|") {
                    "${it.ordinal}:${it.name.originalSpelling}:${it.typeRef.structuralKey()}"
                }
                is PackageSubtypeDeclaration -> declaration.baseType.structuralKey()
                is PackageProcedureDeclaration -> declaration.parameters.orderingKey()
                is PackageFunctionDeclaration -> declaration.parameters.orderingKey() + ":" + declaration.returnType.structuralKey()
            }

        private fun List<ProjectParameter>.orderingKey(): String = joinToString("|") {
            "${it.ordinal}:${it.name.originalSpelling}:${it.mode}:${it.nocopy}:${it.defaultPresent}:${it.typeRef.structuralKey()}"
        }
    }
}
