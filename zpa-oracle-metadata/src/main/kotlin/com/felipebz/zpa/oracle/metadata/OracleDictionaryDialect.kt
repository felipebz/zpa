//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

private const val COLUMN_LIST =
    "SELECT owner, table_name, column_name, column_id, data_type, data_type_owner, data_length, char_length, " +
        "char_used, data_precision, data_scale FROM all_tab_cols "

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
        "SELECT owner, table_name, table_type_owner, table_type, 'TABLE' FROM all_all_tables " +
            "WHERE owner = ? AND (dropped IS NULL OR dropped = 'NO') " +
            "UNION ALL SELECT owner, view_name, view_type_owner, view_type, 'VIEW' FROM all_views WHERE owner = ?"

    val schemaColumnsSql: String =
        COLUMN_LIST + "WHERE owner = ? AND $columnFilter ORDER BY table_name, internal_column_id"

    /** Relations of [pairs] `(owner, name)` pairs; parameters are the pairs twice. */
    fun targetedRelationsSql(pairs: Int): String {
        val placeholders = placeholders(pairs)
        return "SELECT owner, table_name, table_type_owner, table_type, 'TABLE' FROM all_all_tables " +
            "WHERE (owner, table_name) IN ($placeholders) AND (dropped IS NULL OR dropped = 'NO') " +
            "UNION ALL SELECT owner, view_name, view_type_owner, view_type, 'VIEW' FROM all_views WHERE (owner, view_name) IN ($placeholders)"
    }

    /** Columns of [pairs] `(owner, table_name)` pairs; parameters are the pairs once. */
    fun targetedColumnsSql(pairs: Int): String =
        COLUMN_LIST + "WHERE (owner, table_name) IN (${placeholders(pairs)}) AND $columnFilter " +
            "ORDER BY owner, table_name, internal_column_id"

    companion object {
        fun forMajorVersion(major: Int): OracleDictionaryDialect = if (major < 12) LEGACY else CURRENT
    }
}
