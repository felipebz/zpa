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
 * | first child of `MEMBER_EXPRESSION` or `OBJECT_REFERENCE` | QUALIFIER, with the segments after it |
 * | first child of a `METHOD_CALL` that is a `CALL_STATEMENT`, or a `CALL_STATEMENT` name | CALL_STATEMENT |
 * | first child of any other `METHOD_CALL` | CALL_EXPRESSION |
 * | child of `RAISE_STATEMENT` or `EXCEPTION_HANDLER`, first child of `EXCEPTION_INIT_PRAGMA` | EXCEPTION |
 * | inside `SELECT_`/`INSERT_`/`UPDATE_`/`DELETE_`/`MERGE_EXPRESSION`, outside an `INTO_CLAUSE` | SQL_VALUE |
 * | anything else | VALUE |
 * | single-segment `NamedTypeRef`/`RefTypeRef` of a type use | TYPE |
 * | single-segment `AnchoredTypeRef` of a type use | ANCHOR_TYPE / ANCHOR_ROWTYPE |
 * | multi-segment type name of a type use | QUALIFIER |
 *
 * Not collected: the cursor operand of `OPEN`, `FETCH`, `CLOSE` and `OPEN … FOR`; the single expression of a `FOR`
 * iteration control (a cursor or a value); call names and qualified names inside SQL; columns of `INSERT (…)` and
 * `SET` targets; names inside datatypes other than the model's type uses (constraints, `CAST`); names behind a database
 * link; the name of `PRAGMA DEPRECATE`; names of a trigger heading (`WHEN`, `CALL`); labels of `EXIT`, `GOTO` and
 * `END name` (not `VARIABLE_NAME`s); bind and host variables; dynamic SQL text.
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
        Walk(model, references).visit(root, State(inSql = false, inDefault = false, inTrigger = false))
        typeUses(model, references)
        references.sortWith(LocalReference.ORDER)
        return references
    }

    private class State(val inSql: Boolean, val inDefault: Boolean, val inTrigger: Boolean)

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
            }
            var next = state
            when {
                type in SQL_STATEMENTS -> next = State(true, state.inDefault, state.inTrigger)
                type === DmlGrammar.INTO_CLAUSE -> next = State(false, state.inDefault, state.inTrigger)
                type === PlSqlGrammar.CREATE_TRIGGER -> next = State(state.inSql, state.inDefault, true)
                type === PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT && (node.parent.type === PlSqlGrammar.PARAMETER_DECLARATION ||
                    node.parent.type === PlSqlGrammar.CURSOR_PARAMETER_DECLARATION) -> next = State(state.inSql, true, state.inTrigger)
            }
            for ((index, child) in node.children.withIndex()) {
                if (index == 0 && type === DmlGrammar.UPDATE_COLUMN) continue
                visit(child, next)
            }
        }

        private fun reference(node: AstNode, state: State) {
            val identifier = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME) ?: return
            val parent = node.parent
            val first = parent.firstChild === node
            var remaining = 0
            val context = when {
                parent.type === PlSqlGrammar.MEMBER_EXPRESSION || parent.type === PlSqlGrammar.OBJECT_REFERENCE -> {
                    if (!first || state.inSql || parent.hasDirectChildren(PlSqlPunctuator.REMOTE)) return
                    remaining = parent.children.count { it.type === PlSqlPunctuator.DOT || it.type === PlSqlPunctuator.MOD }
                    LocalReferenceContext.QUALIFIER
                }
                parent.type === PlSqlGrammar.METHOD_CALL && first -> {
                    if (state.inSql || parent.parent.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL) return
                    if (parent.parent.type === PlSqlGrammar.CALL_STATEMENT) LocalReferenceContext.CALL_STATEMENT
                    else LocalReferenceContext.CALL_EXPRESSION
                }
                parent.type === PlSqlGrammar.CALL_STATEMENT -> LocalReferenceContext.CALL_STATEMENT
                parent.type === PlSqlGrammar.EXCEPTION_INIT_PRAGMA -> {
                    // Only the first argument names the exception; the error code is an expression.
                    if (parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME) !== node) LocalReferenceContext.VALUE
                    else LocalReferenceContext.EXCEPTION
                }
                parent.type === PlSqlGrammar.RAISE_STATEMENT || parent.type === PlSqlGrammar.EXCEPTION_HANDLER ->
                    LocalReferenceContext.EXCEPTION
                parent.type in CURSOR_STATEMENTS && parent.getFirstChild(PlSqlGrammar.VARIABLE_NAME) === node -> return
                parent.type === PlSqlGrammar.SINGLE_EXPRESSION_CONTROL -> return
                state.inSql -> LocalReferenceContext.SQL_VALUE
                else -> LocalReferenceContext.VALUE
            }
            val range = LocalSyntax.range(fileId, identifier)
            val region = innermostRegion(model, range)
            // A trigger heading (WHEN, CALL) is SQL-level and names correlation rows, not PL/SQL declarations.
            if (state.inTrigger && model.region(region).kind == LocalRegionKind.FILE) return
            out += LocalReference(LocalSyntax.identifier(identifier), range, context, region, remaining, state.inDefault)
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
            out += LocalReference(segments.first(), use.range, actual, use.lookupRegion, segments.size - 1, false)
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
