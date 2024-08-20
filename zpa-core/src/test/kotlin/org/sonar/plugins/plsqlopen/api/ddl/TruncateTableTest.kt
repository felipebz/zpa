/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class TruncateTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.TRUNCATE_TABLE)
    }

    @Test
    fun matchesSimpleTruncate() {
        assertThat(p).matches("truncate table foo;")
    }

    @Test
    fun matchesTruncatePreserveMaterializedViewLog() {
        assertThat(p).matches("truncate table foo preserve materialized view log;")
    }

    @Test
    fun matchesTruncatePurgeMaterializedViewLog() {
        assertThat(p).matches("truncate table foo purge materialized view log;")
    }

    @Test
    fun matchesTruncateDropStorage() {
        assertThat(p).matches("truncate table foo drop storage;")
        assertThat(p).matches("truncate table foo drop all storage;")
    }

    @Test
    fun matchesTruncateReuseStorage() {
        assertThat(p).matches("truncate table foo reuse storage;")
    }

    @Test
    fun matchesTruncateCascade() {
        assertThat(p).matches("truncate table foo cascade;")
    }

    @Test
    fun matchesLongTruncate() {
        assertThat(p).matches("truncate table sch.foo  purge materialized view log drop all storage cascade;")
    }

}
