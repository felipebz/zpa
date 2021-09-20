/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.checks

import com.sonar.sslr.api.AstNode
import org.sonar.plsqlopen.asTree
import org.sonar.plsqlopen.sslr.RaiseStatement
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import java.util.*

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
        val statement = node.asTree<RaiseStatement>()
        val exceptionIdentifier = statement.exception
        if (exceptionIdentifier != null) {
            val exceptionName = exceptionIdentifier.tokenOriginalValue

            if (standardExceptions.contains(exceptionName.uppercase(Locale.getDefault()))) {
                addLineIssue(getLocalizedMessage(), exceptionIdentifier.tokenLine, exceptionName)
            }
        }
    }

}
