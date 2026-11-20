//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MAJOR, category = RuleCategory.READABILITY)
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class ColumnsShouldHaveTableNameCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(DmlGrammar.SELECT_COLUMN)
    }

    override fun visitNode(node: AstNode) {
        var candidate = node.firstChild
        if (candidate.hasChildren()) {
            candidate = candidate.firstChild
        }

        val from = node.parentOrNull?.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE) ?: return
        if (candidate.typeIs(PlSqlGrammar.IDENTIFIER_NAME) &&
            !isStandaloneQueryValue(candidate) &&
            readsSeveralSources(from) &&
            !namesUsingColumn(from, candidate) &&
            isColumn(candidate)) {

            addIssue(candidate, getLocalizedMessage(), candidate.tokenOriginalValue)
        }
    }

    /**
     * Whether [from] reads more than one source, joined by commas or by any join, and has no natural join: Oracle
     * rejects a qualifier on the columns a natural join shares (ORA-25155), and which they are depends on the columns of
     * both sources.
     */
    private fun readsSeveralSources(from: AstNode): Boolean {
        val joins = owned(from, DmlGrammar.INNER_CROSS_JOIN_CLAUSE, DmlGrammar.OUTER_JOIN_CLAUSE)
        if (joins.any { it.hasDirectChildren(PlSqlKeyword.NATURAL) }) return false
        // A source between parentheses, `from (t)` or `from (a join b)`, wraps the sources it holds: count those.
        val sources = owned(from, DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
        return sources.count { source -> sources.none { it !== source && generateSequence(it.parentOrNull) { p -> p.parentOrNull }.any { p -> p === source } } } > 1
    }

    /** Whether [identifier] is a column of a `JOIN … USING` list of [from]: Oracle rejects a qualifier on it (ORA-25154). */
    private fun namesUsingColumn(from: AstNode, identifier: AstNode): Boolean =
        owned(from, DmlGrammar.ON_OR_USING_EXPRESSION)
            .filter { it.hasDirectChildren(PlSqlKeyword.USING) }
            .any { using -> using.getChildren(PlSqlGrammar.IDENTIFIER_NAME).any { it.tokenValue == identifier.tokenValue } }

    /** The nodes of [types] under [from] that belong to it, not to a subquery with its own `FROM`. */
    private fun owned(from: AstNode, vararg types: AstNodeType): List<AstNode> =
        from.getDescendants(*types).filter { it.getFirstAncestorOrNull(DmlGrammar.FROM_CLAUSE) === from }

    private fun isColumn(identifier: AstNode): Boolean {
        val name = identifier.parentOrNull?.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return false
        return (context.nameBinding(name) as? NameBinding.NothingLocal)?.sqlColumnPossible == true
    }

    private fun isStandaloneQueryValue(node: AstNode): Boolean {
        if (node.tokenOriginalValue.startsWith('"')) return false
        return when (node.tokenValue) {
            "ROWNUM", "LEVEL", "CONNECT_BY_ISCYCLE", "CONNECT_BY_ISLEAF",
            "SYSDATE", "SYSTIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP", "LOCALTIMESTAMP",
            "DBTIMEZONE", "SESSIONTIMEZONE", "USER", "UID" -> true
            else -> false
        }
    }
}
