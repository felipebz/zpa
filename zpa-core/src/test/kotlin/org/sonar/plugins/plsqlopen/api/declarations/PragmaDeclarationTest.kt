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
package org.sonar.plugins.plsqlopen.api.declarations

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class PragmaDeclarationTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.PRAGMA_DECLARATION)
    }

    @Test
    fun matchesAutonomousTransaction() {
        assertThat(p).matches("pragma autonomous_transaction;")
    }

    @Test
    fun matchesExceptionInit() {
        assertThat(p).matches("pragma exception_init(1, 1);")
    }

    @Test
    fun matchesSeriallyReusable() {
        assertThat(p).matches("pragma serially_reusable;")
    }

    @Test
    fun matchesInterface() {
        assertThat(p).matches("pragma interface(c, func, 1);")
    }

    @Test
    fun matchesRestrictReferencesPragma() {
        assertThat(p).matches("pragma restrict_references(foo, rnds, wnds);")
    }

    @Test
    fun matchesUdfPragma() {
        assertThat(p).matches("pragma udf;")
    }

}
