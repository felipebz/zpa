//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OracleDictionaryDialectTest {

    @Test
    fun dialectFollowsTheMajorVersion() {
        assertThat(listOf(10, 11).map(OracleDictionaryDialect::forMajorVersion)).containsOnly(OracleDictionaryDialect.LEGACY)
        assertThat(listOf(12, 19, 23).map(OracleDictionaryDialect::forMajorVersion)).containsOnly(OracleDictionaryDialect.CURRENT)
    }

    /** The SQL of 12c and later, verbatim. */
    @Test
    fun currentDialectIsTheExistingSql() {
        val d = OracleDictionaryDialect.CURRENT
        assertThat(d.sessionSql).isEqualTo(
            "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER'), SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA'), " +
                "SYS_CONTEXT('USERENV', 'CON_NAME'), SYS_CONTEXT('USERENV', 'DB_UNIQUE_NAME') FROM dual"
        )
        assertThat(d.schemaRelationsSql).isEqualTo(
            "SELECT owner, table_name, table_type_owner, table_type, 'TABLE' FROM all_all_tables " +
                "WHERE owner = ? AND (dropped IS NULL OR dropped = 'NO') " +
                "UNION ALL SELECT owner, view_name, view_type_owner, view_type, 'VIEW' FROM all_views WHERE owner = ?"
        )
        assertThat(d.schemaColumnsSql).isEqualTo(COLUMN_LIST + "WHERE owner = ? AND user_generated = 'YES' ORDER BY table_name, internal_column_id")
        assertThat(d.targetedRelationsSql(2)).isEqualTo(
            "SELECT owner, table_name, table_type_owner, table_type, 'TABLE' FROM all_all_tables " +
                "WHERE (owner, table_name) IN ((?, ?), (?, ?)) AND (dropped IS NULL OR dropped = 'NO') " +
                "UNION ALL SELECT owner, view_name, view_type_owner, view_type, 'VIEW' FROM all_views WHERE (owner, view_name) IN ((?, ?), (?, ?))"
        )
        assertThat(d.targetedColumnsSql(2)).isEqualTo(
            COLUMN_LIST + "WHERE (owner, table_name) IN ((?, ?), (?, ?)) AND user_generated = 'YES' " +
                "ORDER BY owner, table_name, internal_column_id"
        )
    }

    @Test
    fun legacyDialectAvoidsTwelveCDictionaryColumns() {
        val d = OracleDictionaryDialect.LEGACY
        val all = listOf(
            d.sessionSql, d.schemaRelationsSql, d.schemaColumnsSql, d.targetedRelationsSql(2), d.targetedColumnsSql(2),
            d.schemaObjectsSql, d.schemaSynonymsSql, d.publicSynonymsSql, d.targetedPublicSynonymsSql(2)
        )
        assertThat(all).noneMatch { it.contains("CON_NAME", ignoreCase = true) || it.contains("user_generated", ignoreCase = true) }
        // Same four session columns, in the same order; the container is NULL.
        assertThat(d.sessionSql).isEqualTo(
            "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER'), SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA'), " +
                "NULL, SYS_CONTEXT('USERENV', 'DB_UNIQUE_NAME') FROM dual"
        )
        assertThat(d.schemaColumnsSql).isEqualTo(COLUMN_LIST + "WHERE owner = ? AND hidden_column = 'NO' ORDER BY table_name, internal_column_id")
        assertThat(d.targetedColumnsSql(2)).isEqualTo(
            COLUMN_LIST + "WHERE (owner, table_name) IN ((?, ?), (?, ?)) AND hidden_column = 'NO' " +
                "ORDER BY owner, table_name, internal_column_id"
        )
        // Relation queries do not differ.
        assertThat(d.schemaRelationsSql).isEqualTo(OracleDictionaryDialect.CURRENT.schemaRelationsSql)
        assertThat(d.targetedRelationsSql(3)).isEqualTo(OracleDictionaryDialect.CURRENT.targetedRelationsSql(3))
    }

    @Test
    fun objectAndSynonymQueriesBindNamesAndDoNotDiffer() {
        val d = OracleDictionaryDialect.CURRENT
        assertThat(d.schemaObjectsSql).isEqualTo(
            "SELECT owner, object_name, object_type FROM all_objects WHERE owner = ? AND object_type IN " +
                "('TABLE', 'MATERIALIZED VIEW', 'VIEW', 'SYNONYM', 'PACKAGE', 'TYPE', 'SEQUENCE', 'PROCEDURE', 'FUNCTION')"
        )
        assertThat(d.schemaSynonymsSql).isEqualTo(SYNONYM_LIST + "WHERE owner = ?")
        assertThat(d.publicSynonymsSql).isEqualTo(SYNONYM_LIST + "WHERE owner = 'PUBLIC'")
        assertThat(d.targetedPublicSynonymsSql(3)).isEqualTo(SYNONYM_LIST + "WHERE owner = 'PUBLIC' AND table_owner IN (?, ?, ?)")
        val legacy = OracleDictionaryDialect.LEGACY
        assertThat(listOf(legacy.schemaObjectsSql, legacy.schemaSynonymsSql, legacy.publicSynonymsSql, legacy.targetedPublicSynonymsSql(2)))
            .isEqualTo(listOf(d.schemaObjectsSql, d.schemaSynonymsSql, d.publicSynonymsSql, d.targetedPublicSynonymsSql(2)))
    }

    private companion object {
        const val COLUMN_LIST =
            "SELECT owner, table_name, column_name, column_id, data_type, data_type_owner, data_length, char_length, " +
                "char_used, data_precision, data_scale FROM all_tab_cols "
        const val SYNONYM_LIST = "SELECT owner, synonym_name, table_owner, table_name, db_link FROM all_synonyms "
    }
}
