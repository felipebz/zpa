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

class PurgeTest : RuleTest() {

    @Test
    fun matchesPurgeStatement() {
        setRootRule(DdlGrammar.PURGE_STATEMENT)
        assertThat(p).matches("purge table test;")
        assertThat(p).matches("purge table hr.test;")
        assertThat(p).matches("purge table RB\$\$33750\$TABLE\$0;")
        assertThat(p).matches("purge table \"BIN\$abc==\$0\";")
        assertThat(p).matches("purge index test_idx;")
        assertThat(p).matches("purge index hr.test_idx")
        assertThat(p).matches("purge tablespace ts;")
        assertThat(p).matches("purge tablespace ts user u;")
        assertThat(p).matches("purge tablespace set ts_set;")
        assertThat(p).matches("purge tablespace set ts_set user u")
        assertThat(p).matches("purge recyclebin;")
        assertThat(p).matches("purge dba_recyclebin")
    }

    @Test
    fun rejectsInvalidPurgeStatement() {
        setRootRule(DdlGrammar.PURGE_STATEMENT)
        // Bare PURGE (ORA-38302).
        assertThat(p).notMatches("purge")
        // USER before TABLESPACE (ORA-38302).
        assertThat(p).notMatches("purge user u tablespace ts")
        // Qualified tablespace or user (ORA-38303).
        assertThat(p).notMatches("purge tablespace hr.ts")
        assertThat(p).notMatches("purge tablespace ts user hr.u")
        assertThat(p).notMatches("purge tablespace set hr.ts_set")
        // USER not valid on TABLE/INDEX/RECYCLEBIN (ORA-03048).
        assertThat(p).notMatches("purge table test user u")
        assertThat(p).notMatches("purge index test_idx user u")
        assertThat(p).notMatches("purge recyclebin user u")
        assertThat(p).notMatches("purge dba_recyclebin user u")
    }
}
