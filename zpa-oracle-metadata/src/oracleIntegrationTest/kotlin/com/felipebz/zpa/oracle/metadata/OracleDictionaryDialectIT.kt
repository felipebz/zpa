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

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalRelation
import com.felipebz.zpa.api.project.ProjectIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The legacy (pre-12c) dictionary dialect, forced on this 12c+ database, against the current one. Without invisible
 * columns both read the same metadata; an invisible column is the only difference, and it cannot exist before 12c.
 */
@OptIn(ZpaExperimentalApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleDictionaryDialectIT {
    private lateinit var db: OracleDatabase
    private lateinit var plain: OracleDatabase.User
    private lateinit var invisible: OracleDatabase.User

    private val plainRelations = listOf("ORDINARY", "ORDINARY_VIEW", "ADDR_TAB", "SPECIAL")

    @BeforeAll
    fun setUp() {
        db = OracleDatabase.connect()
        plain = db.createUser("ZPAMD_DIALECT", "CREATE TABLE", "CREATE VIEW", "CREATE TYPE")
        plain.execute(
            "CREATE TABLE ordinary (id NUMBER PRIMARY KEY, name VARCHAR2(30 BYTE), label VARCHAR2(20 CHAR), born DATE, amount NUMBER(10,2))",
            "CREATE VIEW ordinary_view AS SELECT id, name full_name, amount * 2 dbl FROM ordinary",
            "CREATE TYPE addr_t AS OBJECT (street VARCHAR2(30), city VARCHAR2(30))",
            "CREATE TABLE addr_tab OF addr_t",
            // XMLTYPE, object and JSON columns add system-generated hidden columns, filtered by both dialects.
            "CREATE TABLE special (x XMLTYPE, a addr_t, j JSON, r RAW(16))"
        )
        invisible = db.createUser("ZPAMD_INVISIBLE", "CREATE TABLE")
        invisible.execute("CREATE TABLE with_invisible (id NUMBER, secret VARCHAR2(10) INVISIBLE, note VARCHAR2(100))")
    }

    @AfterAll
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun id(s: String) = ProjectIdentifier.fromSource("\"$s\"")

    private fun acquire(user: OracleDatabase.User, dialect: OracleDictionaryDialect, request: OracleMetadataRequest = schema(user)) =
        OracleMetadataAcquisition.acquire(user.dataSource, request, dialect)

    private fun schema(user: OracleDatabase.User) = OracleMetadataRequest.builder().schema(user.name).build()

    private fun relation(result: OracleMetadataResult, schema: String, name: String): ProjectExternalRelation =
        result.metadata.findRelation(id(schema), id(name)).relation.get()

    @Test
    fun theDialectChosenForThisDatabaseIsCurrent() {
        val chosen = OracleMetadataAcquisition.acquire(plain.dataSource, schema(plain))
        val current = acquire(plain, OracleDictionaryDialect.CURRENT)
        assertThat(chosen.container).isEqualTo(current.container).isNotEmpty()
        assertThat(plainRelations.map { relation(chosen, plain.name, it) }).isEqualTo(plainRelations.map { relation(current, plain.name, it) })
    }

    @Test
    fun withoutInvisibleColumnsBothDialectsReadTheSameMetadata() {
        val legacy = acquire(plain, OracleDictionaryDialect.LEGACY)
        val current = acquire(plain, OracleDictionaryDialect.CURRENT)
        assertThat(legacy.relationCount).isEqualTo(current.relationCount).isEqualTo(plainRelations.size)
        assertThat(legacy.columnCount).isEqualTo(current.columnCount)
        assertThat(legacy.completeSchemas).isEqualTo(current.completeSchemas).containsExactly(plain.name)
        for (name in plainRelations) {
            assertThat(relation(legacy, plain.name, name)).describedAs(name).isEqualTo(relation(current, plain.name, name))
        }
        assertThat(legacy.sessionUser).isEqualTo(current.sessionUser)
        assertThat(legacy.sessionCurrentSchema).isEqualTo(current.sessionCurrentSchema)
        assertThat(legacy.database).isEqualTo(current.database).isNotEmpty()
        // The legacy session query has no container.
        assertThat(legacy.container).isEmpty()
        assertThat(current.container).isNotEmpty()

        // Targeted (pair IN list) queries agree too.
        val targeted = OracleMetadataRequest.builder().relation(plain.name, "ORDINARY").relation(plain.name, "SPECIAL").build()
        val legacyOwn = acquire(plain, OracleDictionaryDialect.LEGACY, targeted)
        val currentOwn = acquire(plain, OracleDictionaryDialect.CURRENT, targeted)
        for (name in listOf("ORDINARY", "SPECIAL")) {
            assertThat(relation(legacyOwn, plain.name, name)).describedAs(name).isEqualTo(relation(currentOwn, plain.name, name))
        }
    }

    @Test
    fun anInvisibleColumnIsTheOnlyDifference() {
        val legacy = relation(acquire(invisible, OracleDictionaryDialect.LEGACY), invisible.name, "WITH_INVISIBLE")
        val current = relation(acquire(invisible, OracleDictionaryDialect.CURRENT), invisible.name, "WITH_INVISIBLE")
        // An invisible column is hidden, so the legacy filter drops it; it cannot exist before 12c.
        assertThat(current.columns.map { it.name }).containsExactly(id("ID"), id("NOTE"), id("SECRET"))
        assertThat(legacy.columns).isEqualTo(current.columns.filter { it.name != id("SECRET") })
        assertThat(legacy.rowFields).isEqualTo(current.rowFields)
    }
}
