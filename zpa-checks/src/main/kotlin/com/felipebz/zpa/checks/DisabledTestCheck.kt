//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.checks.utplsql.*
import java.util.*

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL], category = RuleCategory.TESTING)
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
