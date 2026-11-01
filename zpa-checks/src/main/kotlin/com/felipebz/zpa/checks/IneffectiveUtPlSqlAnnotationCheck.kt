//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class IneffectiveUtPlSqlAnnotationCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE_BODY }
            .flatMap { it.annotations }
            .filter { it.kind != UtPlSqlAnnotationKind.UNKNOWN }
            .forEach { addIssue(it.token, getLocalizedMessage()) }
    }
}
