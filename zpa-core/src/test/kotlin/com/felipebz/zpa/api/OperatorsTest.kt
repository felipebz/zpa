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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperatorsTest : RuleTest() {

    @Test
    fun concatenation() {
        setRootRule(PlSqlGrammar.CONCATENATION_OPERATOR)
        assertThat(p).matches("||")
    }

    @Test
    fun equals() {
        setRootRule(PlSqlGrammar.EQUALS_OPERATOR)
        assertThat(p).matches("=")
    }

    @Test
    fun notEquals() {
        setRootRule(PlSqlGrammar.NOTEQUALS_OPERATOR)
        assertThat(p).matches("<>")
        assertThat(p).matches("!=")
        assertThat(p).matches("^=")
        assertThat(p).matches("~=") // this operator is supported in Oracle Forms
    }

    @Test
    fun lessThan() {
        setRootRule(PlSqlGrammar.LESSTHAN_OPERATOR)
        assertThat(p).matches("<")
    }

    @Test
    fun lessThanOrEquals() {
        setRootRule(PlSqlGrammar.LESSTHANOREQUALS_OPERATOR)
        assertThat(p).matches("<=")
    }

    @Test
    fun greaterThan() {
        setRootRule(PlSqlGrammar.GREATERTHAN_OPERATOR)
        assertThat(p).matches(">")
    }

    @Test
    fun greaterThanOrEquals() {
        setRootRule(PlSqlGrammar.GREATERTHANOREQUALS_OPERATOR)
        assertThat(p).matches(">=")
    }

}
