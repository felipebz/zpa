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
package com.felipebz.zpa.internal

import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.PackageFunctionDeclaration
import com.felipebz.zpa.project.PackageProcedureDeclaration
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolution
import com.felipebz.zpa.project.ProjectSubprogramSpecificationResolver
import java.util.Collections
import kotlin.jvm.JvmSynthetic

/**
 * Immutable project-analysis capability reserved for ZPA's built-in modules.
 *
 * The capability interprets existing semantic decorations and delegates project-wide
 * correlation to the project model. It does not retain the queried AST node.
 */
@ZpaInternalApi
class BuiltInProjectAnalysisQueries private constructor(
    private val projectAnalysisContext: ProjectAnalysisContext
) {
    private val specificationResolver = ProjectSubprogramSpecificationResolver(projectAnalysisContext)

    companion object {
        @JvmSynthetic
        internal fun create(projectAnalysisContext: ProjectAnalysisContext): BuiltInProjectAnalysisQueries =
            BuiltInProjectAnalysisQueries(projectAnalysisContext)
    }

    /** Resolves the specification for an associated immediate PACKAGE BODY declaration. */
    @ZpaInternalApi
    @JvmSynthetic
    fun resolvePackageSpecification(node: SemanticAstNode): BuiltInPackageSpecificationResolution {
        if (projectAnalysisContext.state is ProjectAnalysisContext.State.NotPrepared) {
            return BuiltInPackageSpecificationResolution.NotPrepared
        }

        val body = node.projectSubprogramDeclaration
            ?: return BuiltInPackageSpecificationResolution.NotApplicable
        return when (val resolution = specificationResolver.resolve(body)) {
            is ProjectSubprogramSpecificationResolution.Resolved ->
                BuiltInPackageSpecificationResolution.Resolved(
                    body = resolution.body.toBuiltInView(),
                    specification = resolution.specification.toBuiltInView()
                )
            is ProjectSubprogramSpecificationResolution.NotFound ->
                BuiltInPackageSpecificationResolution.NotFound
            is ProjectSubprogramSpecificationResolution.Ambiguous ->
                BuiltInPackageSpecificationResolution.Ambiguous(resolution.candidates.size)
            is ProjectSubprogramSpecificationResolution.Incomplete ->
                BuiltInPackageSpecificationResolution.Incomplete(
                    knownCandidateCount = resolution.knownCandidates.size,
                    failureCount = resolution.failures.size
                )
            is ProjectSubprogramSpecificationResolution.NotPrepared ->
                BuiltInPackageSpecificationResolution.NotPrepared
        }
    }
}

@OptIn(ZpaInternalApi::class)
private fun PackageSubprogramDeclaration.toBuiltInView(): BuiltInPackageSubprogram =
    BuiltInPackageSubprogram(
        kind = when (this) {
            is PackageProcedureDeclaration -> BuiltInPackageSubprogramKind.PROCEDURE
            is PackageFunctionDeclaration -> BuiltInPackageSubprogramKind.FUNCTION
        },
        parameters = parameters.map { parameter ->
            BuiltInPackageParameter(
                ordinal = parameter.ordinal,
                nocopy = parameter.nocopy,
                defaultPresent = parameter.defaultPresent
            )
        }
    )

@ZpaInternalApi
enum class BuiltInPackageSubprogramKind {
    PROCEDURE,
    FUNCTION
}

@ZpaInternalApi
class BuiltInPackageParameter internal constructor(
    val ordinal: Int,
    val nocopy: Boolean,
    val defaultPresent: Boolean
)

@ZpaInternalApi
class BuiltInPackageSubprogram internal constructor(
    val kind: BuiltInPackageSubprogramKind,
    parameters: Collection<BuiltInPackageParameter>
) {
    val parameters: List<BuiltInPackageParameter> =
        Collections.unmodifiableList(parameters.toList())
}

@ZpaInternalApi
sealed interface BuiltInPackageSpecificationResolution {
    data object NotApplicable : BuiltInPackageSpecificationResolution
    data object NotPrepared : BuiltInPackageSpecificationResolution
    data object NotFound : BuiltInPackageSpecificationResolution

    data class Ambiguous(val candidateCount: Int) : BuiltInPackageSpecificationResolution {
        init {
            require(candidateCount > 1)
        }
    }

    data class Incomplete(
        val knownCandidateCount: Int,
        val failureCount: Int
    ) : BuiltInPackageSpecificationResolution {
        init {
            require(knownCandidateCount >= 0)
            require(failureCount > 0)
        }
    }

    data class Resolved(
        val body: BuiltInPackageSubprogram,
        val specification: BuiltInPackageSubprogram
    ) : BuiltInPackageSpecificationResolution
}
