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
package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class PragmaDeclarationTest : RuleTest() {

    @BeforeEach
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

    @Test
    fun matchesDeprecatePragma() {
        assertThat(p).matches("pragma deprecate(object);")
        assertThat(p).matches("pragma deprecate(object, 'object is deprecated');")
    }

    @Test
    fun matchesCoveragePragma() {
        assertThat(p).matches("pragma coverage('NOT_FEASIBLE_START');")
    }

}
