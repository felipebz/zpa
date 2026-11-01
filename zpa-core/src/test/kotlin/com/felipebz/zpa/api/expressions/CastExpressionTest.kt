//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
