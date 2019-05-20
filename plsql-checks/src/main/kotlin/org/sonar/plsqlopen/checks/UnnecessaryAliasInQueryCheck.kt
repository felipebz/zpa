/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import com.google.common.collect.ArrayListMultimap
import com.sonar.sslr.api.AstNode
import com.sonar.sslr.api.AstNodeType
import org.sonar.check.Priority
import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonar.plugins.plsqlopen.api.DmlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annnotations.ConstantRemediation
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo

@Rule(key = UnnecessaryAliasInQueryCheck.CHECK_KEY, priority = Priority.MINOR)
@ConstantRemediation("1min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
class UnnecessaryAliasInQueryCheck : AbstractBaseCheck() {

    @RuleProperty(key = "acceptedLength", defaultValue = "" + DEFAULT_ACCEPTED_LENGTH)
    var acceptedLength = DEFAULT_ACCEPTED_LENGTH

    private val dmlStatements = arrayOf<AstNodeType>(DmlGrammar.SELECT_EXPRESSION, PlSqlGrammar.UPDATE_STATEMENT, PlSqlGrammar.DELETE_STATEMENT)

    override fun init() {
        subscribeTo(*dmlStatements)
    }

    override fun visitNode(node: AstNode) {
        if (node.hasAncestor(*dmlStatements)) {
            // if the current node is inside another DML statement (i.e. subquery), the node should be
            // ignored because it is considered in the analysis of the outer statement
            return
        }

        val tableReferences = ArrayListMultimap.create<String, TableReference>()
        for (fromClause in node.getDescendants(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)) {
            val table = fromClause.getFirstChild(DmlGrammar.TABLE_REFERENCE)
            val alias = fromClause.getFirstChild(DmlGrammar.ALIAS)


            if (table != null) {
                tableReferences.put(table.tokenOriginalValue.toLowerCase(),
                        TableReference(table, alias))
            }
        }

        for (tableName in tableReferences.keySet()) {
            val references = tableReferences.get(tableName)
            checkReference(references)
        }
    }

    private fun checkReference(references: List<TableReference>) {
        if (references.size == 1) {
            val reference: TableReference = references[0]

            var alias: String? = null
            if (reference.alias != null) {
                alias = reference.alias.tokenOriginalValue
            }

            if (alias != null && reference.alias != null && alias.length < acceptedLength) {
                addIssue(reference.alias, getLocalizedMessage(CHECK_KEY),
                        reference.table.tokenOriginalValue,
                        reference.alias.tokenOriginalValue)
            }
        }
    }

    internal inner class TableReference(val table: AstNode, val alias: AstNode?)

    companion object {
        const val CHECK_KEY = "UnnecessaryAliasInQuery"
        private const val DEFAULT_ACCEPTED_LENGTH = 3
    }

}
