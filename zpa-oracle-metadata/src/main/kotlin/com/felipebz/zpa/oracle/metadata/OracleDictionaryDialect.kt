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

private const val NAME = "\u0000name\u0000"

private const val SYNONYM_TARGETS = "(owner, $NAME) IN (SELECT table_owner, table_name FROM all_synonyms WHERE "

/**
 * Which objects a type or subprogram query reads, as a condition on their owner and name, with [parameters] bind
 * values: those of one schema, those its synonyms or the public synonyms stand for, or named ones.
 */
internal class OracleTypeScope private constructor(private val template: String, val parameters: Int) {
    /** The condition over `OWNER` and `TYPE_NAME`, the columns every type view has. */
    val condition: String get() = on("type_name")

    /** The condition with [nameColumn] as the object's name. */
    fun on(nameColumn: String): String = template.replace(NAME, nameColumn)

    companion object {
        /** The objects of one schema; the parameter is the schema. */
        val SCHEMA = OracleTypeScope("owner = ?", 1)

        /** The objects the synonyms of one schema stand for; the parameter is the schema. */
        val SCHEMA_SYNONYMS = OracleTypeScope(SYNONYM_TARGETS + "owner = ? AND db_link IS NULL)", 1)

        /** The objects every public synonym stands for. */
        val PUBLIC_SYNONYMS = OracleTypeScope(SYNONYM_TARGETS + "owner = 'PUBLIC' AND db_link IS NULL)", 0)

        /** The objects the public synonyms whose `TABLE_OWNER` is one of [targets] schemas stand for; the parameters are the schemas. */
        fun publicSynonymsOf(targets: Int) = OracleTypeScope(
            SYNONYM_TARGETS + "owner = 'PUBLIC' AND db_link IS NULL AND table_owner IN (${List(targets) { "?" }.joinToString(", ")}))", targets
        )

        /** The objects of [pairs] `(owner, name)` pairs; the parameters are the pairs. */
        fun named(pairs: Int) = OracleTypeScope("(owner, $NAME) IN (${placeholders(pairs)})", pairs * 2)
    }
}

/**
 * The dictionary SQL of an Oracle release family; every statement the acquisition runs comes from here. Chosen from
 * the server major version before any dictionary query runs, never by trying a query.
 */
internal enum class OracleDictionaryDialect(
    private val containerExpression: String,
    private val columnFilter: String,
    private val attributeCharUsed: String
) {
    /**
     * Before 12c: no `CON_NAME` and no `USER_GENERATED`; there are no invisible columns, so every hidden column is
     * system-generated. The oldest releases have no `CHAR_USED` in `ALL_TYPE_ATTRS`: an attribute's length semantics
     * stay unknown.
     */
    LEGACY("NULL", "hidden_column = 'NO'", "NULL"),

    /** 12c and later. */
    CURRENT("SYS_CONTEXT('USERENV', 'CON_NAME')", "user_generated = 'YES'", "char_used");

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

    /** The object types of [scope]: what `TYPECODE` says each is, and its supertype. */
    fun typesSql(scope: OracleTypeScope): String =
        "SELECT owner, type_name, typecode, supertype_owner, supertype_name FROM all_types WHERE ${scope.condition}"

    /** The attributes the types of [scope] declare themselves (not inherited ones), in order; `CHAR_USED` is NULL for [LEGACY]. */
    fun typeAttributesSql(scope: OracleTypeScope): String =
        "SELECT owner, type_name, attr_name, attr_type_mod, attr_type_owner, attr_type_name, length, precision, scale, $attributeCharUsed " +
            "FROM all_type_attrs WHERE inherited = 'NO' AND ${scope.condition} ORDER BY owner, type_name, attr_no"

    /** The methods the types of [scope] declare themselves (not inherited ones), overloads apart by `METHOD_NO`. */
    fun typeMethodsSql(scope: OracleTypeScope): String =
        "SELECT owner, type_name, method_name, method_no, results FROM all_type_methods " +
            "WHERE inherited = 'NO' AND ${scope.condition} ORDER BY owner, type_name, method_no"

    /** The parameters of the methods of [scope], `SELF` of a member method included, in order. */
    fun methodParametersSql(scope: OracleTypeScope): String =
        "SELECT owner, type_name, method_name, method_no, param_name, param_mode, param_type_mod, param_type_owner, param_type_name " +
            "FROM all_method_params WHERE ${scope.condition} ORDER BY owner, type_name, method_no, param_no"

    /** The results of the functions of [scope]. */
    fun methodResultsSql(scope: OracleTypeScope): String =
        "SELECT owner, type_name, method_name, method_no, result_type_mod, result_type_owner, result_type_name " +
            "FROM all_method_results WHERE ${scope.condition}"

    /**
     * The `ALL_ARGUMENTS` rows of the packages and standalone subprograms of [scope] (an object's name is its package's,
     * else its own), the parts of composite parameters left out. `DEFAULTED` is read only when [defaulted]: releases
     * before 11g have no such column.
     */
    fun argumentsSql(scope: OracleTypeScope, defaulted: Boolean): String =
        "SELECT owner, package_name, object_name, subprogram_id, overload, argument_name, position, in_out, data_type, pls_type, " +
            "type_owner, type_name, type_subname, ${if (defaulted) "defaulted" else "NULL"} FROM all_arguments " +
            "WHERE data_level = 0 AND ${scope.on("NVL(package_name, object_name)")} ORDER BY owner, package_name, object_name, subprogram_id, position"

    companion object {
        fun forMajorVersion(major: Int): OracleDictionaryDialect = if (major < 12) LEGACY else CURRENT
    }
}
