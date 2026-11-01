//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CaseStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
    }

    @Test
    fun matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then foo := bar; end case;")
    }

    @Test
    fun matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then foo := bar; when 2 then foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then foo := bar; else foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithMultipleStataments() {
        assertThat(p).matches("case when x = 1 then foo := bar; bar := baz; end case;")
    }

    @Test
    fun matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesLabeledCase() {
        assertThat(p).matches("<<foo>> case when x = 1 then foo := bar; end case foo;")
    }

    @Test
    fun matchesCaseWithSelectorExpression() {
        assertThat(p).matches("case foo + bar when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesBooleanExpressionSearchedCase() {
        assertThat(p).matches("case when foo is not null and bar is null then foo := bar; end case;")
    }


}
