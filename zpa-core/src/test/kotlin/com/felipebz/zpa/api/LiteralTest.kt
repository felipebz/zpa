/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiteralTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.LITERAL)
    }

    @Test
    fun matchesEveryLiteralBranch() {
        listOf(
            "NULL",
            "TRUE",
            "FALSE",
            "42",
            "42.5",
            "BINARY_DOUBLE_INFINITY",
            "BINARY_DOUBLE_NAN",
            "BINARY_FLOAT_INFINITY",
            "BINARY_FLOAT_NAN",
            "'text'",
            "DATE '2026-09-05'",
            "TIMESTAMP '2026-09-05 12:34:56'",
            "INTERVAL '4' YEAR",
            "\$\$PLSQL_UNIT"
        ).forEach { assertThat(p).matches(it) }
    }

    @Test
    fun preservesLiteralBoundaries() {
        assertThat(p).notMatches("42 + 1")
        assertThat(p).notMatches("DATE")
        assertThat(p).notMatches("INTERVAL '4'")
        assertThat(p).notMatches("UNKNOWN")
    }
}
