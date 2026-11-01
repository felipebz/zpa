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

class JsonScalarTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonScalar() {
        assertThat(p).matches("json_scalar(val)")
    }

    @Test
    fun matchesJsonScalarJsonNullOnNull() {
        assertThat(p).matches("json_scalar(val json null on null)")
    }

    @Test
    fun matchesJsonScalarSqlNullOnNull() {
        assertThat(p).matches("json_scalar(val sql null on null)")
    }

    @Test
    fun matchesJsonScalarEmptyStringOnNull() {
        assertThat(p).matches("json_scalar(val empty string on null)")
    }

    @Test
    fun matchesJsonScalarJsonNullOnError() {
        assertThat(p).matches("json_scalar(val null on error)")
    }

    @Test
    fun matchesLongJsonScalar() {
        assertThat(p).matches("json_scalar(val sql null on null error on error)")
    }

}
