//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
