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
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCardinality
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlContextCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlPackageContextModel
import java.util.IdentityHashMap

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class DisabledTestCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        val packageModels = IdentityHashMap<AstNode, UtPlSqlPackageContextModel>()
        UtPlSqlContextCollector.collect(node).packages.forEach { packageModel ->
            packageModels[packageModel.packageNode] = packageModel
        }

        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { group ->
                group.packageNode.type == PlSqlGrammar.CREATE_PACKAGE &&
                    group.declarationNode?.type == PlSqlGrammar.PROCEDURE_DECLARATION
            }
            .forEach { group ->
                val packageModel = packageModels[group.packageNode] ?: return@forEach
                if (!packageModel.annotations.any { it.kind == UtPlSqlAnnotationKind.SUITE }) return@forEach

                val test = group.annotations.firstOrNull { it.kind == UtPlSqlAnnotationKind.TEST }
                    ?: return@forEach
                if (!UtPlSqlAnnotationCardinality.isEffectiveForArgumentValidation(packageModel, group, test)) {
                    return@forEach
                }

                group.annotations.firstOrNull { it.kind == UtPlSqlAnnotationKind.DISABLED }
                    ?.let { addIssue(it.token, getLocalizedMessage()) }
            }
    }

}
