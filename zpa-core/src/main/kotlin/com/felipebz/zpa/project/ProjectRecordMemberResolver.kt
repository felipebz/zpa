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

/** Resolves only the identity of a RECORD field or OBJECT attribute on an already identified project type. */
class ProjectRecordMemberResolver(private val projectAnalysisContext: ProjectAnalysisContext? = null) {
    private val typeResolver = projectAnalysisContext?.let(::ProjectTypeResolver)

    fun resolve(
        declaration: ProjectTypeDeclaration,
        memberName: OracleIdentifier
    ): ProjectRecordMemberResolution = when (declaration) {
        is PackageTypeDeclaration -> {
            if (declaration.shape != ProjectTypeShape.RECORD) {
                ProjectRecordMemberResolution.UnsupportedType(declaration)
            } else {
                found(declaration, declaration.recordFields, memberName) ?: ProjectRecordMemberResolution.NotFound(declaration)
            }
        }
        is StandaloneTypeDeclaration ->
            if (declaration.shape != ProjectTypeShape.OBJECT) ProjectRecordMemberResolution.UnsupportedType(declaration)
            else attribute(declaration, memberName)
        else -> ProjectRecordMemberResolution.UnsupportedType(declaration)
    }

    private fun attribute(type: StandaloneTypeDeclaration, memberName: OracleIdentifier): ProjectRecordMemberResolution {
        val visited = HashSet<StandaloneTypeDeclaration>()
        var current = type
        var complete = true
        while (true) {
            if (!visited.add(current)) return ProjectRecordMemberResolution.Incomplete(type)
            found(current, current.attributes, memberName)?.let { found ->
                // Statements are not ordered across files: the drop may precede or follow this declaration.
                return if (isDropped(current, memberName)) ProjectRecordMemberResolution.Incomplete(type) else found
            }
            // Methods are not modeled.
            if (memberName in current.methodNames) return ProjectRecordMemberResolution.UnsupportedType(type)
            if (!current.attributesComplete || isAltered(current)) complete = false
            val supertype = current.supertype ?: break
            current = supertypeOf(supertype) ?: return ProjectRecordMemberResolution.Incomplete(type)
        }
        return if (complete) ProjectRecordMemberResolution.NotFound(type) else ProjectRecordMemberResolution.Incomplete(type)
    }

    private fun found(declaration: ProjectTypeDeclaration, fields: List<ProjectRecordField>, memberName: OracleIdentifier): ProjectRecordMemberResolution? {
        val candidates = fields.filter { it.name == memberName }.deterministicallySorted()
        return when (candidates.size) {
            0 -> null
            1 -> ProjectRecordMemberResolution.Resolved(declaration, candidates.single())
            else -> ProjectRecordMemberResolution.Ambiguous(declaration, candidates)
        }
    }

    private fun supertypeOf(reference: TypeRef): StandaloneTypeDeclaration? {
        val named = reference as? NamedTypeRef ?: return null
        val resolution = typeResolver?.resolve(named) as? ProjectTypeResolution.Resolved ?: return null
        return (resolution.declaration as? StandaloneTypeDeclaration)?.takeIf { it.shape == ProjectTypeShape.OBJECT }
    }

    private fun isDropped(type: StandaloneTypeDeclaration, name: OracleIdentifier): Boolean {
        val dropped = (projectAnalysisContext?.state as? ProjectAnalysisContext.State.Prepared)?.result?.index?.droppedTypeAttributes
            ?: return false
        return dropped.any { (altered, names) -> altered.last == type.name.last && name in names }
    }

    private fun isAltered(type: StandaloneTypeDeclaration): Boolean {
        val altered = (projectAnalysisContext?.state as? ProjectAnalysisContext.State.Prepared)?.result?.index?.alteredTypeAttributes
            ?: return false
        return altered.any { it.last == type.name.last }
    }
}

sealed interface ProjectRecordMemberResolution {
    val declaration: ProjectTypeDeclaration

    data class Resolved(
        override val declaration: ProjectTypeDeclaration,
        val field: ProjectRecordField
    ) : ProjectRecordMemberResolution

    data class NotFound(
        override val declaration: ProjectTypeDeclaration
    ) : ProjectRecordMemberResolution

    class Ambiguous(
        override val declaration: ProjectTypeDeclaration,
        candidates: Collection<ProjectRecordField>
    ) : ProjectRecordMemberResolution {
        val candidates: List<ProjectRecordField> = immutableList(candidates.deterministicallySorted())

        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            declaration == other.declaration && candidates == other.candidates

        override fun hashCode(): Int = 31 * declaration.hashCode() + candidates.hashCode()

        override fun toString(): String = "Ambiguous($declaration, $candidates)"
    }

    /** Not found or not provable: the type's attribute chain is not fully known to the project. */
    data class Incomplete(
        override val declaration: ProjectTypeDeclaration
    ) : ProjectRecordMemberResolution

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
