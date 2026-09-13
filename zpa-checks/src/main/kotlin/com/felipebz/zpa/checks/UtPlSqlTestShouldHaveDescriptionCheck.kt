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
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationArgumentSyntax
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCardinality
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationGroup
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlContextCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlPackageContextModel
import java.util.IdentityHashMap

@Rule(priority = Priority.MINOR, tags = [Tags.UTPLSQL])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ConstantRemediation("2min")
class UtPlSqlTestShouldHaveDescriptionCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        val contextModel = UtPlSqlContextCollector.collect(node)
        val packageModels = IdentityHashMap<AstNode, UtPlSqlPackageContextModel>()
        contextModel.packages.forEach { packageModels[it.packageNode] = it }

        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .forEach { group ->
                val packageModel = packageModels[group.packageNode] ?: return@forEach
                if (!hasEffectiveSuite(packageModel)) return@forEach
                checkTestDescription(group, packageModel)
            }
    }

    private fun checkTestDescription(
        group: UtPlSqlAnnotationGroup,
        packageModel: UtPlSqlPackageContextModel
    ) {
        val declaration = group.declarationNode ?: return
        if (declaration.type != PlSqlGrammar.PROCEDURE_DECLARATION) return

        val test = group.annotations.firstOrNull { it.kind == UtPlSqlAnnotationKind.TEST } ?: return
        if (!UtPlSqlAnnotationCardinality.isEffectiveForArgumentValidation(packageModel, group, test)) return
        if (test.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED) return

        val displayName = group.annotations.firstOrNull {
            it.kind == UtPlSqlAnnotationKind.DISPLAYNAME &&
                UtPlSqlAnnotationCardinality.isEffectiveForArgumentValidation(packageModel, group, it)
        }
        if (displayName?.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED) return

        val description = if (displayName != null) displayName.argument else test.argument
        if (description.isNullOrBlank()) {
            addIssue(test.token, getLocalizedMessage())
        }
    }

    private fun hasEffectiveSuite(packageModel: UtPlSqlPackageContextModel): Boolean =
        packageModel.annotations.any { it.kind == UtPlSqlAnnotationKind.SUITE }
}
