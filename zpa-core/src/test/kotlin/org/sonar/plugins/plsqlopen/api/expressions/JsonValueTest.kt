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

class JsonValueTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonValue() {
        assertThat(p).matches("json_value(doc, '$')")
    }

    @Test
    fun matchesJsonValueFormatJson() {
        assertThat(p).matches("json_value('{}' format json, '$')")
    }

    @Test
    fun matchesJsonValueWithPassingClause() {
        assertThat(p).matches("json_value(doc, '$' passing 1 as x, 2 as y)")
    }

    @Test
    fun matchesJsonValueWithReturning() {
        assertThat(p).matches("json_value(doc, '$' returning clob)")
    }

    @Test
    fun matchesJsonValueWithReturningAscii() {
        assertThat(p).matches("json_value(doc, '$' returning clob ascii)")
    }

    @Test
    fun matchesLongJsonValue() {
        assertThat(p).matches("""
            json_value(doc, '$'
            passing 'a' as a
            returning varchar2(10) truncate
            error on error
            error on empty
            error on mismatch (missing data)
            ignore on mismatch (extra data)
            type strict)""")
    }

}
