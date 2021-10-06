/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

class InlineConstraintTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.INLINE_CONSTRAINT)
    }

    @Test
    fun matchesNotNull() {
        assertThat(p).matches("not null")
    }

    @Test
    fun matchesNull() {
        assertThat(p).matches("null")
    }

    @Test
    fun matchesUnique() {
        assertThat(p).matches("unique")
    }

    @Test
    fun matchesPrimaryKey() {
        assertThat(p).matches("primary key")
    }

    @Test
    fun matchesReferences() {
        assertThat(p).matches("references tab (col)")
    }

    @Test
    fun matchesReferencesWithSchema() {
        assertThat(p).matches("references sch.tab (col)")
    }

    @Test
    fun matchesReferencesWithMoreColumns() {
        assertThat(p).matches("references tab (col, col2, col3)")
    }

    @Test
    fun matchesReferencesOnDeleteCascade() {
        assertThat(p).matches("references tab (col) on delete cascade")
    }

    @Test
    fun matchesReferencesOnDeleteSetNull() {
        assertThat(p).matches("references tab (col) on delete set null")
    }

    @Test
    fun matchesCheck() {
        assertThat(p).matches("check (x > 1)")
    }

    @Test
    fun matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key")
    }

}
