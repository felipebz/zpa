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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.PackageFunctionDeclaration
import com.felipebz.zpa.project.PackageProcedureDeclaration
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolution
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolver
import kotlin.jvm.JvmSynthetic

/** Read-only project information available to a custom check during one analysis run. */
@ZpaExperimentalApi
class ProjectAnalysis private constructor(contextHolder: Any) {

    private val projectAnalysisContext = contextHolder as? ProjectAnalysisContext
        ?: throw IllegalArgumentException("Unexpected project analysis context")
    private val specificationResolver = ProjectSubprogramSpecificationResolver(projectAnalysisContext)

    /**
     * Resolves the package specification corresponding to an immediate PACKAGE BODY declaration.
     * Nodes that are not associated package-body declarations return NOT_APPLICABLE.
     */
    @ZpaExperimentalApi
    fun resolvePackageSpecification(node: AstNode): PackageSpecificationResolution {
        if (projectAnalysisContext.state.kind == ProjectAnalysisContext.State.Kind.NOT_PREPARED) {
            return PackageSpecificationResolution.notPrepared()
        }
        val semanticNode = node as? SemanticAstNode
            ?: return PackageSpecificationResolution.notApplicable()
        val body = semanticNode.projectSubprogramDeclaration
            ?: return PackageSpecificationResolution.notApplicable()

        return when (val resolution = specificationResolver.resolve(body)) {
            is ProjectSubprogramSpecificationResolution.Resolved ->
                PackageSpecificationResolution.resolved(
                    resolution.body.toPublicView(),
                    resolution.specification.toPublicView()
                )
            is ProjectSubprogramSpecificationResolution.NotFound -> PackageSpecificationResolution.notFound()
            is ProjectSubprogramSpecificationResolution.Ambiguous -> PackageSpecificationResolution.ambiguous()
            is ProjectSubprogramSpecificationResolution.Incomplete -> PackageSpecificationResolution.incomplete()
            is ProjectSubprogramSpecificationResolution.NotPrepared -> PackageSpecificationResolution.notPrepared()
        }
    }

    @OptIn(ZpaExperimentalApi::class)
    private fun PackageSubprogramDeclaration.toPublicView(): PackageSubprogram {
        val kind = when (this) {
            is PackageProcedureDeclaration -> PackageSubprogramKind.PROCEDURE
            is PackageFunctionDeclaration -> PackageSubprogramKind.FUNCTION
        }
        return PackageSubprogram(
            kind,
            parameters.map { PackageParameter(it.ordinal, it.nocopy) }
        )
    }

    internal companion object {
        @JvmSynthetic
        fun create(contextHolder: Any): ProjectAnalysis = ProjectAnalysis(contextHolder)

        @JvmSynthetic
        fun notPrepared(): ProjectAnalysis = create(ProjectAnalysisContext.NOT_PREPARED)
    }
}
