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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.*

/**
 * Collects the name lookups of one parsed file as detached [LocalReference]s, sorted by range. It is a separate walk
 * over the tree the model was built from: names written in expressions and statements come from the AST, names of
 * written datatypes come from the model's [LocalTypeUse]s. Each AST reference is placed in the innermost region of the
 * model containing its start.
 *
 * Grammar shapes and contexts (each name is the `IDENTIFIER_NAME` of a `VARIABLE_NAME`; a `VARIABLE_NAME` holding a
 * bind variable is not a lookup):
 *
 * | Shape | Context |
 * |---|---|
 * | first child of `MEMBER_EXPRESSION` or `OBJECT_REFERENCE` | QUALIFIER, with the `.`/`%` segments after it; the whole name's own position (below) is the last segment's context |
 * | first child of a `METHOD_CALL` that is a `CALL_STATEMENT`, or a `CALL_STATEMENT` name | CALL_STATEMENT |
 * | first child of a `METHOD_CALL` that is a `SINGLE_EXPRESSION_CONTROL`, or a direct child of one | FOR_CONTROL |
 * | first child of any other `METHOD_CALL` | CALL_EXPRESSION |
 * | child of `RAISE_STATEMENT` or `EXCEPTION_HANDLER`, first child of `EXCEPTION_INIT_PRAGMA` | EXCEPTION |
 * | first name of `OPEN_STATEMENT`, `FETCH_STATEMENT`, `CLOSE_STATEMENT` | CURSOR_OPERAND |
 * | first name of `OPEN_FOR_STATEMENT` | OPEN_FOR_OPERAND |
 * | `IDENTIFIER_NAME` of `GOTO_STATEMENT` | GOTO_TARGET |
 * | `IDENTIFIER_NAME` of `EXIT_STATEMENT`, `CONTINUE_STATEMENT` | LOOP_TARGET |
 * | first child of a `METHOD_CALL` inside SQL or a single-table `VALUES` | SQL_CALL |
 * | in the `VALUES` of a single-table `INSERT`, outside a subquery | INSERT_VALUE |
 * | inside `SELECT_`/`INSERT_`/`UPDATE_`/`DELETE_`/`MERGE_EXPRESSION`, outside an `INTO_CLAUSE` | SQL_VALUE |
 * | anything else | VALUE |
 * | single-segment `NamedTypeRef`/`RefTypeRef` of a type use | TYPE |
 * | single-segment `AnchoredTypeRef` of a type use | ANCHOR_TYPE / ANCHOR_ROWTYPE |
 * | multi-segment type name of a type use | QUALIFIER, last segment TYPE / ANCHOR_TYPE / ANCHOR_ROWTYPE |
 *
 * A name inside SQL carries the [LocalSqlScope] of its query block or statement, whose relations can qualify it.
 *
 * Not collected: columns of `INSERT (…)` and `SET` targets; names inside
 * datatypes other than the model's type uses (constraints, `CAST`); names behind a database link; the name of
 * `PRAGMA DEPRECATE`; names of a trigger heading (`WHEN`, `CALL`); `END name` trailers, which Oracle matches against the
 * unit or label without looking them up; bind and host variables; dynamic SQL text.
 */
internal object LocalReferenceCollector {

    private val SQL_STATEMENTS = setOf(
        DmlGrammar.SELECT_EXPRESSION, DmlGrammar.INSERT_EXPRESSION, DmlGrammar.UPDATE_EXPRESSION,
        DmlGrammar.DELETE_EXPRESSION, DmlGrammar.MERGE_EXPRESSION
    )
    private val CURSOR_STATEMENTS = setOf(
        PlSqlGrammar.OPEN_STATEMENT, PlSqlGrammar.FETCH_STATEMENT, PlSqlGrammar.CLOSE_STATEMENT, PlSqlGrammar.OPEN_FOR_STATEMENT
    )

    fun collect(root: AstNode, model: LocalSemanticModel): List<LocalReference> {
        val references = ArrayList<LocalReference>()
        Walk(model, references).visit(root, State(inSql = false, inValues = false, sql = null, inDefault = false, inTrigger = false, handler = null))
        typeUses(model, references)
        references.sortWith(LocalReference.ORDER)
        return references
    }

    private class State(
        val inSql: Boolean,
        /** In the `VALUES` of a single-table `INSERT`, outside a subquery. */
        val inValues: Boolean,
        val sql: LocalSqlScope?,
        val inDefault: Boolean,
        val inTrigger: Boolean,
        val handler: SourceRange?
    ) {
        fun with(
            inSql: Boolean = this.inSql,
            inValues: Boolean = this.inValues,
            sql: LocalSqlScope? = this.sql,
            inDefault: Boolean = this.inDefault,
            inTrigger: Boolean = this.inTrigger,
            handler: SourceRange? = this.handler
        ) = State(inSql, inValues, sql, inDefault, inTrigger, handler)
    }

    /** Children of a single-table `INSERT` that are not its `VALUES` rows. */
    private val NOT_VALUES = setOf(
        DmlGrammar.INSERT_INTO_CLAUSE, DmlGrammar.SELECT_EXPRESSION, DmlGrammar.INSERT_SET_CLAUSE,
        DmlGrammar.RETURNING_INTO_CLAUSE, DmlGrammar.ERROR_LOGGING_CLAUSE
    )

    private class Walk(private val model: LocalSemanticModel, private val out: MutableList<LocalReference>) {
        private val fileId: FileId = model.fileId

        fun visit(node: AstNode, state: State) {
            val type = node.type
            when {
                type === PlSqlGrammar.VARIABLE_NAME -> {
                    reference(node, state)
                    return
                }
                type === PlSqlGrammar.DATATYPE || type === PlSqlGrammar.DEPRECATE_PRAGMA ||
                    type === DmlGrammar.INSERT_COLUMNS -> return
                type === PlSqlGrammar.GOTO_STATEMENT -> labelTarget(node, LocalReferenceContext.GOTO_TARGET, state)
                type === PlSqlGrammar.EXIT_STATEMENT || type === PlSqlGrammar.CONTINUE_STATEMENT ->
                    labelTarget(node, LocalReferenceContext.LOOP_TARGET, state)
            }
            var next = state
            when {
                type === DmlGrammar.SELECT_EXPRESSION ->
                    next = state.with(inSql = true, inValues = false, sql = if (skipsEnclosing(node)) state.sql?.parent else state.sql)
                type in SQL_STATEMENTS -> next = state.with(inSql = true, inValues = false, sql = LocalSqlScope.of(node, state.sql))
                type === DmlGrammar.QUERY_BLOCK -> next = state.with(sql = LocalSqlScope.of(node, state.sql))
                type === DmlGrammar.INTO_CLAUSE -> next = state.with(inSql = false, sql = null)
                type === PlSqlGrammar.CREATE_TRIGGER -> next = state.with(inTrigger = true)
                type === PlSqlGrammar.EXCEPTION_HANDLER -> next = state.with(handler = LocalSyntax.range(fileId, node))
                type === PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT && (node.parent.type === PlSqlGrammar.PARAMETER_DECLARATION ||
                    node.parent.type === PlSqlGrammar.CURSOR_PARAMETER_DECLARATION) -> next = state.with(inDefault = true)
            }
            for ((index, child) in node.children.withIndex()) {
                if (index == 0 && type === DmlGrammar.UPDATE_COLUMN) continue
                // Single-row VALUES have no column scope (the names are PL/SQL), but are still evaluated by SQL.
                val values = type === DmlGrammar.SINGLE_TABLE_INSERT && child.type !in NOT_VALUES
                visit(child, if (values) next.with(inSql = false, inValues = true, sql = null) else next)
            }
        }

        /**
         * A query that does not see the relations of the query or statement holding it: an inline view in `FROM`
         * without `LATERAL` or `APPLY`, and the query of a single-table `INSERT … SELECT`.
         */
        private fun skipsEnclosing(query: AstNode): Boolean {
            val parent = query.parent
            return when (parent.type) {
                DmlGrammar.SINGLE_TABLE_INSERT -> true
                DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE ->
                    !parent.hasDirectChildren(PlSqlKeyword.LATERAL) && parent.parent.type !== DmlGrammar.CROSS_OUTER_APPLY_CLAUSE
                else -> false
            }
        }

        /** The label named by `GOTO`, `EXIT` or `CONTINUE` (an `IDENTIFIER_NAME`, absent for a plain `EXIT`). */
        private fun labelTarget(statement: AstNode, context: LocalReferenceContext, state: State) {
            val identifier = statement.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return
            add(identifier, context, emptyList(), context, state)
        }

        private fun reference(node: AstNode, state: State) {
            val identifier = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return
            val parent = node.parent
            if (parent.type === PlSqlGrammar.MEMBER_EXPRESSION || parent.type === PlSqlGrammar.OBJECT_REFERENCE) {
                if (parent.firstChild !== node || parent.hasDirectChildren(PlSqlPunctuator.REMOTE)) return
                val whole = position(parent, state) ?: return
                add(identifier, LocalReferenceContext.QUALIFIER, segments(parent), whole, state)
                return
            }
            val context = position(node, state) ?: return
            add(identifier, context, emptyList(), context, state)
        }

        /** The context of a name written as [node] (a `VARIABLE_NAME` or a qualified name), or null when not collected. */
        private fun position(node: AstNode, state: State): LocalReferenceContext? {
            val parent = node.parent
            val first = parent.firstChild === node
            return when {
                parent.type === PlSqlGrammar.METHOD_CALL && first -> when {
                    state.inSql || state.inValues -> LocalReferenceContext.SQL_CALL
                    parent.parent.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL -> LocalReferenceContext.FOR_CONTROL
                    parent.parent.type === PlSqlGrammar.CALL_STATEMENT -> LocalReferenceContext.CALL_STATEMENT
                    else -> LocalReferenceContext.CALL_EXPRESSION
                }
                parent.type === PlSqlGrammar.CALL_STATEMENT -> LocalReferenceContext.CALL_STATEMENT
                parent.type === PlSqlGrammar.EXCEPTION_INIT_PRAGMA ->
                    // Only the first argument names the exception; the error code is an expression.
                    if (parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) !== node) LocalReferenceContext.VALUE
                    else LocalReferenceContext.EXCEPTION
                parent.type === PlSqlGrammar.RAISE_STATEMENT || parent.type === PlSqlGrammar.EXCEPTION_HANDLER ->
                    LocalReferenceContext.EXCEPTION
                parent.type in CURSOR_STATEMENTS && parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) === node ->
                    if (parent.type === PlSqlGrammar.OPEN_FOR_STATEMENT) LocalReferenceContext.OPEN_FOR_OPERAND else LocalReferenceContext.CURSOR_OPERAND
                parent.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL -> LocalReferenceContext.FOR_CONTROL
                state.inValues -> LocalReferenceContext.INSERT_VALUE
                state.inSql -> LocalReferenceContext.SQL_VALUE
                else -> LocalReferenceContext.VALUE
            }
        }

        /** The segments written after the first child of a qualified name: `.name` members and `%name` attributes. */
        private fun segments(qualified: AstNode): List<LocalSegment> {
            val result = ArrayList<LocalSegment>()
            val children = qualified.children
            for (index in 1 until children.size - 1) {
                val separator = children[index].type
                if (separator !== PlSqlPunctuator.DOT && separator !== PlSqlPunctuator.MOD) continue
                val name = children[index + 1]
                if (!name.hasToken()) continue
                result += LocalSegment(LocalSyntax.identifier(name), separator === PlSqlPunctuator.MOD)
            }
            return result
        }

        private fun add(
            identifier: AstNode,
            context: LocalReferenceContext,
            segments: List<LocalSegment>,
            whole: LocalReferenceContext,
            state: State
        ) {
            val range = LocalSyntax.range(fileId, identifier)
            val region = innermostRegion(model, range)
            // A trigger heading (WHEN, CALL) is SQL-level and names correlation rows, not PL/SQL declarations.
            if (state.inTrigger && model.region(region).kind == LocalRegionKind.FILE) return
            out += LocalReference(
                LocalSyntax.identifier(identifier), range, context, region, segments, whole, state.inDefault, state.handler,
                if (state.inSql) state.sql else null
            )
        }
    }

    /** One reference per written datatype of the model (deduplicated: a body's parameter types appear twice). */
    private fun typeUses(model: LocalSemanticModel, out: MutableList<LocalReference>) {
        val seen = HashSet<SourceRange>()
        fun add(use: LocalTypeUse?) {
            val typeRef = use?.typeRef ?: return
            if (!seen.add(use.range)) return
            val segments = typeRef.name.segments
            val context = when (typeRef) {
                is NamedTypeRef, is RefTypeRef -> LocalReferenceContext.TYPE
                is AnchoredTypeRef -> {
                    if (typeRef.link != null) return // a remote object, not a local name
                    if (typeRef.anchor == TypeAnchor.TYPE) LocalReferenceContext.ANCHOR_TYPE else LocalReferenceContext.ANCHOR_ROWTYPE
                }
            }
            val actual = if (segments.size > 1) LocalReferenceContext.QUALIFIER else context
            val rest = segments.drop(1).map { LocalSegment(it, false) }
            out += LocalReference(segments.first(), use.range, actual, use.lookupRegion, rest, context, false, null)
        }
        for (declaration in model.declarations) {
            add(declaration.type)
            when (val details = declaration.details) {
                is LocalSubprogram -> {
                    details.parameters.forEach { add(it.type) }
                    add(details.returnType)
                }
                is LocalParameter -> add(details.type)
                is LocalCursor -> add(details.returnType)
                is LocalRecordType -> details.fields.forEach { add(it.type) }
                is LocalCollectionType -> add(details.indexType)
                is LocalRefCursorType -> add(details.returnType)
                else -> Unit
            }
        }
    }

    /** The innermost region of [model] containing the start of [range]. Regions nest and children are in start order. */
    fun innermostRegion(model: LocalSemanticModel, range: SourceRange): LocalRegionId {
        var current = model.root
        while (true) {
            val children = current.children
            var low = 0
            var high = children.size - 1
            var candidate: LocalRegion? = null
            while (low <= high) {
                val middle = (low + high) ushr 1
                val child = model.region(children[middle])
                if (compare(child.range.startLine, child.range.startColumn, range.startLine, range.startColumn) <= 0) {
                    candidate = child
                    low = middle + 1
                } else {
                    high = middle - 1
                }
            }
            val inside = candidate != null &&
                compare(range.startLine, range.startColumn, candidate.range.endLine, candidate.range.endColumn) < 0
            if (!inside) return current.id
            current = candidate!!
        }
    }

    private fun compare(line: Int, column: Int, otherLine: Int, otherColumn: Int) =
        if (line != otherLine) line.compareTo(otherLine) else column.compareTo(otherColumn)
}
