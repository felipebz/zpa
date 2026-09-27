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

class FlashbackTableTest : RuleTest() {

    @Test
    fun matchesPointInTimeForms() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).matches("flashback table employees to restore point good_data;")
        assertThat(p).matches("flashback table employees to restore point \"Rp 1\"")
        assertThat(p).matches("flashback table t to scn 1")
        assertThat(p).matches("flashback table t to scn 1 + 2")
        assertThat(p).matches("flashback table t to scn (1)")
        assertThat(p).matches("flashback table t to scn abs(1)")
        assertThat(p).matches("flashback table t to scn :x")
        assertThat(p).matches("flashback table t to scn (select 1 from dual)")
        assertThat(p).matches("flashback table employees_test to timestamp (systimestamp - interval '1' minute)")
        assertThat(p).matches("flashback table hr.employees to timestamp to_timestamp('29-DEC-20 01.26.29.000000000 PM')")
    }

    @Test
    fun matchesTableLists() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).matches("flashback table a, b to scn 1")
        assertThat(p).matches("flashback table a, a to scn 1")
        assertThat(p).matches("flashback table \"S\".\"Nx\", s.t to scn 1")
        // Database links parse; Oracle rejects remote DDL afterwards (ORA-02021).
        assertThat(p).matches("flashback table t@dbl to scn 1")
        assertThat(p).matches("flashback table s.t@dbl to scn 1")
    }

    @Test
    fun matchesTriggerClause() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).matches("flashback table t to scn 1 enable triggers")
        assertThat(p).matches("flashback table t to scn 1 disable triggers")
        assertThat(p).matches("flashback table t to timestamp systimestamp enable triggers")
        assertThat(p).matches("flashback table t to restore point rp disable triggers")
        // Oracle 26 also accepts the singular TRIGGER.
        assertThat(p).matches("flashback table t to scn 1 enable trigger")
        assertThat(p).matches("flashback table t to scn 1 disable trigger")
    }

    @Test
    fun matchesBeforeDrop() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).matches("flashback table print_media to before drop;")
        assertThat(p).matches("flashback table print_media to before drop rename to print_media_old;")
        assertThat(p).matches("flashback table s.t to before drop")
        assertThat(p).matches("flashback table \"BIN\$abc==\$0\" to before drop")
        assertThat(p).matches("flashback table t@dbl to before drop")
        assertThat(p).matches("flashback table t to before drop rename to \"Nx 2\"")
    }

    @Test
    fun rejectsMalformedTableLists() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).notMatches("flashback table to scn 1")
        assertThat(p).notMatches("flashback table t, to scn 1")
        assertThat(p).notMatches("flashback table t,, u to scn 1")
        assertThat(p).notMatches("flashback table , t to scn 1")
        assertThat(p).notMatches("flashback table a.b.c to scn 1")
        assertThat(p).notMatches("flashback t to scn 1")
        assertThat(p).notMatches("flashback table t")
    }

    @Test
    fun rejectsMalformedPointInTime() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).notMatches("flashback table t to 1")
        assertThat(p).notMatches("flashback table t to scn")
        assertThat(p).notMatches("flashback table t to timestamp")
        assertThat(p).notMatches("flashback table t to scn 1, 2")
        assertThat(p).notMatches("flashback table t to scn 1 to scn 2")
        assertThat(p).notMatches("flashback table t to restore point")
        assertThat(p).notMatches("flashback table t to restore point a.b")
        assertThat(p).notMatches("flashback table t to restore rp")
        assertThat(p).notMatches("flashback table t to scn 1 rename to a")
    }

    @Test
    fun rejectsMalformedTriggerClause() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).notMatches("flashback table t to scn 1 enable")
        assertThat(p).notMatches("flashback table t to scn 1 triggers")
        assertThat(p).notMatches("flashback table t to scn 1 enable triggers enable triggers")
        assertThat(p).notMatches("flashback table t to scn 1 enable triggers disable triggers")
        assertThat(p).notMatches("flashback table t enable triggers to scn 1")
    }

    @Test
    fun rejectsMalformedBeforeDrop() {
        setRootRule(DdlGrammar.FLASHBACK_TABLE)
        assertThat(p).notMatches("flashback table a, b to before drop")
        assertThat(p).notMatches("flashback table t to before")
        assertThat(p).notMatches("flashback table t to before drop enable triggers")
        assertThat(p).notMatches("flashback table t to before drop rename to u disable triggers")
        assertThat(p).notMatches("flashback table t to before drop rename u")
        assertThat(p).notMatches("flashback table t to before drop rename to")
        assertThat(p).notMatches("flashback table t to before drop rename to s.u")
        assertThat(p).notMatches("flashback table t to before drop rename to u@dbl")
        assertThat(p).notMatches("flashback table t to before drop rename to a, b")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("flashback table employees to restore point good_data;")
        assertThat(p).matches("flashback table t to scn 1 enable triggers;")
        assertThat(p).matches("flashback table t to before drop rename to u;")
    }
}
