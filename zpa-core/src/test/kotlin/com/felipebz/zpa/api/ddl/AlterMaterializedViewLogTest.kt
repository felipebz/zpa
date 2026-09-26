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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterMaterializedViewLogTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ALTER_MATERIALIZED_VIEW_LOG)
    }

    @Test
    fun matchesAddAndNewValues() {
        assertThat(p).matches("alter materialized view log on order_items add rowid;")
        assertThat(p).matches("alter materialized view log on employees add (commission_pct) excluding new values;")
        assertThat(p).matches("alter materialized view log on t add rowid, sequence, primary key including new values")
        assertThat(p).matches("alter materialized view log on t add object id")
        assertThat(p).matches("alter materialized view log on t add rowid, (a) including new values")
        assertThat(p).matches("alter materialized view log on t add sequence (a, b) purge immediate")
        assertThat(p).matches("alter materialized view log on t excluding new values purge immediate")
    }

    @Test
    fun matchesMaintenancePurgeAndRefresh() {
        assertThat(p).matches("alter materialized view log on t")
        assertThat(p).matches("alter materialized view log if exists force on hr.t pctfree 5 pctused 40 nocache parallel 2 nologging")
        assertThat(p).matches("alter materialized view log on t allocate extent (size 1m) shrink space compact cascade")
        assertThat(p).matches("alter materialized view log on t move tablespace users parallel 2")
        assertThat(p).matches("alter materialized view log on t pctfree 5 add (a) purge start with sysdate")
        assertThat(p).matches("alter materialized view log on t add (a) excluding new values " +
            "purge start with sysdate next sysdate + 1 for synchronous refresh using stg")
        assertThat(p).matches("alter materialized view log on t purge repeat interval '1' day")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-00969 without ON; ORA-12045 for TABLESPACE; ORA-32418 for COMMIT SCN.
        assertThat(p).notMatches("alter materialized view log t add rowid")
        assertThat(p).notMatches("alter materialized view log on t tablespace users")
        assertThat(p).notMatches("alter materialized view log on t add commit scn")
        // ORA-03048: one column list, last.
        assertThat(p).notMatches("alter materialized view log on t add rowid (a), primary key")
        assertThat(p).notMatches("alter materialized view log on t add (a), (b)")
        // ORA-02000: only NEW VALUES may follow a bare keyword item.
        assertThat(p).notMatches("alter materialized view log on t add rowid purge immediate")
        assertThat(p).notMatches("alter materialized view log on t add rowid for synchronous refresh using s")
        // ORA-00922 / ORA-03048 / ORA-03049: FOR FAST REFRESH and out-of-order clauses.
        assertThat(p).notMatches("alter materialized view log on t for fast refresh")
        assertThat(p).notMatches("alter materialized view log on t purge immediate add rowid")
        assertThat(p).notMatches("alter materialized view log on t purge immediate excluding new values")
        assertThat(p).notMatches("alter materialized view log on t add rowid add primary key")
    }
}
