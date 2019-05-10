/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.statements

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class AssignmentStatementTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.ASSIGNMENT_STATEMENT)
    }

    @Test
    fun assignmentToVariable() {
        assertThat(p).matches("var := 1;")
    }

    @Test
    fun assignmentToRecordAttribute() {
        assertThat(p).matches("rec.attribute := 1;")
    }

    @Test
    fun assignmentToCollectionElement() {
        assertThat(p).matches("collection(1) := 1;")
    }

    @Test
    fun assignmentToItemInRecordCollection() {
        assertThat(p).matches("collection(1).field := 1;")
    }

    @Test
    fun assignmentToHostVariable() {
        assertThat(p).matches(":var := 1;")
    }

    @Test
    fun assignmentToIndicatorVariable() {
        assertThat(p).matches(":var:indicator := 1;")
    }

    @Test
    fun matchesLabeledAssignment() {
        assertThat(p).matches("<<foo>> var := 1;")
    }

}
