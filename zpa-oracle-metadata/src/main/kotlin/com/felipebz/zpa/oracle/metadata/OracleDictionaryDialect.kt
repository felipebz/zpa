//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

private const val COLUMN_LIST =
    "SELECT owner, table_name, column_name, column_id, data_type, data_type_owner, data_length, char_length, " +
        "char_used, data_precision, data_scale, nullable FROM all_tab_cols "

private const val SYNONYM_LIST = "SELECT owner, synonym_name, table_owner, table_name, db_link FROM all_synonyms "

private const val TABLE_LIST = "SELECT owner, table_name, table_type_owner, table_type, 'TABLE', NULL FROM all_all_tables "

/**
 * A view's status comes with it: Oracle keeps the `NULLABLE` its columns had when it was last compiled, and does not
 * recompile an invalid view until it is used, so the `NULLABLE` of an invalid view's column may be out of date.
 */
private const val VIEW_LIST =
    "SELECT v.owner, v.view_name, v.view_type_owner, v.view_type, 'VIEW', (SELECT o.status FROM all_objects o " +
        "WHERE o.owner = v.owner AND o.object_name = v.view_name AND o.object_type = 'VIEW') FROM all_views v "

private fun placeholders(pairs: Int) = List(pairs) { "(?, ?)" }.joinToString(", ")

/**
 * The dictionary SQL of an Oracle release family; every statement the acquisition runs comes from here. Chosen from
 * the server major version before any dictionary query runs, never by trying a query.
 */
internal enum class OracleDictionaryDialect(private val containerExpression: String, private val columnFilter: String) {
    /** Before 12c: no `CON_NAME` and no `USER_GENERATED`; there are no invisible columns, so every hidden column is system-generated. */
    LEGACY("NULL", "hidden_column = 'NO'"),

    /** 12c and later. */
    CURRENT("SYS_CONTEXT('USERENV', 'CON_NAME')", "user_generated = 'YES'");

    /** Session user, current schema, container (NULL for [LEGACY]) and database unique name, in this order. */
    val sessionSql: String =
        "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER'), SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA'), " +
            "$containerExpression, SYS_CONTEXT('USERENV', 'DB_UNIQUE_NAME') FROM dual"

    val schemaRelationsSql: String =
        TABLE_LIST + "WHERE owner = ? AND (dropped IS NULL OR dropped = 'NO') " +
            "UNION ALL " + VIEW_LIST + "WHERE v.owner = ?"

    val schemaColumnsSql: String =
        COLUMN_LIST + "WHERE owner = ? AND $columnFilter ORDER BY table_name, internal_column_id"

    /** Relations of [pairs] `(owner, name)` pairs; parameters are the pairs twice. */
    fun targetedRelationsSql(pairs: Int): String {
        val placeholders = placeholders(pairs)
        return TABLE_LIST + "WHERE (owner, table_name) IN ($placeholders) AND (dropped IS NULL OR dropped = 'NO') " +
            "UNION ALL " + VIEW_LIST + "WHERE (v.owner, v.view_name) IN ($placeholders)"
    }

    /** Columns of [pairs] `(owner, table_name)` pairs; parameters are the pairs once. */
    fun targetedColumnsSql(pairs: Int): String =
        COLUMN_LIST + "WHERE (owner, table_name) IN (${placeholders(pairs)}) AND $columnFilter " +
            "ORDER BY owner, table_name, internal_column_id"

    /**
     * Object names of one owner, of the types that can start a qualified name. A materialized view is read too (its
     * container table has the same name); package and type bodies and other types are not read.
     */
    val schemaObjectsSql: String =
        "SELECT owner, object_name, object_type FROM all_objects WHERE owner = ? AND object_type IN " +
            "('TABLE', 'MATERIALIZED VIEW', 'VIEW', 'SYNONYM', 'PACKAGE', 'TYPE', 'SEQUENCE', 'PROCEDURE', 'FUNCTION')"

    val schemaSynonymsSql: String = SYNONYM_LIST + "WHERE owner = ?"

    val publicSynonymsSql: String = SYNONYM_LIST + "WHERE owner = 'PUBLIC'"

    /** Public synonyms whose `TABLE_OWNER` is one of [targets] schemas, which are the parameters. */
    fun targetedPublicSynonymsSql(targets: Int): String =
        SYNONYM_LIST + "WHERE owner = 'PUBLIC' AND table_owner IN (${List(targets) { "?" }.joinToString(", ")})"

    companion object {
        fun forMajorVersion(major: Int): OracleDictionaryDialect = if (major < 12) LEGACY else CURRENT
    }
}
