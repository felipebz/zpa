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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.PackageParameter
import com.felipebz.zpa.api.project.PackageSpecificationResolution

@OptIn(ZpaExperimentalApi::class)
@Rule(priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class PackageBodyParameterNocopyCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val resolution = projectAnalysis().resolvePackageSpecification(node)
        if (resolution.status != PackageSpecificationResolution.Status.RESOLVED) return

        val body = resolution.body.orElseThrow()
        val specification = resolution.specification.orElseThrow()

        val bodyParameters = body.parameters
        val specificationParameters = specification.parameters
        val astParameters = node.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS)
            ?.getChildren(PlSqlGrammar.PARAMETER_DECLARATION)
            ?: emptyList()

        if (!parametersAreAligned(bodyParameters, specificationParameters, astParameters.size)) return

        bodyParameters.forEachIndexed { index, bodyParameter ->
            val specificationParameter = specificationParameters[index]
            if (specificationParameter.isNocopy() != bodyParameter.isNocopy()) {
                addIssue(
                    astParameters[index].getFirstChild(PlSqlGrammar.IDENTIFIER_NAME),
                    getLocalizedMessage()
                )
            }
        }
    }

    private fun parametersAreAligned(
        bodyParameters: List<PackageParameter>,
        specificationParameters: List<PackageParameter>,
        astParameterCount: Int
    ): Boolean {
        if (bodyParameters.size != specificationParameters.size || bodyParameters.size != astParameterCount) {
            return false
        }
        return bodyParameters.indices.all { index ->
            val expectedOrdinal = index + 1
            bodyParameters[index].ordinal == expectedOrdinal &&
                specificationParameters[index].ordinal == expectedOrdinal
        }
    }
}
