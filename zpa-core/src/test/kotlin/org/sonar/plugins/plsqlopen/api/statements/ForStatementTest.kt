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
package org.sonar.plugins.plsqlopen.api.statements

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

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

}
