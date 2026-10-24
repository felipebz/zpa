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

/** Resolves only the identity of a RECORD field, or of an OBJECT attribute or method set, on an already identified project type. */
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
            else member(declaration, memberName)
        else -> ProjectRecordMemberResolution.UnsupportedType(declaration)
    }

    /**
     * An attribute or the methods of that name along the chain from [type]. Methods form one overload set: the type's
     * own first, then inherited ones that a nearer method of the same parameters does not override. Constructors are
     * not members of a value.
     */
    private fun member(type: StandaloneTypeDeclaration, memberName: OracleIdentifier): ProjectRecordMemberResolution {
        val visited = HashSet<StandaloneTypeDeclaration>()
        val methods = ArrayList<ProjectMethodCandidate>()
        var current = type
        var complete = true
        while (true) {
            if (!visited.add(current)) return methodsOr(type, methods, false) { ProjectRecordMemberResolution.Incomplete(type) }
            if (methods.isEmpty()) {
                found(current, current.attributes, memberName)?.let { found ->
                    // Statements are not ordered across files: the drop may precede or follow this declaration.
                    return if (isDropped(current, memberName)) ProjectRecordMemberResolution.Incomplete(type) else found
                }
            }
            for (method in current.methods) {
                if (method.name != memberName || method.methodKind == ProjectMethodKind.CONSTRUCTOR) continue
                if (methods.none { it.method.overloadKey() == method.overloadKey() }) methods += ProjectMethodCandidate(current, method)
            }
            if (!current.attributesComplete || !current.methodsComplete || isAltered(current)) complete = false
            val supertype = current.supertype ?: break
            current = supertypeOf(supertype) ?: return methodsOr(type, methods, false) { ProjectRecordMemberResolution.Incomplete(type) }
        }
        return methodsOr(type, methods, complete) {
            if (complete) ProjectRecordMemberResolution.NotFound(type) else ProjectRecordMemberResolution.Incomplete(type)
        }
    }

    private inline fun methodsOr(
        type: StandaloneTypeDeclaration,
        methods: List<ProjectMethodCandidate>,
        complete: Boolean,
        otherwise: () -> ProjectRecordMemberResolution
    ): ProjectRecordMemberResolution = if (methods.isEmpty()) otherwise() else ProjectRecordMemberResolution.Method(type, methods, complete)

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
        val altered = (projectAnalysisContext?.state as? ProjectAnalysisContext.State.Prepared)?.result?.index?.alteredTypes ?: return false
        return altered.dropped(type.name, name)
    }

    private fun isAltered(type: StandaloneTypeDeclaration): Boolean {
        val altered = (projectAnalysisContext?.state as? ProjectAnalysisContext.State.Prepared)?.result?.index?.alteredTypes ?: return false
        return altered.attributesOf(type.name) || altered.methodsOf(type.name)
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

    /**
     * Methods of that name: the overload set, the type's own first, then inherited ones not overridden. Static methods
     * are kept with their kind. [complete] is false when a list of the chain is incomplete, altered or outside the project.
     */
    class Method(
        override val declaration: ProjectTypeDeclaration,
        overloads: Collection<ProjectMethodCandidate>,
        val complete: Boolean
    ) : ProjectRecordMemberResolution {
        val overloads: List<ProjectMethodCandidate> = immutableList(overloads)

        override fun equals(other: Any?): Boolean = other is Method &&
            declaration == other.declaration && overloads == other.overloads && complete == other.complete

        override fun hashCode(): Int = 31 * (31 * declaration.hashCode() + overloads.hashCode()) + complete.hashCode()

        override fun toString(): String = "Method($declaration, $overloads, complete=$complete)"
    }

    /** Not found or not provable: the type's attribute chain is not fully known to the project. */
    data class Incomplete(
        override val declaration: ProjectTypeDeclaration
    ) : ProjectRecordMemberResolution

    data class UnsupportedType(
        override val declaration: ProjectTypeDeclaration
    ) : ProjectRecordMemberResolution
}

/** A method of an overload set and the object type that declares it. */
data class ProjectMethodCandidate(val declaringType: StandaloneTypeDeclaration, val method: ProjectTypeMethod)

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
