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
package com.felipebz.zpa.oracle.metadata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OracleDictionaryDialectTest {

    @Test
    fun dialectFollowsTheMajorVersion() {
        assertThat(listOf(10, 11).map(OracleDictionaryDialect::forMajorVersion)).containsOnly(OracleDictionaryDialect.LEGACY)
        assertThat(listOf(12, 19, 23).map(OracleDictionaryDialect::forMajorVersion)).containsOnly(OracleDictionaryDialect.CURRENT)
    }

    /** The SQL used before dialects existed, verbatim: 12c and later must not change. */
    @Test
    fun currentDialectIsTheExistingSql() {
        val d = OracleDictionaryDialect.CURRENT
        assertThat(d.sessionSql).isEqualTo(
            "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER'), SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA'), " +
                "SYS_CONTEXT('USERENV', 'CON_NAME'), SYS_CONTEXT('USERENV', 'DB_UNIQUE_NAME') FROM dual"
        )
        assertThat(d.schemaRelationsSql).isEqualTo(
            "SELECT owner, table_name, table_type_owner, table_type FROM all_all_tables " +
                "WHERE owner = ? AND (dropped IS NULL OR dropped = 'NO') " +
                "UNION ALL SELECT owner, view_name, NULL, NULL FROM all_views WHERE owner = ?"
        )
        assertThat(d.schemaColumnsSql).isEqualTo(COLUMN_LIST + "WHERE owner = ? AND user_generated = 'YES' ORDER BY table_name, internal_column_id")
        assertThat(d.targetedRelationsSql(2)).isEqualTo(
            "SELECT owner, table_name, table_type_owner, table_type FROM all_all_tables " +
                "WHERE (owner, table_name) IN ((?, ?), (?, ?)) AND (dropped IS NULL OR dropped = 'NO') " +
                "UNION ALL SELECT owner, view_name, NULL, NULL FROM all_views WHERE (owner, view_name) IN ((?, ?), (?, ?))"
        )
        assertThat(d.targetedColumnsSql(2)).isEqualTo(
            COLUMN_LIST + "WHERE (owner, table_name) IN ((?, ?), (?, ?)) AND user_generated = 'YES' " +
                "ORDER BY owner, table_name, internal_column_id"
        )
    }

    @Test
    fun legacyDialectAvoidsTwelveCDictionaryColumns() {
        val d = OracleDictionaryDialect.LEGACY
        val all = listOf(d.sessionSql, d.schemaRelationsSql, d.schemaColumnsSql, d.targetedRelationsSql(2), d.targetedColumnsSql(2))
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

    private companion object {
        const val COLUMN_LIST =
            "SELECT owner, table_name, column_name, column_id, data_type, data_type_owner, data_length, char_length, " +
                "char_used, data_precision, data_scale FROM all_tab_cols "
    }
}
