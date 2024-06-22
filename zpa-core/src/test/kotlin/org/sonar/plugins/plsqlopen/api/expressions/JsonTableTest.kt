/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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

class JsonTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonWithValueColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 path '$.c1'))")
    }

    @Test
    fun matchesJsonWithExistsColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 exists path '$.c1'))")
    }

    @Test
    fun matchesJsonWithQueryColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 format json path '$.c1'))")
    }

    @Test
    fun matchesJsonWithNestedColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (nested '$.c1' columns (c2 path '$.c2')))")
    }

    @Test
    fun matchesJsonWithOrdinalityColumn() {
        assertThat(p).matches("json_table(doc, '$' columns (c1 for ordinality))")
    }

    @Test
    fun matchesJsonWithColumnWithoutParenthesis() {
        assertThat(p).matches("json_table(doc, '$' columns c1 for ordinality)")
    }

    @Test
    fun matchesLongJsonTable() {
        assertThat(p).matches("""
            json_table(doc, '$'
            error on error
            type strict
            error on empty
            columns (
              c1 clob truncate path '$.c1' error on error error on empty error on mismatch(missing data),
              c1 varchar2(10) format json allow scalars without wrapper path '$.c1' error on error,
              c1 number exists path '$.c1' error on error error on empty,
              nested path subDoc[*] columns (c2 path '$.c2'),
              c1 for ordinality
            ))
            """)
    }

}
