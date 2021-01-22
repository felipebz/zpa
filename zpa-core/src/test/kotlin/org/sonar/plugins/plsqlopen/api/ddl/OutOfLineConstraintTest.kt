/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class OutOfLineConstraintTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(DdlGrammar.OUT_OF_LINE_CONSTRAINT)
    }

    @Test
    fun matchesUnique() {
        assertThat(p).matches("unique (foo)")
    }

    @Test
    fun matchesUniqueWithMoreColumns() {
        assertThat(p).matches("unique (foo, bar, baz)")
    }

    @Test
    fun matchesPrimaryKey() {
        assertThat(p).matches("primary key (foo)")
    }

    @Test
    fun matchesPrimaryKeyWithMoreColumns() {
        assertThat(p).matches("primary key (foo, bar, baz)")
    }

    @Test
    fun matchesForeignKey() {
        assertThat(p).matches("foreign key (col) references tab (col)")
    }

    @Test
    fun matchesReferencesWithMoreColumns() {
        assertThat(p).matches("foreign key (col, col2, col3) references tab (col, col2, col3)")
    }

    @Test
    fun matchesCheck() {
        assertThat(p).matches("check (x > 1)")
    }

    @Test
    fun matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key (foo)")
    }

}
