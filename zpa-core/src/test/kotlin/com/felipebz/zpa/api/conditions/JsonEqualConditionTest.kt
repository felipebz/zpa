//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.conditions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.ConditionsGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JsonEqualConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleJsonEqual() {
        assertThat(p).matches("json_equal('{}', '{}')")
    }

    @Test
    fun matchesJsonEqualOnErrorClause() {
        assertThat(p).matches("json_equal('{}', '{}' error on error)")
        assertThat(p).matches("json_equal('{}', '{}' true on error)")
        assertThat(p).matches("json_equal('{}', '{}' false on error)")
    }

}
