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
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotation
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationGroup
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCardinality
import com.felipebz.zpa.checks.utplsql.UtPlSqlContextCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlPackageContextModel

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class ConflictingUtPlSqlAnnotationsCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        val annotationModel = UtPlSqlAnnotationCollector.collect(node)
        val contextModel = UtPlSqlContextCollector.collect(node)
        val groups = annotationModel.groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }

        groups.groupBy { it.packageNode }.values.forEach { packageGroups ->
            val packageModel = contextModel.packages.firstOrNull { it.packageNode === packageGroups.first().packageNode }
            checkPackageGlobalDuplicates(packageGroups, packageModel)
            val hasEffectiveSuite = packageModel?.annotations?.any { it.kind == UtPlSqlAnnotationKind.SUITE } == true
            packageGroups.filter { it.declarationNode != null }.forEach { group ->
                checkProcedureGroup(group, hasEffectiveSuite)
            }
            if (hasEffectiveSuite) {
                checkEffectiveScopeDuplicates(packageModel)
            }
        }
    }

    private fun checkPackageGlobalDuplicates(
        packageGroups: List<UtPlSqlAnnotationGroup>,
        packageModel: UtPlSqlPackageContextModel?
    ) {
        if (packageModel?.annotations?.none { it.kind == UtPlSqlAnnotationKind.SUITE } == true) return
        checkDuplicate(
            packageModel?.annotations
                ?: packageGroups.filter { it.declarationNode == null }.flatMap { it.annotations },
            UtPlSqlAnnotationCardinality.PACKAGE_GLOBAL_SINGLETONS
        )
    }

    private fun checkEffectiveScopeDuplicates(packageModel: UtPlSqlPackageContextModel) {
        checkDuplicate(packageModel.rootAnnotations, UtPlSqlAnnotationCardinality.SCOPE_SINGLETONS)
        packageModel.contexts.forEach { context ->
            checkDuplicate(context.annotations, UtPlSqlAnnotationCardinality.SCOPE_SINGLETONS)
        }
    }

    private fun checkProcedureGroup(group: UtPlSqlAnnotationGroup, hasEffectiveSuite: Boolean) {
        checkPackageOnlyAnnotations(group)
        if (hasEffectiveSuite) {
            checkDuplicate(group.annotations, UtPlSqlAnnotationCardinality.PROCEDURE_SINGLETONS)
            checkRoleConflicts(group)
        }
    }

    private fun checkPackageOnlyAnnotations(group: UtPlSqlAnnotationGroup) {
        group.annotations
            .filter { it.kind in PACKAGE_ONLY }
            .forEach {
                addIssue(it.token, getLocalizedMessage("packageOnlyMessage"))
            }
    }

    private fun checkDuplicate(
        annotations: List<UtPlSqlAnnotation>,
        singletonKinds: Set<UtPlSqlAnnotationKind>
    ) {
        val firstByKind = mutableMapOf<UtPlSqlAnnotationKind, String>()
        annotations.forEach { annotation ->
            if (annotation.kind !in singletonKinds) return@forEach

            val first = firstByKind.putIfAbsent(annotation.kind, annotation.name)
            if (first != null) {
                addIssue(annotation.token, getLocalizedMessage(), annotation.name)
            }
        }
    }

    private fun checkRoleConflicts(group: UtPlSqlAnnotationGroup) {
        val testAnnotation = group.annotations.firstOrNull { it.kind == UtPlSqlAnnotationKind.TEST } ?: return
        group.annotations
            .filter { it.kind in PROCEDURE_SETUP_ROLES }
            .forEach { annotation ->
                addIssue(annotation.token, getLocalizedMessage(), testAnnotation.name)
            }
    }

    companion object {
        private val PACKAGE_ONLY = setOf(
            UtPlSqlAnnotationKind.SUITE,
            UtPlSqlAnnotationKind.SUITEPATH,
            UtPlSqlAnnotationKind.CONTEXT,
            UtPlSqlAnnotationKind.NAME,
            UtPlSqlAnnotationKind.ENDCONTEXT
        )
        private val PROCEDURE_SETUP_ROLES = setOf(
            UtPlSqlAnnotationKind.BEFOREALL,
            UtPlSqlAnnotationKind.AFTERALL,
            UtPlSqlAnnotationKind.BEFOREEACH,
            UtPlSqlAnnotationKind.AFTEREACH
        )
    }
}
