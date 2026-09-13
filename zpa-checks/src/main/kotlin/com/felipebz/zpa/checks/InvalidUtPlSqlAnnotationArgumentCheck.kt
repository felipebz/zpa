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
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationCollector
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationGroup
import com.felipebz.zpa.checks.utplsql.UtPlSqlAnnotationKind

@Rule(priority = Priority.MAJOR, tags = [Tags.UTPLSQL, Tags.BUG])
@RuleInfo(scope = RuleInfo.Scope.TEST)
@ActivatedByDefault
class InvalidUtPlSqlAnnotationArgumentCheck : AbstractBaseCheck() {

    override fun visitFile(node: AstNode) {
        UtPlSqlAnnotationCollector.collect(node).groups
            .filter { it.packageNode.type == PlSqlGrammar.CREATE_PACKAGE }
            .forEach { group ->
                group.annotations.forEach { annotation ->
                    validate(group, annotation)
                }
            }
    }

    private fun validate(group: UtPlSqlAnnotationGroup, annotation: UtPlSqlAnnotation) {
        if (annotation.kind == UtPlSqlAnnotationKind.UNKNOWN || !isApplicable(group, annotation)) return

        if (annotation.argumentSyntax == UtPlSqlAnnotationArgumentSyntax.MALFORMED) {
            addIssue(annotation.token, getLocalizedMessage("malformedMessage"))
            return
        }

        when (annotation.kind) {
            UtPlSqlAnnotationKind.SUITEPATH -> {
                val argument = annotation.argument
                if (argument == null) {
                    reportRequired(annotation)
                } else if (!SUITEPATH_PATTERN.matches(argument)) {
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
                if (annotation.argument == null) reportRequired(annotation)
            }

            UtPlSqlAnnotationKind.DISPLAYNAME -> {
                if (group.declarationNode == null && annotation.argument == null) reportRequired(annotation)
            }

            UtPlSqlAnnotationKind.BEFOREALL,
            UtPlSqlAnnotationKind.AFTERALL,
            UtPlSqlAnnotationKind.BEFOREEACH,
            UtPlSqlAnnotationKind.AFTEREACH -> {
                if (group.declarationNode == null && annotation.argument == null) reportRequired(annotation)
            }

            else -> Unit
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
        private val SUITEPATH_PATTERN = Regex("^[A-Za-z0-9_$#]+(?:\\.[A-Za-z0-9_$#]+)*$")
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
