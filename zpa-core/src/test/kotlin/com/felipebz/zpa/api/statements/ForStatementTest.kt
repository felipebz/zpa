/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class ForStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FOR_STATEMENT)
    }

    @Test
    fun matchesForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesReverseForLoop() {
        assertThat(p).matches(""
                + "for i in reverse 1..2 loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursor() {
        assertThat(p).matches(""
                + "for i in cur loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursorWithPackage() {
        assertThat(p).matches(""
                + "for i in pack.cur loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursorWithPackageAndSchema() {
        assertThat(p).matches(""
                + "for i in sch.pack.cur loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursorWithParameters() {
        assertThat(p).matches(""
                + "for i in cur(x) loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursorWithMultipleParameters() {
        assertThat(p).matches(""
                + "for i in cur(x, y) loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInCursorWithExplicitParameters() {
        assertThat(p).matches(""
                + "for i in cur(p1 => x) loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesForInQuery() {
        assertThat(p).matches(""
            + "for i in (select col from tab) loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesNestedForLoop() {
        assertThat(p).matches(""
                + "for i in 1..2 loop "
                + "for i in 1..2 loop "
                + "null; "
                + "end loop; "
                + "end loop;")
    }

    @Test
    fun matchesForLoopWithExpressionInBothSides() {
        assertThat(p).matches(""
                + "for i in 1 + 1 .. 2 + 2 loop "
                + "null; "
                + "end loop;")
    }

    @Test
    fun matchesLabeledForLoop() {
        assertThat(p).matches(""
                + "<<foo>> for i in 1..2 loop "
                + "null; "
                + "end loop foo;")
    }

    @Test
    fun matchesForLoopWithIncrement() {
        assertThat(p).matches(""
            + "for i in 1..2 by 2 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithFloatIncrement() {
        assertThat(p).matches(""
            + "for i in 1..2 by 0.2 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesReverseForLoopWithIncrement() {
        assertThat(p).matches(""
            + "for i in reverse 1..2 by 0.2 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithExplicitDatatype() {
        assertThat(p).matches(""
            + "for i number in 1..2 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithSkipCondition() {
        assertThat(p).matches(""
            + "for i in 1..10 when val <> 5 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithMutipleIterationControl() {
        assertThat(p).matches(""
            + "for i in 1, i * 2 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithRepeatExpression() {
        assertThat(p).matches(""
            + "for i in 1, repeat i * 2 while i < 100 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithMutableVariable() {
        assertThat(p).matches(""
            + "for i mutable in 1..10 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithMutableVariableAndExplicitDatatype() {
        assertThat(p).matches(""
            + "for i mutable number(2) in 1..10 loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopIndicesOf() {
        assertThat(p).matches(""
            + "for i in indices of arr loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopValuesOf() {
        assertThat(p).matches(""
            + "for i in values of arr loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopPairsOf() {
        assertThat(p).matches(""
            + "for i, val in pairs of arr loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesForLoopWithExecuteImmediate() {
        assertThat(p).matches(""
            + "for i in (execute immediate 'select col from tab') loop "
            + "null; "
            + "end loop;")
    }

    @Test
    fun matchesComplexForLoop() {
        assertThat(p).matches(""
            + "for i in reverse 1..3," +
            "      repeat i*2 while i <= 8," +
            "      indices of arr loop "
            + "null; "
            + "end loop;")
    }

}
