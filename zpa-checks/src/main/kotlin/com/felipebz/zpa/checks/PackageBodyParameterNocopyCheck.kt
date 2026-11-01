//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
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
