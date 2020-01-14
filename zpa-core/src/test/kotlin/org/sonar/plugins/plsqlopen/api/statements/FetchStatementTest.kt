/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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

class FetchStatementTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.FETCH_STATEMENT)
    }

    @Test
    fun matchesSimpleFetchInto() {
        assertThat(p).matches("fetch foo into bar;")
    }

    @Test
    fun matchesFetchCursorInPackage() {
        assertThat(p).matches("fetch pack.foo into bar;")
    }

    @Test
    fun matchesFetchHostCursorInto() {
        assertThat(p).matches("fetch :foo into bar;")
    }

    @Test
    fun matchesFetchIntoMultipleVariables() {
        assertThat(p).matches("fetch foo into bar, baz;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectInto() {
        assertThat(p).matches("fetch foo bulk collect into bar;")
    }

    @Test
    fun matchesFetchHostCursorBulkCollectInto() {
        assertThat(p).matches("fetch :foo bulk collect into bar;")
    }

    @Test
    fun matchesFetchBulkCollectIntoMultipleVariables() {
        assertThat(p).matches("fetch foo bulk collect into bar, baz;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectIntoHostVariable() {
        assertThat(p).matches("fetch foo bulk collect into :bar;")
    }

    @Test
    fun matchesSimpleFetchBulkCollectIntoWithLimit() {
        assertThat(p).matches("fetch foo bulk collect into bar limit 10;")
    }

    @Test
    fun matchesLabeledFetchInto() {
        assertThat(p).matches("<<foo>> fetch foo into bar;")
    }

}
