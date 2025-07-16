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

class CastExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleCast() {
        assertThat(p).matches("cast(foo as number)")
    }

    @Test
    fun matchesCastWithDefaultClause() {
        assertThat(p).matches("cast(foo as number default 0 on conversion error)")
    }

    @Test
    fun matchesCastWithMultipleArguments() {
        assertThat(p).matches("cast('01/01/2000' as date, 'dd/mm/yyyy', 'NLS_DATE_LANGUAGE = American')")
    }

    @Test
    fun matchesCastMultiset() {
        assertThat(p).matches("cast(multiset (select 1 from dual) as number)")
    }

    @Test
    fun matchesComplexCast() {
        assertThat(p).matches("cast((cast(localtimestamp as domain timestamp with time zone validate) at time zone 'gmt') at time zone custom_zone as timestamp default 0 on conversion error)")
    }

}
