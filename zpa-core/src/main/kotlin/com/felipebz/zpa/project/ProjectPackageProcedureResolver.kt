//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
