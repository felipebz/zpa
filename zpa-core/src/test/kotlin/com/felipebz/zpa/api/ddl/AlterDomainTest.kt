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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterDomainTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DOMAIN)
    }

    @Test
    fun matchesDisplayAndOrderActions() {
        assertThat(p).matches("alter domain day_of_week modify display lower(day_of_week);")
        assertThat(p).matches("alter domain day_of_week drop display;")
        assertThat(p).matches("alter domain d add display initcap(d)")
        assertThat(p).matches("alter domain year_of_birth modify order mod(year_of_birth, 100)")
        assertThat(p).matches("alter domain d drop order")
        assertThat(p).matches("alter domain d add order floor(d / 100)")
        assertThat(p).matches("alter usecase domain if exists hr.d drop display")
    }

    @Test
    fun matchesAnnotations() {
        assertThat(p).matches("alter domain day_of_week annotations(Display 'Day of week');")
        assertThat(p).matches("alter domain d annotations (add a 'x', drop b, replace c 'y', " +
            "add if not exists d, drop if exists e)")
        assertThat(p).matches("alter domain d annotations (add or replace a 'x')")
    }

    @Test
    fun rejectsFormsOracleRejects() {
        // ORA-00904: an action is required, and only DISPLAY, ORDER and annotations can change.
        assertThat(p).notMatches("alter domain d")
        assertThat(p).notMatches("alter domain d display d")
        assertThat(p).notMatches("alter domain d add default 1")
        assertThat(p).notMatches("alter domain d add constraint c check (d > 0)")
        // ORA-03048 / ORA-03049: one action per statement.
        assertThat(p).notMatches("alter domain d drop display drop order")
        assertThat(p).notMatches("alter domain d drop display annotations (a 'b')")
        assertThat(p).notMatches("alter domain d annotations (a 'x') annotations (b 'y')")
    }
}
