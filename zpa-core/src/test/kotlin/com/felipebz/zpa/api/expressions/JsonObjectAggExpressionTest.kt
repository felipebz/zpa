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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class JsonObjectAggExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleJsonObjectAgg() {
        assertThat(p).matches("json_objectagg(k value 'v')")
    }

    @Test
    fun matchesJsonObjectAggWithExplicitKey() {
        assertThat(p).matches("json_objectagg(key k value 'v')")
    }

    @Test
    fun matchesJsonObjectAggWithFormatJson() {
        assertThat(p).matches("json_objectagg('k' value '{}' format json)")
    }

    @Test
    fun matchesJsonObjectAggWithReturning() {
        assertThat(p).matches("json_objectagg(k value 'v' returning clob)")
    }

    @Test
    fun matchesJsonObjectAggWithAbsentOnNull() {
        assertThat(p).matches("json_objectagg('v' value val absent on null)")
    }

    @Test
    fun matchesLongJsonObjectAgg() {
        assertThat(p).matches("""json_objectagg('a' value '{}' format json
            null on null
            returning varchar2(200)
            strict with unique keys)""")
    }

}
