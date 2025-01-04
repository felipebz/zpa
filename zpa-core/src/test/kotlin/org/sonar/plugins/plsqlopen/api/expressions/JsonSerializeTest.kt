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
package org.sonar.plugins.plsqlopen.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class JsonSerializeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonSerialize() {
        assertThat(p).matches("json_serialize(val)")
    }

    @Test
    fun matchesJsonSerializeWithReturning() {
        assertThat(p).matches("json_serialize(val returning clob)")
    }

    @Test
    fun matchesJsonSerializeSqlNullOnError() {
        assertThat(p).matches("json_serialize(val null on error)")
    }

    @Test
    fun matchesJsonSerializeSqlErrorOnError() {
        assertThat(p).matches("json_serialize(val error on error)")
    }

    @Test
    fun matchesJsonSerializeSqlEmptyArrayOnError() {
        assertThat(p).matches("json_serialize(val empty array on error)")
    }

    @Test
    fun matchesJsonSerializeSqlEmptyObjectOnError() {
        assertThat(p).matches("json_serialize(val empty object on error)")
    }

    @Test
    fun matchesLongJsonSerialize() {
        assertThat(p).matches("json_serialize(val returning blob pretty ascii ordered truncate empty object on error)")
    }

}
