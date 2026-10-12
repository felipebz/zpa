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

/** Internal outcome of resolving a public package procedure callable without arguments. */
internal sealed interface ProjectPackageProcedureResolution {
    data class Resolved(val declaration: PackageProcedureDeclaration) : ProjectPackageProcedureResolution
    data object NotFound : ProjectPackageProcedureResolution
    data object NotCallable : ProjectPackageProcedureResolution
    data object AmbiguousCall : ProjectPackageProcedureResolution
    data object AmbiguousTarget : ProjectPackageProcedureResolution
    data object UnknownTarget : ProjectPackageProcedureResolution
    data object Incomplete : ProjectPackageProcedureResolution
    data object NotPrepared : ProjectPackageProcedureResolution
}

/**
 * Zero-argument procedure call `targetPackage.procedureName` as an adapter over [ProjectCallBinder].
 * Discovery, coverage, kind and callability are the binder's; a specification that may hold unretained callable
 * headers is [ProjectPackageProcedureResolution.Incomplete], never proof of absence or uniqueness.
 */
internal class ProjectPackageProcedureResolver(
    context: ProjectAnalysisContext
) {
    private val binder = ProjectCallBinder(context)

    fun resolve(
        targetPackage: QualifiedName,
        procedureName: OracleIdentifier
    ): ProjectPackageProcedureResolution {
        val result = binder.resolve(
            ProjectCallTarget.packageMember(targetPackage, procedureName),
            ProjectCallKind.PROCEDURE,
            ProjectCallArguments.none()
        )
        return when (result.status) {
            ProjectCallResolution.Status.NOT_PREPARED -> ProjectPackageProcedureResolution.NotPrepared
            ProjectCallResolution.Status.INCOMPLETE_INDEX,
            ProjectCallResolution.Status.PARTIAL_DECLARATIONS -> ProjectPackageProcedureResolution.Incomplete
            ProjectCallResolution.Status.AMBIGUOUS_TARGET -> ProjectPackageProcedureResolution.AmbiguousTarget
            ProjectCallResolution.Status.UNKNOWN_PACKAGE -> ProjectPackageProcedureResolution.UnknownTarget
            ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT -> ProjectPackageProcedureResolution.NotFound
            ProjectCallResolution.Status.WRONG_KIND,
            ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE -> ProjectPackageProcedureResolution.NotCallable
            ProjectCallResolution.Status.RESOLVED ->
                ProjectPackageProcedureResolution.Resolved(requireNotNull(result.selected).declaration.declaration as PackageProcedureDeclaration)
            ProjectCallResolution.Status.AMBIGUOUS -> ProjectPackageProcedureResolution.AmbiguousCall
            ProjectCallResolution.Status.INVALID_NOTATION ->
                error("Zero actual arguments cannot have an invalid notation")
        }
    }
}
