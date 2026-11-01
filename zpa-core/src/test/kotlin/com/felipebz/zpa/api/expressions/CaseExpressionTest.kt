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

class CaseExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
    }

    @Test
    fun matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then 1 end")
    }

    @Test
    fun matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then 1 end")
    }

    @Test
    fun matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then 1 when 2 then 2 end")
    }

    @Test
    fun matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then 1 else 2 end")
    }

    @Test
    fun matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then 1 end")
    }

    @Test
    fun matchesCaseWithSelectorExpression() {
        assertThat(p).matches("case foo + bar when 1 then 1 end")
    }

    @Test
    fun matchesBooleanSearchedCase() {
        assertThat(p).matches("case when foo is not null and bar is null then 1 end")
    }

}
