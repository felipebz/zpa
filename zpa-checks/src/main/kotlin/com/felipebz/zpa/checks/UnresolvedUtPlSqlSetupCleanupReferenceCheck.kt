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
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.PackageProcedureResolution
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationArgumentSyntax
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationGroup
import com.felipebz.zpa.checks.utplsql.UtPlSqlContextCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlExecutableReference
import com.felipebz.zpa.checks.utplsql.UtPlSqlExecutableReferenceParser

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
