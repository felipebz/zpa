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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType

/** Facts about where a node sits, read from the syntax tree. */
internal object PlSqlStructure {

    /** A type method marked `OVERRIDING` (and not `NOT OVERRIDING`) in its inheritance clause. */
    fun isOverridingMember(unit: AstNode): Boolean {
        val inheritance = unit.parentOrNull?.getFirstChildOrNull(PlSqlGrammar.INHERITANCE_CLAUSE) ?: return false
        return inheritance.firstChild.type !== PlSqlKeyword.NOT && inheritance.hasDirectChildren(PlSqlKeyword.OVERRIDING)
    }

    /**
     * Whether the declarative part of [unit] holds a kept conditional-compilation branch: the branch the analysis kept
     * may not be the one Oracle compiles. A directive that keeps no text hides nothing Oracle compiles under the
     * configured settings.
     */
    fun hasKeptConditionalBranchInDeclarations(unit: AstNode): Boolean {
        val declarations = unit.getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)?.tokens.orEmpty()
        val body = unit.getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)?.token
        return (declarations + listOfNotNull(body)).any { token ->
            token.trivia.any {
                it.token.type === PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY
            }
        }
    }
}
