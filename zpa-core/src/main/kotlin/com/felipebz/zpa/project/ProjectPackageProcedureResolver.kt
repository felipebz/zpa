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

/** Resolves only immutable package specification facts; it never consults lexical scopes or AST state. */
internal class ProjectPackageProcedureResolver(
    private val context: ProjectAnalysisContext
) {

    fun resolve(
        targetPackage: QualifiedName,
        procedureName: OracleIdentifier
    ): ProjectPackageProcedureResolution {
        val prepared = context.state as? ProjectAnalysisContext.State.Prepared
            ?: return ProjectPackageProcedureResolution.NotPrepared
        if (prepared.result.failures.isNotEmpty()) {
            return ProjectPackageProcedureResolution.Incomplete
        }

        val specifications = prepared.result.index.findPackages(targetPackage)
            .filter { it.role == DeclarationRole.SPECIFICATION }
        if (specifications.isEmpty()) {
            return ProjectPackageProcedureResolution.UnknownTarget
        }
        if (specifications.size > 1) {
            return ProjectPackageProcedureResolution.AmbiguousTarget
        }

        val candidates = prepared.result.index.findSubprograms(targetPackage, procedureName)
            .filter { it.role == DeclarationRole.SPECIFICATION }
        val procedures = candidates.filterIsInstance<PackageProcedureDeclaration>()
        if (procedures.isEmpty()) {
            return if (candidates.isEmpty()) {
                ProjectPackageProcedureResolution.NotFound
            } else {
                ProjectPackageProcedureResolution.NotCallable
            }
        }

        val callable = procedures.filter { procedure ->
            procedure.parameters.all { parameter -> parameter.defaultPresent }
        }
        return when (callable.size) {
            0 -> ProjectPackageProcedureResolution.NotCallable
            1 -> ProjectPackageProcedureResolution.Resolved(callable.single())
            else -> ProjectPackageProcedureResolution.AmbiguousCall
        }
    }
}
