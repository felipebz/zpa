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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.OracleIdentifier
import java.util.*

/**
 * The relations of one query block or DML statement that can qualify a SQL name, as AST-detached facts: the aliases,
 * and the names of tables written without an alias. A table that has an alias is reachable only through it. [parent]
 * is the scope a subquery sees too (correlation): the enclosing query or statement, except for an inline view in
 * `FROM` without `LATERAL`/`APPLY` and the query of `INSERT … SELECT`, which see only the scope above it.
 */
internal class LocalSqlScope(
    val relations: Set<OracleIdentifier>,
    /** Tables written without an alias: `schema.table.column` reaches them. */
    val unaliasedTables: Set<OracleIdentifier>,
    val parent: LocalSqlScope?,
    /**
     * Every relation is the `DUAL` table (`DUAL` or `SYS.DUAL`, not a `WITH` query), whose only column is `DUMMY`. An
     * unqualified `DUAL` is taken as `SYS.DUAL`: a table named `DUAL` in the owner's schema would hide it.
     */
    val dualOnly: Boolean = false
) {
    /** Whether no relation this scope or an enclosing one sees can have a column named [name]. */
    fun excludesColumn(name: OracleIdentifier): Boolean {
        var scope: LocalSqlScope? = this
        while (scope != null) {
            if (!scope.dualOnly || name.lookupName == "DUMMY") return false
            scope = scope.parent
        }
        return true
    }

    /** Whether a name with these [segments] after [first] qualifies a column or object path of a relation. */
    fun qualifies(first: OracleIdentifier, segments: List<LocalSegment>): Boolean {
        var scope: LocalSqlScope? = this
        while (scope != null) {
            if (first in scope.relations) return true
            if (segments.size >= 2 && !segments[0].isAttribute && segments[0].name in scope.unaliasedTables) return true
            scope = scope.parent
        }
        return false
    }

    override fun toString() = "${relations.joinToString(",")}${parent?.let { " < $it" } ?: ""}"

    companion object {
        /** Regions whose functions are stored, so SQL can call them. */
        private val STORED = EnumSet.of(LocalRegionKind.PACKAGE_SPEC, LocalRegionKind.OBJECT_TYPE, LocalRegionKind.TYPE_BODY)

        /** A built-in of the same name wins over a local variable or parameter called with arguments in SQL. */
        fun calledValue(declaration: LocalDeclaration): LocalResolution? =
            if (declaration.kind == LocalDeclarationKind.VARIABLE || declaration.kind == LocalDeclarationKind.PARAMETER) {
                LocalResolution.SqlBuiltinOrLocal(listOf(declaration.id))
            } else null

        /**
         * Callables named by a call in SQL or a single-row `VALUES` name. A procedure fails (ORA-03066). A function
         * that is not stored is PLS-00231. A stored one is accepted: called with arguments a SQL built-in of the
         * same name wins over it; a parameterless name binds it.
         */
        fun callables(
            model: LocalSemanticModel,
            entities: List<List<LocalDeclaration>>,
            representatives: List<LocalDeclarationId>,
            context: LocalReferenceContext
        ): LocalResolution {
            val functions = entities.filter { it.first().kind == LocalDeclarationKind.FUNCTION }
            if (functions.isEmpty()) return LocalResolution.WrongKind(representatives.first())
            val stored = functions.any { entity -> entity.any { model.region(it.region).kind in STORED } }
            return when {
                !stored -> LocalResolution.LocalFunctionInSql(representatives)
                context == LocalReferenceContext.SQL_CALL -> LocalResolution.SqlBuiltinOrLocal(representatives)
                else -> LocalResolution.OverloadSet(representatives)
            }
        }

        /**
         * The relations [node] introduces: the `FROM` items of a `QUERY_BLOCK`, or the target (and `USING`/`FROM`
         * items) of an `INSERT`, `UPDATE`, `DELETE` or `MERGE`. Subqueries are not entered.
         */
        fun of(node: AstNode, parent: LocalSqlScope?): LocalSqlScope {
            val relations = LinkedHashSet<OracleIdentifier>()
            val tables = LinkedHashSet<OracleIdentifier>()
            var dualOnly = true
            fun add(table: AstNode?, alias: AstNode?) {
                if (table == null || !isDual(table)) dualOnly = false
                val name = alias ?: table?.let(::tableName) ?: return
                relations += LocalSyntax.identifier(name)
                if (alias == null) tables += LocalSyntax.identifier(name)
            }
            fun walk(current: AstNode) {
                when (current.type) {
                    DmlGrammar.SELECT_EXPRESSION -> return
                    DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE -> {
                        val values = current.getFirstChildOrNull(DmlGrammar.VALUES_EXPRESSION_CLAUSE)
                        if (values != null) add(null, values.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME))
                        else add(current.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE), current.getFirstChildOrNull(DmlGrammar.ALIAS)?.firstChild)
                    }
                    DmlGrammar.INSERT_INTO_CLAUSE ->
                        add(current.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE), current.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME))
                    else -> current.children.forEach(::walk)
                }
            }
            if (node.type === DmlGrammar.QUERY_BLOCK) node.getFirstChildOrNull(DmlGrammar.FROM_CLAUSE)?.children?.forEach(::walk)
            else {
                if (node.type === DmlGrammar.MERGE_EXPRESSION) {
                    add(node.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE), node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME))
                }
                node.children.forEach(::walk)
            }
            return LocalSqlScope(relations, tables, parent, relations.isNotEmpty() && dualOnly && !dualIsWithQuery(node))
        }

        private fun isDual(reference: AstNode): Boolean {
            if (reference.type !== DmlGrammar.TABLE_REFERENCE || reference.hasDescendant(PlSqlPunctuator.REMOTE)) return false
            val names = reference.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { LocalSyntax.identifier(it).lookupName }
            return names == listOf("DUAL") || names == listOf("SYS", "DUAL")
        }

        /** Whether a `WITH` query named `DUAL` is visible from [node], hiding the table. */
        private fun dualIsWithQuery(node: AstNode): Boolean =
            generateSequence(node) { it.parentOrNull }
                .filter { it.type === DmlGrammar.SELECT_EXPRESSION }
                .any { select ->
                    select.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE).any {
                        it.getFirstAncestorOrNull(DmlGrammar.SELECT_EXPRESSION) === select &&
                            it.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let { name -> LocalSyntax.identifier(name).lookupName } == "DUAL"
                    }
                }

        /** The table name of a `TABLE_REFERENCE`: its last identifier before a database link. */
        private fun tableName(reference: AstNode): AstNode? =
            reference.children.takeWhile { it.type !== PlSqlPunctuator.REMOTE }.lastOrNull { it.type === PlSqlGrammar.IDENTIFIER_NAME }
    }
}
