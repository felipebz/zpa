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
import java.util.*

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class InvalidUtPlSqlAnnotationArgumentCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        val contextModel = UtPlSqlContextCollector.collect(node)
        val packageModels = IdentityHashMap<AstNode, UtPlSqlPackageContextModel>()
        contextModel.packages.forEach { packageModels[it.packageNode] = it }

        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .forEach { group ->
                val packageModel = packageModels[group.packageNode] ?: return@forEach
                if (!packageModel.annotations.any { it.kind == UtPlSqlAnnotationKind.SUITE }) return@forEach
                group.annotations.forEach { annotation ->
                    if (isStructurallyEffective(packageModel, annotation) &&
                        UtPlSqlAnnotationCardinality.isEffectiveForArgumentValidation(packageModel, group, annotation)
                    ) {
                        validate(group, annotation)
                    }
                }
            }
    }

    private fun isStructurallyEffective(
        packageModel: UtPlSqlPackageContextModel,
        annotation: UtPlSqlAnnotation
    ): Boolean = when (annotation.kind) {
        UtPlSqlAnnotationKind.NAME -> packageModel.contexts.any { it.selectedNameAnnotation?.token === annotation.token }
        UtPlSqlAnnotationKind.CONTEXT -> packageModel.contexts.any { it.openingAnnotation.token === annotation.token }
        UtPlSqlAnnotationKind.ENDCONTEXT -> packageModel.contexts.any { it.closingAnnotation?.token === annotation.token }
        else -> true
    }

    private fun validate(group: UtPlSqlAnnotationGroup, annotation: UtPlSqlAnnotation) {
        if (annotation.kind == UtPlSqlAnnotationKind.UNKNOWN || !isApplicable(group, annotation)) return
        if (!isArgumentSemanticallyConsumed(group, annotation)) return

        if (annotation.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED) {
            addIssue(annotation.token, getLocalizedMessage("malformedMessage"))
            return
        }

        when (annotation.kind) {
            UtPlSqlAnnotationKind.SUITEPATH -> {
                val argument = annotation.argument
                if (argument == null) {
                    reportRequired(annotation)
                } else if (!UtPlSqlOracleWordSyntax.isQualifiedWordOrDollarOrHash(argument)) {
                    reportInvalidValue(annotation, argument)
                }
            }

            UtPlSqlAnnotationKind.ROLLBACK -> {
                val argument = annotation.argument
                if (argument == null) {
                    reportRequired(annotation)
                } else if (!argument.equals("auto", ignoreCase = true) &&
                    !argument.equals("manual", ignoreCase = true)
                ) {
                    reportInvalidValue(annotation, argument)
                }
            }

            UtPlSqlAnnotationKind.TAGS -> {
                val argument = annotation.argument
                if (argument == null) {
                    reportRequired(annotation)
                } else {
                    argument.split(',')
                        .map(String::trim)
                        .filterNot(::isValidTag)
                        .forEach { invalidTag -> reportInvalidValue(annotation, invalidTag) }
                }
            }

            UtPlSqlAnnotationKind.THROWS,
            UtPlSqlAnnotationKind.BEFORETEST,
            UtPlSqlAnnotationKind.AFTERTEST -> {
                if (annotation.argument == null) {
                    reportRequired(annotation)
                } else if (annotation.kind == UtPlSqlAnnotationKind.BEFORETEST ||
                    annotation.kind == UtPlSqlAnnotationKind.AFTERTEST
                ) {
                    validateExecutableReferences(annotation)
                }
            }

            UtPlSqlAnnotationKind.DISPLAYNAME -> {
                if (group.declarationNode == null && annotation.argument == null) reportRequired(annotation)
            }

            UtPlSqlAnnotationKind.BEFOREALL,
            UtPlSqlAnnotationKind.AFTERALL,
            UtPlSqlAnnotationKind.BEFOREEACH,
            UtPlSqlAnnotationKind.AFTEREACH -> {
                if (group.declarationNode == null) {
                    if (annotation.argument == null) reportRequired(annotation)
                    validateExecutableReferences(annotation)
                }
            }

            else -> Unit
        }
    }

    private fun isArgumentSemanticallyConsumed(
        group: UtPlSqlAnnotationGroup,
        annotation: UtPlSqlAnnotation
    ): Boolean = when (annotation.kind) {
        UtPlSqlAnnotationKind.BEFOREALL,
        UtPlSqlAnnotationKind.AFTERALL,
        UtPlSqlAnnotationKind.BEFOREEACH,
        UtPlSqlAnnotationKind.AFTEREACH -> group.declarationNode == null
        else -> true
    }

    private fun validateExecutableReferences(annotation: UtPlSqlAnnotation) {
        val argument = annotation.argument ?: return
        UtPlSqlExecutableReferenceParser.parse(argument)
            .filterNot { it.hasSupportedComponentShape }
            .forEach { reference ->
                addIssue(
                    annotation.token,
                    getLocalizedMessage("invalidReferenceMessage"),
                    reference.sourceText
                )
            }
    }

    private fun isApplicable(group: UtPlSqlAnnotationGroup, annotation: UtPlSqlAnnotation): Boolean {
        val declaration = group.declarationNode
        val isTest = declaration != null &&
            group.annotations.any { it.kind == UtPlSqlAnnotationKind.TEST }

        return when (annotation.kind) {
            in PACKAGE_ONLY -> declaration == null
            UtPlSqlAnnotationKind.TEST -> declaration != null
            UtPlSqlAnnotationKind.THROWS,
            UtPlSqlAnnotationKind.BEFORETEST,
            UtPlSqlAnnotationKind.AFTERTEST -> isTest
            UtPlSqlAnnotationKind.ROLLBACK,
            UtPlSqlAnnotationKind.TAGS,
            UtPlSqlAnnotationKind.DISPLAYNAME,
            UtPlSqlAnnotationKind.DISABLED -> declaration == null || isTest
            else -> true
        }
    }

    private fun reportRequired(annotation: UtPlSqlAnnotation) {
        addIssue(annotation.token, getLocalizedMessage("requiredMessage"))
    }

    private fun reportInvalidValue(annotation: UtPlSqlAnnotation, value: String) {
        addIssue(annotation.token, getLocalizedMessage("invalidValueMessage"), value)
    }

    private fun isValidTag(tag: String): Boolean =
        tag.length > 1 &&
            !tag.startsWith('-') &&
            tag.none(Char::isWhitespace) &&
            tag.none { it in TAG_EXPRESSION_CHARACTERS } &&
            tag !in RESERVED_TAG_WORDS

    companion object {
        private val TAG_EXPRESSION_CHARACTERS = setOf('(', ')', '&', '|', '!')
        private val RESERVED_TAG_WORDS = setOf("none", "any")
        private val PACKAGE_ONLY = setOf(
            UtPlSqlAnnotationKind.SUITE,
            UtPlSqlAnnotationKind.SUITEPATH,
            UtPlSqlAnnotationKind.CONTEXT,
            UtPlSqlAnnotationKind.NAME,
            UtPlSqlAnnotationKind.ENDCONTEXT
        )
    }
}
