/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.statements

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

class UpdateStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.UPDATE_STATEMENT)
    }

    @Test
    fun matchesSimpleUpdate() {
        assertThat(p).matches("update tab set x = 1;")
    }

    @Test
    fun matchesUpdateWithWhere() {
        assertThat(p).matches("update tab set x = 1 where y = 1;")
    }

    @Test
    fun matchesUpdateWithWhereCurrentOf() {
        assertThat(p).matches("update tab set x = 1 where current of cur;")
    }

    @Test
    fun matchesUpdateMultipleColumns() {
        assertThat(p).matches("update tab set x = 1, y = 1;")
    }

    @Test
    fun matchesUpdateWithAlias() {
        assertThat(p).matches("update tab t set t.x = 1;")
    }

    @Test
    fun matchesUpdateWithSchema() {
        assertThat(p).matches("update sch.tab set sch.tab.x = 1;")
    }

    @Test
    fun matchesLabeledUpdate() {
        assertThat(p).matches("<<foo>> update tab set x = 1;")
    }

    @Test
    fun matchesUpdateWithReturningInto() {
        assertThat(p).matches("update tab set x = 1 returning x into y;")
    }

    @Test
    fun matchesUpdateDefaultValue() {
        assertThat(p).matches("update tab set x = default;")
    }

}
