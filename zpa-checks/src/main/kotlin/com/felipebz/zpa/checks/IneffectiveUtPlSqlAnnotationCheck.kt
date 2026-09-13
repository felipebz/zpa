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
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationParser

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class IneffectiveUtPlSqlAnnotationCheck : AbstractBaseCheck() {

    private var packageBodyDepth = 0

    override fun startScan() {
        super.startScan()
        packageBodyDepth = 0
    }

    override fun init() {
        subscribeTo(PlSqlGrammar.CREATE_PACKAGE_BODY)
    }

    override fun visitNode(node: AstNode) {
        if (node.type == PlSqlGrammar.CREATE_PACKAGE_BODY) {
            packageBodyDepth++
        }
    }

    override fun leaveNode(node: AstNode) {
        if (node.type == PlSqlGrammar.CREATE_PACKAGE_BODY) {
            packageBodyDepth--
        }
    }

    override fun visitComment(trivia: Trivia, content: String) {
        if (packageBodyDepth == 0) return

        val annotation = UtPlSqlAnnotationParser.parse(trivia.token, content) ?: return
        if (annotation.kind != UtPlSqlAnnotationKind.UNKNOWN) {
            addIssue(annotation.token, getLocalizedMessage())
        }
    }
}
