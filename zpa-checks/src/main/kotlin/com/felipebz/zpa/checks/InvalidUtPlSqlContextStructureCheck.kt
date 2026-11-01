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
import com.felipebz.zpa.checks.utplsql.*

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
