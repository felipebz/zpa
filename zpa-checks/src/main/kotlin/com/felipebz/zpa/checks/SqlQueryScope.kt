//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator

/** Small AST-only helpers for SQL source scopes used by function checks. */
internal object SqlQueryScope {

    fun isCteReference(selectExpression: AstNode, tableReference: AstNode): Boolean {
        val components = tableReference.getChildren(PlSqlGrammar.IDENTIFIER_NAME)
        return components.size == 1 && matchesIdentifier(
            components.single().tokenOriginalValue,
            visibleCteNames(selectExpression)
        )
    }

    fun visibleCteNames(selectExpression: AstNode): List<String> =
        generateSequence(selectExpression) { it.parentOrNull }
            .filter { it.type === DmlGrammar.SELECT_EXPRESSION }
            .flatMap { directCteNames(it).asSequence() }
            .toList()

    fun isDual(tableReference: AstNode): Boolean {
        if (tableReference.hasDescendant(PlSqlPunctuator.REMOTE)) return false
        val names = tableReference.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { it.tokenOriginalValue }
        return when (names.size) {
            1 -> isUnquoted(names[0]) && names[0].equals("DUAL", ignoreCase = true)
            2 -> isUnquoted(names[0]) && isUnquoted(names[1]) &&
                names[0].equals("SYS", ignoreCase = true) && names[1].equals("DUAL", ignoreCase = true)
            else -> false
        }
    }

    private fun directCteNames(selectExpression: AstNode): List<String> =
        selectExpression.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
            .filter { it.getFirstAncestorOrNull(DmlGrammar.SELECT_EXPRESSION) === selectExpression }
            .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue }

    private fun matchesIdentifier(candidate: String, names: Collection<String>): Boolean =
        names.any { expected ->
            if (isUnquoted(candidate) && isUnquoted(expected)) {
                candidate.equals(expected, ignoreCase = true)
            } else {
                candidate == expected
            }
        }

    private fun isUnquoted(value: String): Boolean =
        !value.startsWith("\"") && !value.endsWith("\"")
}
