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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class JsonScalarTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonScalar() {
        assertThat(p).matches("json_scalar(val)")
    }

    @Test
    fun matchesJsonScalarJsonNullOnNull() {
        assertThat(p).matches("json_scalar(val json null on null)")
    }

    @Test
    fun matchesJsonScalarSqlNullOnNull() {
        assertThat(p).matches("json_scalar(val sql null on null)")
    }

    @Test
    fun matchesJsonScalarEmptyStringOnNull() {
        assertThat(p).matches("json_scalar(val empty string on null)")
    }

    @Test
    fun matchesJsonScalarJsonNullOnError() {
        assertThat(p).matches("json_scalar(val null on error)")
    }

    @Test
    fun matchesLongJsonScalar() {
        assertThat(p).matches("json_scalar(val sql null on null error on error)")
    }

}
