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
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class FlashbackArchiveTest : RuleTest() {

    @Test
    fun matchesCreateFlashbackArchive() {
        setRootRule(DdlGrammar.CREATE_FLASHBACK_ARCHIVE)
        // Documented example with DEFAULT and QUOTA.
        assertThat(p).matches("create flashback archive default test_archive1 " +
            "tablespace example quota 1 m retention 1 day;")
        // Without DEFAULT.
        assertThat(p).matches("create flashback archive test_archive2 " +
            "tablespace example quota 1 m retention 1 day;")
        // Without QUOTA (unlimited).
        assertThat(p).matches("create flashback archive test_archive3 " +
            "tablespace example retention 1 day;")
        // RETENTION YEAR and MONTH.
        assertThat(p).matches("create flashback archive test_y tablespace tbs retention 1 year;")
        assertThat(p).matches("create flashback archive test_m tablespace tbs retention 1 month;")
        // QUOTA with G/T/P/E size units.
        assertThat(p).matches("create flashback archive test_g tablespace tbs quota 10 g retention 1 day")
        assertThat(p).matches("create flashback archive test_t tablespace tbs quota 1 t retention 1 day")
        // OPTIMIZE DATA and NO OPTIMIZE DATA.
        assertThat(p).matches("create flashback archive test_opt tablespace tbs " +
            "optimize data retention 1 year")
        assertThat(p).matches("create flashback archive test_nopt tablespace tbs " +
            "no optimize data retention 1 month")
        // QUOTA + OPTIMIZE DATA + RETENTION (full form).
        assertThat(p).matches("create flashback archive default full_test tablespace tbs " +
            "quota 100 m optimize data retention 2 year;")
    }

    @Test
    fun rejectsInvalidCreateFlashbackArchive() {
        setRootRule(DdlGrammar.CREATE_FLASHBACK_ARCHIVE)
        // No TABLESPACE (mandatory per production).
        assertThat(p).notMatches("create flashback archive test_no_ts retention 1 day")
        // No RETENTION (mandatory per production).
        assertThat(p).notMatches("create flashback archive test_no_ret tablespace tbs quota 1 m")
        // OR REPLACE (ORA-00922).
        assertThat(p).notMatches("create or replace flashback archive test_or tablespace tbs retention 1 day")
        // Schema-qualified name (ORA-55603).
        assertThat(p).notMatches("create flashback archive hr.test_fa tablespace tbs retention 1 day")
        // DEFAULT after name (ORA-55603, strict ordering).
        assertThat(p).notMatches("create flashback archive test_da default tablespace tbs retention 1 day")
        // RETENTION before TABLESPACE (ORA-55603, strict ordering).
        assertThat(p).notMatches("create flashback archive test_rbt retention 1 day tablespace tbs")
        // QUOTA before TABLESPACE (ORA-55603, strict ordering).
        assertThat(p).notMatches("create flashback archive test_qt quota 1 m tablespace tbs retention 1 day")
    }

    @Test
    fun matchesAlterFlashbackArchive() {
        setRootRule(DdlGrammar.ALTER_FLASHBACK_ARCHIVE)
        assertThat(p).matches("alter flashback archive test_archive1 set default;")
        assertThat(p).matches("alter flashback archive test_archive1 add tablespace tbs;")
        assertThat(p).matches("alter flashback archive test_archive1 add tablespace tbs quota 10 m;")
        assertThat(p).matches("alter flashback archive test_archive1 modify tablespace tbs quota 20 m;")
        assertThat(p).matches("alter flashback archive test_archive1 remove tablespace tbs;")
        assertThat(p).matches("alter flashback archive test_archive1 modify retention 1 month;")
        assertThat(p).matches("alter flashback archive test_archive1 purge all;")
        assertThat(p).matches("alter flashback archive test_archive1 purge before scn 123456;")
        assertThat(p).matches("alter flashback archive test_archive1 " +
            "purge before timestamp to_timestamp('2020-01-01','YYYY-MM-DD');")
        assertThat(p).matches("alter flashback archive test_archive1 optimize data;")
        assertThat(p).matches("alter flashback archive test_archive1 no optimize data;")
    }

    @Test
    fun rejectsInvalidAlterFlashbackArchive() {
        setRootRule(DdlGrammar.ALTER_FLASHBACK_ARCHIVE)
        // No action (ORA-00921).
        assertThat(p).notMatches("alter flashback archive test_archive1")
        // Two actions (ORA-03048).
        assertThat(p).notMatches("alter flashback archive test_archive1 set default modify retention 1 month")
        // Schema-qualified name (ORA-55603).
        assertThat(p).notMatches("alter flashback archive hr.test_fa set default")
    }

    @Test
    fun matchesAlterTableFlashbackArchive() {
        setRootRule(DdlGrammar.ALTER_TABLE)
        // Bare FLASHBACK ARCHIVE (uses default archive).
        assertThat(p).matches("alter table oe.customers flashback archive;")
        // Named archive.
        assertThat(p).matches("alter table oe.orders flashback archive test_archive2;")
        // NO FLASHBACK ARCHIVE.
        assertThat(p).matches("alter table oe.orders no flashback archive;")
    }

    @Test
    fun rejectsInvalidAlterTableFlashbackArchive() {
        setRootRule(DdlGrammar.ALTER_TABLE)
        // NO FLASHBACK ARCHIVE does not take a name.
        assertThat(p).notMatches("alter table oe.orders no flashback archive test_archive2")
    }
}
