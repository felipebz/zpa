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
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationArgumentSyntax
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind
import com.felipebz.zpa.checks.utplsql.UtPlSqlContext
import com.felipebz.zpa.checks.utplsql.UtPlSqlContextCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlOracleWordSyntax
import com.felipebz.zpa.checks.utplsql.UtPlSqlPackageContextModel

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class InvalidUtPlSqlContextStructureCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        UtPlSqlContextCollector.collect(node).packages
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .filter { packageModel -> packageModel.annotations.any { it.kind == UtPlSqlAnnotationKind.SUITE } }
            .forEach(::checkPackage)
    }

    private fun checkPackage(packageModel: UtPlSqlPackageContextModel) {
        packageModel.nameAnnotationsOutsideContexts
            .filterNot { it.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED }
            .forEach {
                addIssue(it.token, getLocalizedMessage("nameOutsideContextMessage"))
            }

        packageModel.unmatchedEndContextAnnotations.forEach {
            addIssue(it.token, getLocalizedMessage("unmatchedEndContextMessage"))
        }

        packageModel.duplicateContextAnnotations.forEach {
            addIssue(it.token, getLocalizedMessage("duplicateContextMessage"))
        }

        packageModel.contexts.forEach(::checkContext)
        checkSiblingNames(packageModel.contexts)
    }

    private fun checkContext(context: UtPlSqlContext) {
        if (context.closingAnnotation == null) {
            addIssue(context.openingAnnotation.token, getLocalizedMessage("missingEndContextMessage"))
        }

        context.selectedNameAnnotation?.let { annotation ->
            if (annotation.argumentSyntax != UtPlSqlAnnotationArgumentSyntax.MALFORMED) {
                val argument = annotation.argument
                if (argument != null && !UtPlSqlOracleWordSyntax.isWordOrDollarOrHash(argument)) {
                    addIssue(annotation.token, getLocalizedMessage("invalidNameMessage"), argument)
                }
            }
        }

        context.duplicateNameAnnotations.forEach { annotation ->
            addIssue(annotation.token, getLocalizedMessage("duplicateNameMessage"))
        }

        context.lateNameAnnotations
            .filterNot { it.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED }
            .forEach { annotation ->
                addIssue(annotation.token, getLocalizedMessage("nameAfterNestedContextMessage"))
            }
    }

    private fun checkSiblingNames(contexts: List<UtPlSqlContext>) {
        val firstByParentAndName = mutableMapOf<Pair<Int?, String>, UtPlSqlAnnotation>()
        contexts.forEach { context ->
            val name = context.selectedNameAnnotation ?: return@forEach
            val argument = name.argument ?: return@forEach
            if (name.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED ||
                !UtPlSqlOracleWordSyntax.isWordOrDollarOrHash(argument)
            ) {
                return@forEach
            }
            val key = context.parentIndex to argument
            if (firstByParentAndName.putIfAbsent(key, name) != null) {
                addIssue(name.token, getLocalizedMessage("duplicateSiblingNameMessage"), argument)
            }
        }
    }
}
