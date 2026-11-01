//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
     * may not be the one Oracle compiles. A directive that keeps no text, and a branch the configured settings decide,
     * hide nothing Oracle compiles under those settings.
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
