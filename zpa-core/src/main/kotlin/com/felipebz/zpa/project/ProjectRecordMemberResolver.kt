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
 * Inc., 51 Franklin Street, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.project

/** Resolves only the identity of a field on an already identified project RECORD type. */
class ProjectRecordMemberResolver {
    fun resolve(
        declaration: ProjectTypeDeclaration,
        memberName: OracleIdentifier
    ): ProjectRecordMemberResolution = when (declaration) {
        is PackageTypeDeclaration -> {
            if (declaration.shape != ProjectTypeShape.RECORD) {
                ProjectRecordMemberResolution.UnsupportedType(declaration)
            } else {
                val candidates = declaration.recordFields
                    .filter { it.name == memberName }
                    .deterministicallySorted()
                when (candidates.size) {
                    0 -> ProjectRecordMemberResolution.NotFound(declaration)
                    1 -> ProjectRecordMemberResolution.Resolved(declaration, candidates.single())
                    else -> ProjectRecordMemberResolution.Ambiguous(declaration, candidates)
                }
            }
        }
        else -> ProjectRecordMemberResolution.UnsupportedType(declaration)
    }
}

sealed interface ProjectRecordMemberResolution {
    val declaration: ProjectTypeDeclaration

    data class Resolved(
        override val declaration: PackageTypeDeclaration,
        val field: ProjectRecordField
    ) : ProjectRecordMemberResolution

    data class NotFound(
        override val declaration: PackageTypeDeclaration
    ) : ProjectRecordMemberResolution

    class Ambiguous(
        override val declaration: PackageTypeDeclaration,
        candidates: Collection<ProjectRecordField>
    ) : ProjectRecordMemberResolution {
        val candidates: List<ProjectRecordField> = immutableList(candidates.deterministicallySorted())

        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            declaration == other.declaration && candidates == other.candidates

        override fun hashCode(): Int = 31 * declaration.hashCode() + candidates.hashCode()

        override fun toString(): String = "Ambiguous($declaration, $candidates)"
    }

    data class UnsupportedType(
        override val declaration: ProjectTypeDeclaration
    ) : ProjectRecordMemberResolution
}

private fun Collection<ProjectRecordField>.deterministicallySorted(): List<ProjectRecordField> = sortedWith(
    compareBy(
        { it.ordinal },
        { it.sourceRange.fileId.value },
        { it.sourceRange.startLine },
        { it.sourceRange.startColumn },
        { it.name.originalSpelling },
        { it.typeRef.structuralKey() }
    )
)
