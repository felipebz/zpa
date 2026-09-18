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
