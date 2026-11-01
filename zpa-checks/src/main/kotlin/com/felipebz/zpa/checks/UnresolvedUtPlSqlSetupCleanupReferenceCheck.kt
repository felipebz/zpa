//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.project.PackageProcedureResolution
import com.felipebz.zpa.checks.utplsql.*

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class UnresolvedUtPlSqlSetupCleanupReferenceCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        val contextModel = UtPlSqlContextCollector.collect(node)
        val packageModels = contextModel.packages.associateBy { it.packageNode }

        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .forEach { group ->
                val packageModel = packageModels[group.packageNode] ?: return@forEach
                if (!packageModel.annotations.any { it.kind == UtPlSqlAnnotationKind.SUITE }) return@forEach
                checkGroup(group)
            }
    }

    private fun checkGroup(group: UtPlSqlAnnotationGroup) {
        group.annotations.forEach { annotation ->
            if (!isApplicable(group, annotation.kind) ||
                annotation.argumentSyntax != UtPlSqlAnnotationArgumentSyntax.PARENTHESIZED ||
                annotation.argument == null
            ) return@forEach

            UtPlSqlExecutableReferenceParser.parse(annotation.argument)
                .forEach { executableReference ->
                    val reference = executableReference.toProjectReference() ?: return@forEach
                    val resolution = projectAnalysis().resolvePackageProcedure(group.packageNode, reference)
                    if (resolution.status in DEFINITIVE_FAILURES) {
                        addIssue(
                            annotation.token,
                            getLocalizedMessage(),
                            executableReference.sourceText
                        )
                    }
                }
        }
    }

    private fun isApplicable(
        group: UtPlSqlAnnotationGroup,
        kind: UtPlSqlAnnotationKind
    ): Boolean = when {
        group.declarationNode == null -> kind in PACKAGE_REFERENCE_ANNOTATIONS
        group.annotations.any { it.kind == UtPlSqlAnnotationKind.TEST } ->
            kind == UtPlSqlAnnotationKind.BEFORETEST || kind == UtPlSqlAnnotationKind.AFTERTEST
        else -> false
    }

    companion object {
        private val PACKAGE_REFERENCE_ANNOTATIONS = setOf(
            UtPlSqlAnnotationKind.BEFOREALL,
            UtPlSqlAnnotationKind.AFTERALL,
            UtPlSqlAnnotationKind.BEFOREEACH,
            UtPlSqlAnnotationKind.AFTEREACH
        )
        private val DEFINITIVE_FAILURES = setOf(
            PackageProcedureResolution.Status.NOT_FOUND,
            PackageProcedureResolution.Status.NOT_CALLABLE,
            PackageProcedureResolution.Status.AMBIGUOUS
        )
    }
}
