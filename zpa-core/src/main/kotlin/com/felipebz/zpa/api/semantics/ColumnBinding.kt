//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalColumn
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription

/**
 * What a name written in static SQL reads, as Oracle resolves it: a column of a source of the statement, a select item,
 * a pseudocolumn, or a PL/SQL name. [Unknown] is never a proof.
 */
@ZpaExperimentalApi
sealed interface ColumnBinding {
    /**
     * The column [name] (its lookup form) of the relation [source] names: a table, a view, a synonym of one, or `DUAL`.
     * [exists] is true or false when the external metadata covers the relation's columns, and null otherwise; Oracle
     * rejects a column that does not exist (ORA-00904). [column] is the metadata's column when it exists.
     */
    class Column(val source: ColumnSource, val name: String, val exists: Boolean?, val column: ProjectExternalColumn?) : ColumnBinding {
        override fun equals(other: Any?) = other is Column && source == other.source && name == other.name && exists == other.exists

        override fun hashCode() = (source.hashCode() * 31 + name.hashCode()) * 31 + exists.hashCode()

        override fun toString() = "Column(${source.label}.$name${exists?.let { if (it) "" else ", missing" } ?: ", unverified"})"
    }

    /**
     * The column [name] of an inline view or a `WITH` query [source]: [item] is the `SELECT_COLUMN` of its query that
     * gives it (a `*` item when a star covers it), null when its place in the select list is not known.
     */
    class QueryColumn(val source: ColumnSource, val name: String, val item: AstNode?) : ColumnBinding {
        override fun equals(other: Any?) = other is QueryColumn && source == other.source && name == other.name && item === other.item

        override fun hashCode() = source.hashCode() * 31 + name.hashCode()

        override fun toString() = "QueryColumn(${source.label}.$name)"
    }

    /**
     * A name of `ORDER BY` that names a select item of the query it orders (its alias, or the name of a plain column
     * item without one): Oracle orders by that item, even where a source has a column of the same name.
     */
    class SelectItem(val item: AstNode) : ColumnBinding {
        override fun equals(other: Any?) = other is SelectItem && item === other.item

        override fun hashCode() = System.identityHashCode(item)

        override fun toString() = "SelectItem(${item.token.line}:${item.token.column})"
    }

    /** The alias of [source]: the qualifier of `t.*`, or the argument of `REF(t)` or `VALUE(t)`. */
    class Source(val source: ColumnSource) : ColumnBinding {
        override fun equals(other: Any?) = other is Source && source == other.source

        override fun hashCode() = source.hashCode()

        override fun toString() = "Source(${source.label})"
    }

    /**
     * A pseudocolumn SQL itself answers: `ROWNUM`, `ROWID`, `LEVEL` and the like, or `NEXTVAL`/`CURRVAL` of a sequence.
     * A column of that name a source has comes first, except for an unqualified `CONNECT_BY_ISLEAF` or
     * `CONNECT_BY_ISCYCLE`; `ROWNUM`, `ROWID` and `LEVEL` are reserved words no column has.
     */
    data object Pseudocolumn : ColumnBinding

    /** A column of a `USING` or `NATURAL` join, which every joined source has: Oracle reads the joined value. */
    class Joined(columns: List<ColumnBinding>) : ColumnBinding {
        val columns: List<ColumnBinding> = columns.toList()

        override fun equals(other: Any?) = other is Joined && columns == other.columns

        override fun hashCode() = columns.hashCode()

        override fun toString() = "Joined$columns"
    }

    /** Several sources (ORA-00918) or several select items (ORA-00960) take the name: Oracle rejects it. */
    class Ambiguous(candidates: List<ColumnBinding>) : ColumnBinding {
        val candidates: List<ColumnBinding> = candidates.toList()

        override fun equals(other: Any?) = other is Ambiguous && candidates == other.candidates

        override fun hashCode() = candidates.hashCode()

        override fun toString() = "Ambiguous$candidates"
    }

    /**
     * No column takes the name: it is a PL/SQL name, which `nameBinding` answers. [reason] says why. In a dynamic SQL
     * text that is a SQL statement no PL/SQL declaration exists: there it is a function called without parentheses, or
     * Oracle rejects the statement (ORA-00904). In a text that is a PL/SQL block, the block's declarations take names.
     */
    class PlSqlName(val reason: PlSqlNameReason) : ColumnBinding {
        override fun equals(other: Any?) = other is PlSqlName && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "PlSqlName($reason)"
    }

    /**
     * Not decided; [reasons] says what the analysis could not see. Never a proof.
     *
     * [candidates] are the answers the name would have, each unverified:
     * - when a source whose columns are not known may take it ([ColumnUncertainty.SOURCE_COLUMNS_UNKNOWN],
     *   [ColumnUncertainty.STAR_NOT_COVERED]): for each source of the innermost scope that leaves it undecided, in the
     *   order of the statement, the [Column] of a relation or of another table expression (`exists` null), or the
     *   [QueryColumn] of an inline view or `WITH` query whose columns are not all known; and the sources of that scope
     *   known to have the column (`exists` true), which Oracle would find ambiguous (ORA-00918) beside another one. When
     *   no source of that scope is known to have it, then the sources of the enclosing scopes the name reaches if those
     *   lack it that are known to have it (`exists` true), from the inside out, up to the first scope that has one;
     * - for [ColumnUncertainty.PLSQL_NAME_UNDECIDED]: the [Column] of the source the qualifier names, which lacks it
     *   (`exists` false): what Oracle reads, and rejects, when no PL/SQL name takes the name;
     * - otherwise (text the analysis cannot see, a hole of a dynamic text, a text not analyzed) none: the scopes are not
     *   walked.
     */
    class Unknown(reasons: Set<ColumnUncertainty>, candidates: List<ColumnBinding> = emptyList()) : ColumnBinding {
        val reasons: Set<ColumnUncertainty> = reasons.toSet()
        val candidates: List<ColumnBinding> = candidates.toList()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons && candidates == other.candidates

        override fun hashCode() = 31 * reasons.hashCode() + candidates.hashCode()

        override fun toString() = "Unknown$reasons" + if (candidates.isEmpty()) "" else candidates.toString()
    }

    /** No evidence: the file was not analyzed, or the node is not a name of static SQL this query covers. */
    class Unavailable(val reason: UsageUnavailability) : ColumnBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/**
 * A source of a SQL statement: [node] is the `TABLE_REFERENCE` of a relation or a `WITH` name, or the
 * `DML_TABLE_EXPRESSION_CLAUSE` of an inline view or of another table expression. [alias] is the alias written for it,
 * in its lookup form (upper-cased unless quoted).
 * [relation] is the project's reading of a relation written in the statement, through its synonyms and the scan's
 * external metadata (as a `%ROWTYPE` of that name); null for any other source.
 */
@ZpaExperimentalApi
class ColumnSource internal constructor(val node: AstNode, val kind: ColumnSourceKind, val alias: String?, val relation: ProjectExternalTypeDescription?) {
    internal val label: String get() = alias ?: if (node.type === DmlGrammar.TABLE_REFERENCE) node.tokens.joinToString("") { it.originalValue } else kind.name

    override fun equals(other: Any?) = other is ColumnSource && node === other.node

    override fun hashCode() = System.identityHashCode(node)

    override fun toString() = "$kind $label"
}

@ZpaExperimentalApi
enum class ColumnSourceKind {
    /** A table, a view or a synonym of one, as written. */
    RELATION,

    /** The `DUAL` table, whose only column is `DUMMY`. */
    DUAL,

    /** A `WITH` name, which hides a relation of the same name. */
    WITH_QUERY,

    /** A subquery in `FROM`. */
    INLINE_VIEW,

    /** Any other table expression (`TABLE(collection)`, a table function): its columns are not known. */
    OTHER
}

/** Why a name of static SQL is a PL/SQL name. */
@ZpaExperimentalApi
enum class PlSqlNameReason {
    /** No source of the statement's scopes has the column: each is known to lack it, or there is none. */
    NO_SOURCE_HAS_IT,

    /** The first segment names no source of the statement's scopes. */
    QUALIFIER_NOT_A_SOURCE,

    /** A target of `INTO` or `RETURNING … INTO`. */
    INTO_TARGET,

    /** A SQL function written without parentheses (`SYSDATE`, `USER`…), which no column takes. */
    BUILT_IN_FUNCTION,

    /** A bind or host variable (`:name`), alone or qualifying a name (a trigger's `:new.col`): never a column. */
    BIND_VARIABLE
}

/** What keeps a name of static SQL undecided. */
@ZpaExperimentalApi
enum class ColumnUncertainty {
    /** A source the name may read has columns the analysis does not know: no metadata covers the relation. */
    SOURCE_COLUMNS_UNKNOWN,

    /** An inline view or `WITH` query selects `*` from a source whose columns are not known. */
    STAR_NOT_COVERED,

    /** The name binds no column, and what PL/SQL binds it to is not decided either. */
    PLSQL_NAME_UNDECIDED,

    /** Conditional-compilation text inside the statement may write a source, an alias or a column. */
    CONDITIONAL_COMPILATION,

    /** A SQL*Plus substitution inside the statement. */
    SUBSTITUTION,

    /** The parser recovered from a syntax error inside the statement. */
    PARSE_RECOVERY,

    /** Other text inside the statement the analysis discarded. */
    INCOMPLETE_SOURCE,

    /** A name of a dynamic SQL text whose analysis is not available. */
    DYNAMIC_SQL,

    /**
     * A name of a dynamic SQL text that a concatenated value zpa could not read may change: the placeholder of that
     * value, or any name of the text when such a value may write more than a value (a source, an alias, a column).
     */
    DYNAMIC_SQL_HOLE
}
