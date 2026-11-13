//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.dynamicsql.DynamicSqlHole
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName
import java.util.*

/** A dynamic SQL text the column walk answers, and whether a hole's expression of the running file is a number. */
internal class LocalDynamicText(val text: DynamicSqlText, val isNumber: (AstNode) -> Boolean) {
    /**
     * The text is a SQL statement, with no PL/SQL in it: no PL/SQL name takes a name of its SQL. A PL/SQL block (or a
     * `WITH` function) declares names its static SQL sees, as in a file.
     */
    val isSqlStatement: Boolean by lazy(LazyThreadSafetyMode.NONE) { !text.root.hasDescendant(PlSqlGrammar.STATEMENTS_SECTION) }
}

/**
 * What the names of the file's static SQL read, as Oracle resolves them: the sources each SQL scope owns, an inner scope
 * before an outer one, a column of a source before a PL/SQL name, and a source known to lack the column passing the
 * name outward and then to PL/SQL. The scopes follow those `nameBinding` uses: an inline view without `LATERAL` and
 * the query of `INSERT … SELECT` do not see the scope that holds them.
 *
 * Over a dynamic SQL text ([dynamic]) the same walk answers the text's names, with its statement as the outermost
 * scope: Oracle compiles the text on its own, so no PL/SQL name of the code that runs it takes one. A text that is a
 * SQL statement has no PL/SQL name at all; in a text that is a PL/SQL block, the block's declarations take names as
 * in a file.
 */
internal class LocalColumnBindings(
    private val model: LocalSemanticModel,
    private val walk: LocalTypeWalk,
    private val nameBinding: (AstNode) -> NameBinding,
    private val dynamic: LocalDynamicText? = null
) {
    private class Source(
        val source: ColumnSource,
        /** The alias, else the relation or `WITH` name; null for an inline view or a table expression without alias. */
        val qualifier: OracleIdentifier?,
        /** A relation's name as written, without a database link. */
        val written: QualifiedName?,
        /** The query of an inline view or a `WITH` name. */
        val query: AstNode?,
        /** The column list of a `WITH` name. */
        val listed: List<OracleIdentifier>?
    )

    /** The columns an inline view or `WITH` query gives, by lookup name, each with its select item when known. */
    private class QueryNames(val items: Map<String, AstNode?>, val complete: Boolean, val starUncovered: Boolean)

    private val scopeSources = IdentityHashMap<AstNode, List<Source>>()
    private val queryNames = IdentityHashMap<Source, QueryNames>()

    fun binding(name: AstNode): ColumnBinding {
        if (name.type !== PlSqlGrammar.VARIABLE_NAME && name.type !== PlSqlGrammar.MEMBER_EXPRESSION) return unavailable()
        val parent = name.parent
        if (name.type === PlSqlGrammar.VARIABLE_NAME && parent.type === PlSqlGrammar.MEMBER_EXPRESSION) return unavailable()
        val start = startScope(name) ?: return unavailable()
        if (name.getFirstAncestorOrNull(DmlGrammar.INTO_CLAUSE)?.let { isBelow(it, start) } == true) return ColumnBinding.PlSqlName(PlSqlNameReason.INTO_TARGET)
        if (isBindVariable(name)) return ColumnBinding.PlSqlName(PlSqlNameReason.BIND_VARIABLE)
        dynamicHole(name)?.let { return it }
        hidden(start)?.let { return it }
        if (parent.type === PlSqlGrammar.OBJECT_REFERENCE && parent.firstChild === name) objectReference(name, parent, start)?.let { return it }
        val segments = segments(name) ?: return unavailable()
        correlationArgument(name, start)?.let { return it }
        writtenColumn(name, start, segments.last())?.let { return it }
        if (parent.type === PlSqlGrammar.METHOD_CALL && parent.firstChild === name) return methodCallee(name, start, segments)
        if (segments.size == 1) return unqualified(name, start, segments.single())
        return qualifiedName(name, start, segments)
    }

    /**
     * The first part of an object reference: the qualifier of `t.*`; `t.col(+)` of an outer join is the name `t.col`
     * (null: answered as a name); a member of anything else (`t.col.f(…)`) is not a name.
     */
    private fun objectReference(name: AstNode, reference: AstNode, start: AstNode): ColumnBinding? = when {
        reference.lastChild.type === PlSqlPunctuator.MULTIPLICATION -> starQualifier(name, start)
        reference.children.size == 2 && reference.lastChild.type === PlSqlGrammar.OUTER_JOIN_PLUS_SIGN -> null
        else -> unavailable()
    }

    /** `q.col.method(…)`: a method of a column; `f(…)` and `q.f(…)` are calls. */
    private fun methodCallee(name: AstNode, start: AstNode, segments: List<OracleIdentifier>): ColumnBinding {
        if (segments.size < 3) return unavailable()
        return when (val capture = qualified(name, start, segments.dropLast(1))) {
            is Capture.Found -> capture.binding
            // A PL/SQL call may take the whole name (`schema.pkg.f`, `pkg.v.method`); in a SQL statement text none does.
            is Capture.Lacking -> if (dynamic?.isSqlStatement == true) columnOf(capture.source, capture.column, false) else plSqlNameUndecided(capture)
            null -> unavailable()
        }
    }

    private fun qualifiedName(name: AstNode, start: AstNode, segments: List<OracleIdentifier>): ColumnBinding {
        val last = segments.last()
        val capture = qualified(name, start, segments)
        // `seq.NEXTVAL`, unless a source the qualifier names has a column of that name.
        if (segments.size <= 3 && !last.quoted && last.lookupName in SEQUENCE_PSEUDOCOLUMNS &&
            (capture as? Capture.Found)?.binding.let { it !is ColumnBinding.Column || it.exists != true }
        ) return ColumnBinding.Pseudocolumn
        return when (capture) {
            is Capture.Found -> capture.unverified?.let { unverified(name, capture.binding, it, segments) } ?: capture.binding
            is Capture.Lacking -> lacking(name, capture, segments)
            null -> ColumnBinding.PlSqlName(PlSqlNameReason.QUALIFIER_NOT_A_SOURCE)
        }
    }

    /**
     * A qualified name whose source's columns are not known: the column [column] when the source has it, else [rest],
     * what the same walk reads were the source to lack it (a source of an enclosing scope with that qualifier, else
     * [lacking]). When that is the missing column of the innermost source, the name is that column either way; else it
     * is undecided, with the inner column, then each outer column the walk reaches, from the inside out.
     */
    private fun unverified(name: AstNode, column: ColumnBinding, rest: Capture, segments: List<OracleIdentifier>): ColumnBinding {
        val innerSource = (column as? ColumnBinding.Column)?.source ?: (column as? ColumnBinding.QueryColumn)?.source
        val reasons = EnumSet.noneOf(ColumnUncertainty::class.java)
        val candidates = arrayListOf(column)
        var next: Capture? = rest
        while (next != null) {
            when (next) {
                is Capture.Found -> {
                    candidates += next.binding
                    next.unverified?.let { reasons += ColumnUncertainty.SOURCE_COLUMNS_UNKNOWN }
                    next = next.unverified
                }
                is Capture.Lacking -> {
                    val otherwise = lacking(name, next, segments)
                    // A missing column is Oracle rejecting the statement, not a reading of the name. A PL/SQL name the
                    // analysis only cannot exclude (a schema object outside the project) weighs no more than it does
                    // for an unqualified name: only a declaration zpa knows is another reading.
                    if (otherwise !is ColumnBinding.PlSqlName && candidates.size == 1) return column
                    next = null
                }
            }
        }
        if (innerSource != null) reasons += ColumnUncertainty.SOURCE_COLUMNS_UNKNOWN
        return ColumnBinding.Unknown(reasons, candidates)
    }

    /**
     * A qualified name whose source lacks the column. Oracle then reads it as a PL/SQL name: a declaration of the file
     * that takes the first name (as `nameBinding` answers), else a package variable or constant (`pk.v`, `pk.v.f`,
     * `schema.pk.v`; a function of that name does not take it). It is the missing column, which Oracle rejects, only
     * when the project proves that neither can. A dynamic text that is a SQL statement sees no PL/SQL name: always the
     * missing column.
     */
    private fun lacking(name: AstNode, capture: Capture.Lacking, segments: List<OracleIdentifier>): ColumnBinding {
        if (dynamic?.isSqlStatement == true) return columnOf(capture.source, capture.column, false)
        when (val first = nameBinding(firstName(name))) {
            is NameBinding.Local -> return declared(name, capture, first.declaration)
            is NameBinding.ColumnOrLocal -> return declared(name, capture, first.declaration)
            is NameBinding.NothingLocal -> Unit
            else -> return plSqlNameUndecided(capture)
        }
        val region = region(name)
        // `schema.t.col`: with no object named like the schema, its first name is the schema, where `t` is the relation.
        val taken = walk.packageValueTakes(segments, region)
            ?: false.takeIf { capture.index == 2 && segments.size == 3 && walk.provesNoObjectNamed(segments[0], region) }
        return when (taken) {
            true -> ColumnBinding.PlSqlName(PlSqlNameReason.NO_SOURCE_HAS_IT)
            false -> columnOf(capture.source, capture.column, false)
            null -> plSqlNameUndecided(capture)
        }
    }

    /** A PL/SQL name may take the name the source [capture] names lacks; else Oracle reads that missing column. */
    private fun plSqlNameUndecided(capture: Capture.Lacking): ColumnBinding =
        ColumnBinding.Unknown(EnumSet.of(ColumnUncertainty.PLSQL_NAME_UNDECIDED), listOf(columnOf(capture.source, capture.column, false)))

    /**
     * A qualified name whose source lacks the column, and whose first name a declaration of the file takes. A value
     * takes the whole name. A unit or block that names its members (`pk.v`, `fn.p`) takes it only through a variable,
     * constant or parameter; a function there does not, and Oracle rejects the statement.
     */
    private fun declared(name: AstNode, capture: Capture.Lacking, declaration: NameDeclaration): ColumnBinding {
        val plsql = ColumnBinding.PlSqlName(PlSqlNameReason.NO_SOURCE_HAS_IT)
        if (declaration.kind !in NAMING_UNITS) return plsql
        val member = name.children.getOrNull(2)?.takeIf { it.type === PlSqlGrammar.IDENTIFIER_NAME }
            ?: return plSqlNameUndecided(capture)
        val kind = when (val binding = nameBinding(member)) {
            is NameBinding.Local -> binding.declaration.kind
            is NameBinding.ColumnOrLocal -> binding.declaration.kind
            is NameBinding.NothingLocal -> return columnOf(capture.source, capture.column, false)
            else -> null
        }
        return when (kind) {
            NameDeclarationKind.VARIABLE, NameDeclarationKind.PARAMETER, NameDeclarationKind.LOOP_INDEX -> plsql
            NameDeclarationKind.FUNCTION, NameDeclarationKind.PROCEDURE -> columnOf(capture.source, capture.column, false)
            else -> plSqlNameUndecided(capture)
        }
    }

    private sealed interface Capture {
        /**
         * The source takes the name; [unverified] when its columns are not known: what the name reads were the source
         * to lack the column.
         */
        class Found(val binding: ColumnBinding, val unverified: Capture? = null) : Capture

        /** The innermost source the qualifier names is known to lack [column], named at segment [index]. */
        class Lacking(val source: Source, val column: OracleIdentifier, val index: Int) : Capture
    }

    /**
     * `q.col`, `schema.t.col`: the column of the first source, from the innermost scope out, whose qualifier matches and
     * that is not known to lack it; null when no qualifier matches. A source whose columns are not known takes it with
     * what the rest of the walk reads were it to lack the column, else [lackingIn] or its own missing column.
     */
    private fun qualified(name: AstNode, start: AstNode?, names: List<OracleIdentifier>, lackingIn: Capture.Lacking? = null): Capture? {
        var lacking: Capture.Lacking? = lackingIn
        var scope: AstNode? = start
        while (scope != null) {
            for (source in sourcesFor(scope, name, qualified = true)) {
                val index = qualifierIndex(source, names, name) ?: continue
                val capture = capture(source, names, index)
                if (capture is Capture.Lacking) {
                    if (lacking == null) lacking = capture
                    break
                }
                val own = (capture as Capture.Found).unverified as? Capture.Lacking ?: return capture
                val ifLacking = lacking ?: own
                return Capture.Found(capture.binding, qualified(name, enclosing(scope), names, ifLacking) ?: ifLacking)
            }
            scope = enclosing(scope)
        }
        return lacking
    }

    /** The column of [source] at segment [index] of [names], or the source itself for `t.*`. */
    private fun capture(source: Source, names: List<OracleIdentifier>, index: Int): Capture {
        val column = names.getOrNull(index) ?: return Capture.Found(ColumnBinding.Source(source.source))
        if (!column.quoted && column.lookupName in RESERVED_PSEUDOCOLUMNS) return Capture.Found(ColumnBinding.Pseudocolumn)
        val has = presence(source, column)
        if (has == false && !column.quoted && column.lookupName in ROW_PSEUDOCOLUMNS) return Capture.Found(ColumnBinding.Pseudocolumn)
        return when (has) {
            false -> Capture.Lacking(source, column, index)
            null -> Capture.Found(columnOf(source, column, null), Capture.Lacking(source, column, index))
            true -> Capture.Found(columnOf(source, column, true))
        }
    }

    private fun qualifierIndex(source: Source, names: List<OracleIdentifier>, at: AstNode): Int? {
        if (source.qualifier != null && source.qualifier == names[0]) return 1
        val written = source.written ?: return null
        if (names.size < 3 || source.source.alias != null || names[1] != written.last) return null
        val schema = if (written.segments.size >= 2) written.segments[written.segments.size - 2]
        else walk.compilationSchema(region(at))?.identifier ?: return null
        return 2.takeIf { names[0] == schema }
    }

    /**
     * An unqualified name: a pseudocolumn or SQL function no column can hide, the name of an `ORDER BY` select item,
     * else the column of the one source of the innermost scope that has it, else the next scope out while every source
     * of this one is known to lack it. `ORA_ROWSCN` a source lacks is the pseudocolumn of the innermost scope's rows.
     * A scope whose sources may have it, while none is known to, leaves it undecided: the candidates are its own, then
     * the known matches of the scopes further out, which Oracle reads when those sources lack it.
     */
    private fun unqualified(name: AstNode, start: AstNode, column: OracleIdentifier): ColumnBinding {
        if (!column.quoted && column.lookupName in BARE_PSEUDOCOLUMNS) return ColumnBinding.Pseudocolumn
        if (!column.quoted && column.lookupName in PARAMETERLESS_FUNCTIONS) return ColumnBinding.PlSqlName(PlSqlNameReason.BUILT_IN_FUNCTION)
        orderByItem(name, start, column)?.let { return it }
        var scope: AstNode? = start
        while (scope != null) {
            val sources = mergeValuesSources(name, scope, column, sourcesFor(scope, name, qualified = false))
            val known = sources.map { presence(it, column) }
            val matches = sources.filterIndexed { index, _ -> known[index] == true }
            when {
                // Another source may have the column too (ORA-00918): not decided.
                known.any { it == null } -> return ColumnBinding.Unknown(
                    uncertainty(sources.filterIndexed { index, _ -> known[index] == null }),
                    sources.indices.filter { known[it] != false }.map { columnOf(sources[it], column, has(sources[it], column)) } +
                        if (matches.isEmpty()) outerMatches(name, scope, column) else emptyList()
                )
                matches.size == 1 -> return columnOf(matches.single(), column, true)
                matches.size > 1 -> {
                    val candidates = matches.map { columnOf(it, column, true) }
                    return if (isJoinedColumn(scope, column)) ColumnBinding.Joined(candidates) else ColumnBinding.Ambiguous(candidates)
                }
                sources.isNotEmpty() && !column.quoted && column.lookupName in ROW_PSEUDOCOLUMNS -> return ColumnBinding.Pseudocolumn
            }
            scope = enclosing(scope)
        }
        return ColumnBinding.PlSqlName(PlSqlNameReason.NO_SOURCE_HAS_IT)
    }

    /**
     * The sources known to have [column] in the scopes around [scope], from the inside out, up to the first scope that
     * has one: the name reaches them when every source of the scopes before lacks it.
     */
    private fun outerMatches(name: AstNode, scope: AstNode, column: OracleIdentifier): List<ColumnBinding> {
        var outer = enclosing(scope)
        while (outer != null) {
            val matches = mergeValuesSources(name, outer, column, sourcesFor(outer, name, qualified = false)).filter { presence(it, column) == true }
            if (matches.isNotEmpty()) return matches.map { columnOf(it, column, true) }
            outer = enclosing(outer)
        }
        return emptyList()
    }

    private fun uncertainty(sources: List<Source>): Set<ColumnUncertainty> {
        val reasons = EnumSet.noneOf(ColumnUncertainty::class.java)
        for (source in sources) {
            reasons += if (source.query != null && names(source).starUncovered) ColumnUncertainty.STAR_NOT_COVERED else ColumnUncertainty.SOURCE_COLUMNS_UNKNOWN
        }
        return reasons
    }

    /**
     * An `ORDER BY` name, bare or inside an expression, that names a select item of the query it orders (of the first
     * branch of a compound query): an alias, or the last segment of a plain column item without alias. Oracle orders by
     * the item, even where a source has a column of that name; several items are ORA-00960.
     */
    private fun orderByItem(name: AstNode, start: AstNode, column: OracleIdentifier): ColumnBinding? {
        val orderBy = name.getFirstAncestorOrNull(DmlGrammar.ORDER_BY_CLAUSE) ?: return null
        val owner = orderBy.parent
        val block = if (owner.type === DmlGrammar.QUERY_BLOCK) owner else owner.getFirstChildOrNull(DmlGrammar.QUERY_BLOCK) ?: return null
        if (start !== owner && start !== block) return null
        val items = block.getChildren(DmlGrammar.SELECT_COLUMN).filter { itemName(it)?.lookupName == column.lookupName }
        return when (items.size) {
            0 -> null
            1 -> ColumnBinding.SelectItem(items.single())
            else -> ColumnBinding.Ambiguous(items.map { ColumnBinding.SelectItem(it) })
        }
    }

    /** The name a select item gives its column: its alias, else the last segment of a plain column; null for anything else. */
    private fun itemName(item: AstNode): OracleIdentifier? {
        val children = item.children
        children.lastOrNull()?.takeIf { children.size > 1 && it.type === PlSqlGrammar.IDENTIFIER_NAME }?.let { return LocalSyntax.identifier(it) }
        val first = item.firstChild
        if (first.type !== PlSqlGrammar.VARIABLE_NAME && first.type !== PlSqlGrammar.MEMBER_EXPRESSION) return null
        return segments(first)?.last()
    }

    /**
     * The sources of a name in the `VALUES` of a `MERGE` insert: the target when it has the column (Oracle reads it and
     * rejects the statement, ORA-38101), else only the `USING` source.
     */
    private fun mergeValuesSources(name: AstNode, scope: AstNode, column: OracleIdentifier, sources: List<Source>): List<Source> {
        if (scope.type !== DmlGrammar.MERGE_EXPRESSION) return sources
        val insert = name.getFirstAncestorOrNull(DmlGrammar.MERGE_INSERT_CLAUSE)?.takeIf { owner(it) === scope } ?: return sources
        var node = name
        while (node.parent !== insert) node = node.parent
        val children = insert.children
        if (children.indexOf(node) < children.indexOfFirst { it.type === PlSqlKeyword.VALUES }) return sources
        val target = sources.firstOrNull() ?: return sources
        return when (has(target, column)) {
            true -> listOf(target)
            false -> sources.drop(1)
            null -> sources
        }
    }

    /** A column of a `USING` join, or any column of a scope with a `NATURAL` join: every joined source gives it. */
    private fun isJoinedColumn(scope: AstNode, column: OracleIdentifier): Boolean =
        scope.getDescendants(DmlGrammar.ON_OR_USING_EXPRESSION, DmlGrammar.INNER_CROSS_JOIN_CLAUSE, DmlGrammar.OUTER_JOIN_CLAUSE).any { node ->
            owner(node) === scope && when (node.type) {
                DmlGrammar.ON_OR_USING_EXPRESSION -> node.hasDirectChildren(PlSqlKeyword.USING) &&
                    node.getChildren(PlSqlGrammar.IDENTIFIER_NAME).any { LocalSyntax.identifier(it) == column }
                else -> node.hasDirectChildren(PlSqlKeyword.NATURAL)
            }
        }

    /** A column an `INSERT` column list, the left side of `SET` or a `MERGE` insert list writes: one of the target. */
    private fun writtenColumn(name: AstNode, start: AstNode, column: OracleIdentifier): ColumnBinding? {
        var node = name
        while (node.parent.type === PlSqlGrammar.BRACKED_EXPRESSION || node.parent.type === PlSqlGrammar.MULTIPLE_VALUE_EXPRESSION) node = node.parent
        val parent = node.parent
        val target = when (parent.type) {
            DmlGrammar.INSERT_COLUMNS -> parent.parent.takeIf { it.type === DmlGrammar.INSERT_INTO_CLAUSE }?.let(::insertTarget)
            DmlGrammar.UPDATE_COLUMN -> if (parent.firstChild === node) sourcesOf(start).firstOrNull() else null
            DmlGrammar.MERGE_INSERT_CLAUSE -> {
                val children = parent.children
                val index = children.indexOf(node)
                if (index > children.indexOfFirst { it.type === PlSqlKeyword.INSERT } && index < children.indexOfFirst { it.type === PlSqlKeyword.VALUES }) {
                    sourcesOf(start).firstOrNull()
                } else null
            }
            else -> null
        } ?: return null
        return columnOf(target, column, has(target, column))
    }

    /** The `t` of `t.*`: the source it names. */
    private fun starQualifier(name: AstNode, start: AstNode): ColumnBinding {
        val names = segments(name) ?: return unavailable()
        val source = sourcesOf(start).firstOrNull { qualifierIndex(it, names + STAR, name) != null }
            ?: return ColumnBinding.PlSqlName(PlSqlNameReason.QUALIFIER_NOT_A_SOURCE)
        return ColumnBinding.Source(source.source)
    }

    /** The argument of `REF(x)` or `VALUE(x)` that is the alias of a source of an enclosing scope: Oracle takes only an alias there. */
    private fun correlationArgument(name: AstNode, start: AstNode): ColumnBinding? {
        if (name.type !== PlSqlGrammar.VARIABLE_NAME || name.parent.type !== PlSqlGrammar.ARGUMENT) return null
        val call = name.parent.parentOrNull?.parentOrNull?.takeIf { it.type === PlSqlGrammar.METHOD_CALL } ?: return null
        val callee = call.firstChild.takeIf { it.type === PlSqlGrammar.VARIABLE_NAME } ?: return null
        if (segments(callee)?.single()?.lookupName !in CORRELATION_FUNCTIONS) return null
        if (call.getChildren(PlSqlGrammar.ARGUMENTS).singleOrNull()?.getChildren(PlSqlGrammar.ARGUMENT)?.size != 1) return null
        val alias = segments(name)?.single() ?: return null
        var scope: AstNode? = start
        while (scope != null) {
            sourcesFor(scope, name, qualified = true).firstOrNull { it.source.alias != null && it.qualifier == alias }?.let { return ColumnBinding.Source(it.source) }
            scope = enclosing(scope)
        }
        return null
    }

    private fun columnOf(source: Source, column: OracleIdentifier, exists: Boolean?): ColumnBinding = when (source.source.kind) {
        ColumnSourceKind.WITH_QUERY, ColumnSourceKind.INLINE_VIEW ->
            ColumnBinding.QueryColumn(source.source, column.lookupName, names(source).items[column.lookupName])
        ColumnSourceKind.RELATION -> {
            val (_, external) = walk.relationColumn(source.source.relation, column)
            ColumnBinding.Column(source.source, column.lookupName, exists, external)
        }
        else -> ColumnBinding.Column(source.source, column.lookupName, exists, null)
    }

    /**
     * [has], except for `OBJECT_ID` and `OBJECT_VALUE` a relation lacks: an object table or view has them as
     * pseudocolumns, and the metadata does not say which relations are.
     */
    private fun presence(source: Source, column: OracleIdentifier): Boolean? = has(source, column).takeUnless {
        it == false && source.source.kind == ColumnSourceKind.RELATION && !column.quoted && column.lookupName in OBJECT_PSEUDOCOLUMNS
    }

    /** Whether [source] has [column]: null when its columns are not known. */
    private fun has(source: Source, column: OracleIdentifier): Boolean? = when (source.source.kind) {
        ColumnSourceKind.RELATION -> walk.relationColumn(source.source.relation, column).first
        ColumnSourceKind.DUAL -> column.lookupName == "DUMMY"
        ColumnSourceKind.WITH_QUERY, ColumnSourceKind.INLINE_VIEW -> names(source).let { names ->
            if (column.lookupName in names.items) true else if (names.complete) false else null
        }
        ColumnSourceKind.OTHER -> null
    }

    // Sources.

    /**
     * The sources that can take [name] in [scope]: those it owns, except that an `INSERT` target takes only a qualified
     * name or one of `RETURNING`, and the `ORDER BY` of a query sees the sources of its first query block.
     */
    private fun sourcesFor(scope: AstNode, name: AstNode, qualified: Boolean): List<Source> = when (scope.type) {
        DmlGrammar.SELECT_EXPRESSION -> scope.getFirstChildOrNull(DmlGrammar.QUERY_BLOCK)?.let(::sourcesOf).orEmpty()
        DmlGrammar.INSERT_EXPRESSION ->
            if (qualified || name.getFirstAncestorOrNull(DmlGrammar.RETURNING_INTO_CLAUSE)?.let { owner(it) === scope } == true) sourcesOf(scope)
            else emptyList()
        else -> sourcesOf(scope)
    }

    /** The sources [scope] owns directly: its `FROM` items, and the target of `UPDATE`, `DELETE`, `INSERT` or `MERGE`. */
    private fun sourcesOf(scope: AstNode): List<Source> = scopeSources.getOrPut(scope) {
        val result = ArrayList<Source>()
        if (scope.type === DmlGrammar.MERGE_EXPRESSION) {
            scope.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE)?.let { result += namedSource(it, scope.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)) }
        }
        if (scope.type === DmlGrammar.INSERT_EXPRESSION) {
            scope.getDescendants(DmlGrammar.INSERT_INTO_CLAUSE).filter { owner(it) === scope }.mapNotNullTo(result, ::insertTarget)
        }
        for (clause in scope.getDescendants(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)) {
            if (owner(clause) === scope) result += tableExpression(clause)
        }
        result
    }

    private fun insertTarget(clause: AstNode): Source? =
        clause.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE)?.let { namedSource(it, clause.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)) }

    private fun tableExpression(clause: AstNode): Source {
        val aliasNode = clause.getFirstChildOrNull(DmlGrammar.ALIAS)?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
        val reference = clause.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE)
        val query = clause.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION)
        val alias = aliasNode?.let(LocalSyntax::identifier)
        return when {
            reference != null -> namedSource(reference, aliasNode)
            query != null -> Source(ColumnSource(clause, ColumnSourceKind.INLINE_VIEW, alias?.lookupName, null), alias, null, query, null)
            else -> Source(ColumnSource(clause, ColumnSourceKind.OTHER, alias?.lookupName, null), alias, null, null, null)
        }
    }

    /** A relation reference: a `WITH` name, `DUAL` (one column, `DUMMY`), or a relation the project reads. */
    private fun namedSource(reference: AstNode, aliasNode: AstNode?): Source {
        val alias = aliasNode?.let(LocalSyntax::identifier)
        val remote = reference.hasDirectChildren(PlSqlPunctuator.REMOTE)
        val parts = reference.children.takeWhile { it.type !== PlSqlPunctuator.REMOTE }.filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
            .map(LocalSyntax::identifier)
        val written = QualifiedName(parts)
        val qualifier = alias ?: written.last
        if (!remote && parts.size == 1) {
            withQuery(reference, written.last)?.let { factoring ->
                val listed = factoring.children.drop(1).takeWhile { it.type !== PlSqlKeyword.AS }.filter { it.type === PlSqlGrammar.IDENTIFIER_NAME }
                    .map(LocalSyntax::identifier)
                return Source(ColumnSource(reference, ColumnSourceKind.WITH_QUERY, alias?.lookupName, null), qualifier, written,
                    factoring.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION), listed.ifEmpty { null })
            }
        }
        if (!remote && (parts.map { it.lookupName } == listOf("DUAL") || parts.map { it.lookupName } == listOf("SYS", "DUAL"))) {
            return Source(ColumnSource(reference, ColumnSourceKind.DUAL, alias?.lookupName, null), qualifier, written, null, null)
        }
        val relation = if (remote) null else walk.relationRow(written, region(reference))
        return Source(ColumnSource(reference, ColumnSourceKind.RELATION, alias?.lookupName, relation), qualifier, written, null, null)
    }

    /** The `WITH` query named [name] visible from [reference], the nearest first. */
    private fun withQuery(reference: AstNode, name: OracleIdentifier): AstNode? =
        generateSequence(reference.parent) { it.parentOrNull }.filter { it.type === DmlGrammar.SELECT_EXPRESSION }.firstNotNullOfOrNull { select ->
            select.getFirstChildOrNull(DmlGrammar.WITH_CLAUSE)?.getChildren(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)
                ?.firstOrNull { LocalSyntax.identifier(it.firstChild) == name }
        }

    /**
     * The columns of an inline view or a `WITH` query: its column list, else the names its select list gives (of the
     * first branch of a compound query): an alias, the last segment of a plain column, or every column of the sources a
     * `*` or `t.*` covers when they are known.
     */
    private fun names(source: Source): QueryNames = queryNames.getOrPut(source) {
        val query = source.query ?: return@getOrPut QueryNames(emptyMap(), complete = false, starUncovered = false)
        val block = query.getFirstChildOrNull(DmlGrammar.QUERY_BLOCK)
        val items = block?.getChildren(DmlGrammar.SELECT_COLUMN).orEmpty()
        source.listed?.let { listed ->
            val positional = items.size == listed.size && items.none(::isStar)
            return@getOrPut QueryNames(listed.mapIndexed { index, it -> it.lookupName to items.getOrNull(index)?.takeIf { positional } }.toMap(), true, false)
        }
        if (block == null) return@getOrPut QueryNames(emptyMap(), complete = false, starUncovered = false)
        val names = LinkedHashMap<String, AstNode?>()
        var complete = true
        var starUncovered = false
        for (item in items) {
            val covered = starSources(item, block)
            val columns = if (covered == null) itemName(item)?.let { listOf(it.lookupName) } else starColumns(covered)
            if (columns == null) {
                complete = false
                starUncovered = starUncovered || !covered.isNullOrEmpty()
            } else columns.forEach { names.putIfAbsent(it, item) }
        }
        QueryNames(names, complete, starUncovered)
    }

    /** Every column the sources a `*` covers give, or null when one of them has columns that are not known. */
    private fun starColumns(covered: List<Source>): List<String>? =
        if (covered.isEmpty()) null else covered.flatMap { sourceColumns(it) ?: return null }

    /** Every column of [source] by lookup name, when known. */
    private fun sourceColumns(source: Source): List<String>? = when (source.source.kind) {
        ColumnSourceKind.RELATION -> walk.coveredRelation(source.source.relation)?.rowFields?.map { it.name.identifier.lookupName }
        ColumnSourceKind.DUAL -> listOf("DUMMY")
        ColumnSourceKind.WITH_QUERY, ColumnSourceKind.INLINE_VIEW -> names(source).takeIf { it.complete }?.items?.keys?.toList()
        ColumnSourceKind.OTHER -> null
    }

    private fun isStar(item: AstNode) = item.firstChild.type === PlSqlPunctuator.MULTIPLICATION ||
        item.firstChild.type === PlSqlGrammar.OBJECT_REFERENCE && item.firstChild.lastChild.type === PlSqlPunctuator.MULTIPLICATION

    /** The sources a `*` or `t.*` item of [block] covers, or null when the item is not one. */
    private fun starSources(item: AstNode, block: AstNode): List<Source>? {
        val first = item.firstChild
        return when {
            first.type === PlSqlPunctuator.MULTIPLICATION -> sourcesOf(block)
            first.type === PlSqlGrammar.OBJECT_REFERENCE && first.lastChild.type === PlSqlPunctuator.MULTIPLICATION -> {
                val names = segments(first.firstChild) ?: return emptyList()
                sourcesOf(block).filter { qualifierIndex(it, names + STAR, item) != null }.take(1)
            }
            else -> null
        }
    }

    // Scopes.

    /** The innermost scope of [name]: its query block or DML statement, or for an `ORDER BY` name the query it orders. */
    private fun startScope(name: AstNode): AstNode? {
        var node: AstNode? = name.parent
        while (node != null) {
            when {
                node.type === DmlGrammar.ORDER_BY_CLAUSE && node.parent.type === DmlGrammar.SELECT_EXPRESSION -> return node.parent
                node.type in SCOPES -> return node
            }
            node = node.parentOrNull
        }
        return null
    }

    /**
     * The scope [scope] sees around it, as `nameBinding`'s SQL scopes: an inline view without `LATERAL` or `APPLY` and
     * the query of a single-table `INSERT … SELECT` skip the scope that holds them.
     */
    private fun enclosing(scope: AstNode): AstNode? {
        var skip = 0
        var node: AstNode? = if (scope.type === DmlGrammar.SELECT_EXPRESSION) scope else scope.parentOrNull
        while (node != null) {
            if (node.type === DmlGrammar.SELECT_EXPRESSION && skipsEnclosing(node)) skip++
            else if (node.type in SCOPES && node !== scope) {
                if (skip == 0) return node
                skip--
            }
            node = node.parentOrNull
        }
        return null
    }

    private fun skipsEnclosing(query: AstNode): Boolean {
        val parent = query.parentOrNull ?: return false
        return when (parent.type) {
            DmlGrammar.SINGLE_TABLE_INSERT -> true
            DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE ->
                !parent.hasDirectChildren(PlSqlKeyword.LATERAL) && parent.parent.type !== DmlGrammar.CROSS_OUTER_APPLY_CLAUSE
            else -> false
        }
    }

    /** The query block or statement that directly holds [node] (a select expression counts, as it holds its own query). */
    private fun owner(node: AstNode): AstNode? = node.getFirstAncestorOrNull(*OWNERS)

    private fun isBelow(node: AstNode, scope: AstNode): Boolean = generateSequence(node.parentOrNull) { it.parentOrNull }.any { it === scope }

    /**
     * Hidden text inside the outermost statement holding [scope]: conditional text may write a source, an alias or a
     * column there, and a substitution or a recovered parse may hide one. A kept branch holding the whole statement does not.
     */
    private fun hidden(scope: AstNode): ColumnBinding? {
        if (model.hiddenText.isEmpty()) return null
        val statement = generateSequence(scope) { it.parentOrNull }.last { it.type in SCOPES || it.type === DmlGrammar.SELECT_EXPRESSION }
        val range = LocalSyntax.range(model.fileId, statement)
        val reasons = EnumSet.noneOf(ColumnUncertainty::class.java)
        for (text in model.hiddenText) {
            val t = text.range
            val overlaps = !before(t.endLine, t.endColumn, range.startLine, range.startColumn) && !before(range.endLine, range.endColumn, t.startLine, t.startColumn)
            val holds = !before(range.startLine, range.startColumn, t.startLine, t.startColumn) && !before(t.endLine, t.endColumn, range.endLine, range.endColumn)
            if (overlaps && !holds) {
                reasons += when (text.reason.usageUncertainty()) {
                    UsageUncertainty.CONDITIONAL_COMPILATION -> ColumnUncertainty.CONDITIONAL_COMPILATION
                    UsageUncertainty.SUBSTITUTION -> ColumnUncertainty.SUBSTITUTION
                    UsageUncertainty.PARSE_RECOVERY -> ColumnUncertainty.PARSE_RECOVERY
                    else -> ColumnUncertainty.INCOMPLETE_SOURCE
                }
            }
        }
        return if (reasons.isEmpty()) null else ColumnBinding.Unknown(reasons)
    }

    private fun before(line: Int, column: Int, otherLine: Int, otherColumn: Int) = line < otherLine || line == otherLine && column < otherColumn

    /**
     * A name of a dynamic text whose value a hole may change: a placeholder itself, or any name when some hole may write
     * more than a value ([DynamicSqlText.writesOnlyValue]), or when the holes were left out to parse the text.
     */
    private fun dynamicHole(name: AstNode): ColumnBinding? {
        val dynamic = dynamic ?: return null
        val text = dynamic.text
        if (text.holes.isEmpty()) return null
        if (text.holesDropped || text.holesIn(name).isNotEmpty() || text.holes.any { !text.writesOnlyValue(it, dynamic.isNumber) }) {
            return ColumnBinding.Unknown(EnumSet.of(ColumnUncertainty.DYNAMIC_SQL_HOLE))
        }
        return null
    }

    // Names.

    /** The identifiers of a plain or qualified name (no attribute, no database link), or null. */
    private fun segments(name: AstNode): List<OracleIdentifier>? = when (name.type) {
        PlSqlGrammar.VARIABLE_NAME -> name.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let { listOf(LocalSyntax.identifier(it)) }
        PlSqlGrammar.MEMBER_EXPRESSION -> {
            val first = name.firstChild
            if (first.type !== PlSqlGrammar.VARIABLE_NAME || name.hasDirectChildren(PlSqlPunctuator.MOD, PlSqlPunctuator.REMOTE)) null
            else first.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.let { head ->
                listOf(LocalSyntax.identifier(head)) + name.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map(LocalSyntax::identifier)
            }
        }
        else -> null
    }

    /** `:name`, or a name it qualifies (`:new.col`). */
    private fun isBindVariable(name: AstNode): Boolean {
        val head = if (name.type === PlSqlGrammar.MEMBER_EXPRESSION) name.firstChild else name
        return head.type === PlSqlGrammar.VARIABLE_NAME && head.firstChild.type === PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE
    }

    private fun firstName(name: AstNode) = if (name.type === PlSqlGrammar.MEMBER_EXPRESSION) name.firstChild else name

    private fun region(node: AstNode) = LocalReferenceCollector.innermostRegion(model, LocalSyntax.range(model.fileId, node))

    private fun unavailable() = ColumnBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)

    private companion object {
        val SCOPES = setOf(
            DmlGrammar.QUERY_BLOCK, DmlGrammar.UPDATE_EXPRESSION, DmlGrammar.DELETE_EXPRESSION, DmlGrammar.INSERT_EXPRESSION, DmlGrammar.MERGE_EXPRESSION
        )
        val OWNERS = arrayOf(
            DmlGrammar.QUERY_BLOCK, DmlGrammar.SELECT_EXPRESSION, DmlGrammar.UPDATE_EXPRESSION,
            DmlGrammar.DELETE_EXPRESSION, DmlGrammar.INSERT_EXPRESSION, DmlGrammar.MERGE_EXPRESSION
        )

        /** Declarations whose name qualifies their members: packages, subprograms, labelled blocks. */
        val NAMING_UNITS = EnumSet.of(NameDeclarationKind.OTHER, NameDeclarationKind.FUNCTION, NameDeclarationKind.PROCEDURE)

        /** Reserved words: no column has these names. */
        val RESERVED_PSEUDOCOLUMNS = setOf("ROWNUM", "ROWID", "LEVEL")

        /** Unqualified, the pseudocolumn even where a source has a column of that name; qualified, that column. */
        val BARE_PSEUDOCOLUMNS = RESERVED_PSEUDOCOLUMNS + setOf("CONNECT_BY_ISLEAF", "CONNECT_BY_ISCYCLE")

        /** A pseudocolumn of every table's rows, unless the table has a column of that name. */
        val ROW_PSEUDOCOLUMNS = setOf("ORA_ROWSCN")

        /** Pseudocolumns of an object table or view, unless it has a column of that name. */
        val OBJECT_PSEUDOCOLUMNS = setOf("OBJECT_ID", "OBJECT_VALUE")
        val SEQUENCE_PSEUDOCOLUMNS = setOf("NEXTVAL", "CURRVAL")

        /** SQL functions called without parentheses. */
        val PARAMETERLESS_FUNCTIONS = setOf(
            "SYSDATE", "SYSTIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP", "LOCALTIMESTAMP", "USER", "UID", "DBTIMEZONE", "SESSIONTIMEZONE"
        )
        val CORRELATION_FUNCTIONS = setOf("REF", "VALUE")

        /** A last segment that matches no column, for the qualifier of `t.*`. */
        val STAR = OracleIdentifier.fromSource("\"*\"")
    }
}
