//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.*

@Rule(priority = Priority.MAJOR)
@ConstantRemediation("20min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class RaiseStandardExceptionCheck : AbstractBaseCheck() {

    private val standardExceptions = listOf(
            "ACCESS_INTO_NULL",
            "CASE_NOT_FOUND",
            "COLLECTION_IS_NULL",
            "CURSOR_ALREADY_OPEN",
            "DUP_VAL_ON_INDEX",
            "INVALID_CURSOR",
            "INVALID_NUMBER",
            "LOGIN_DENIED",
            "NO_DATA_FOUND",
            "NOT_LOGGED_ON",
            "PROGRAM_ERROR",
            "ROWTYPE_MISMATCH",
            "SELF_IS_NULL",
            "STORAGE_ERROR",
            "SUBSCRIPT_BEYOND_COUNT",
            "SUBSCRIPT_OUTSIDE_LIMIT",
            "SYS_INVALID_ROWID",
            "TIMEOUT_ON_RESOURCE",
            "TOO_MANY_ROWS",
            "VALUE_ERROR",
            "ZERO_DIVIDE")

    override fun init() {
        subscribeTo(PlSqlGrammar.RAISE_STATEMENT)
    }

    override fun visitNode(node: AstNode) {
        val exceptionIdentifier = raisedException(node)
        if (exceptionIdentifier != null) {
            val exceptionName = exceptionIdentifier.tokenValue

            if (standardExceptions.contains(exceptionName)) {
                addLineIssue(getLocalizedMessage(), exceptionIdentifier.tokenLine, exceptionName)
            }
        }
    }

    private fun raisedException(node: AstNode): AstNode? {
        return node.getFirstChildOrNull(PlSqlKeyword.RAISE)?.nextSibling
            ?.takeUnless { it.type === PlSqlPunctuator.SEMICOLON }
    }

}
