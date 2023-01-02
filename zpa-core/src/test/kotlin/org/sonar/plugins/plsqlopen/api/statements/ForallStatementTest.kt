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

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class ForallStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FORALL_STATEMENT)
    }

    @Test
    fun matchesForallWithFixedRangeInsert() {
        assertThat(p).matches("forall foo in 1 .. 2 insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallWithFixedRangeUpdate() {
        assertThat(p).matches("forall foo in 1 .. 2 update tab set value = var(foo).value;")
    }

    @Test
    fun matchesForallWithFixedRangeDelete() {
        assertThat(p).matches("forall foo in 1 .. 2 delete tab where value = var(foo).value;")
    }

    @Test
    fun matchesForallWithFixedRangeMerge() {
        assertThat(p).matches("forall foo in 1 .. 2 merge into dest_tab " +
            "using source_tab on (1 = 2) " +
            "when matched then update set col = val " +
            "when not matched then insert values (val);")
    }

    @Test
    fun matchesForallWithFixedRangeExecuteImmediate() {
        assertThat(p).matches("forall foo in 1 .. 2 execute immediate 'insert into tab values (:1)' " +
            "using var(foo).value;")
    }

    @Test
    fun matchesForallWithVariablesRange() {
        assertThat(p).matches("forall x in foo .. bar.count insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesOf() {
        assertThat(p).matches("forall foo in indices of bar insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesOfWithRange() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2 insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallValuesOf() {
        assertThat(p).matches("forall foo in values of bar insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallValuesWithSaveExceptions() {
        assertThat(p).matches("forall foo in values of bar save exceptions insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesWithSaveExceptions() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2 save exceptions insert into tab values (var(foo).value);")
    }

}
