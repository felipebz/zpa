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

class JsonTextContainsConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleJsonTextcontains() {
        assertThat(p).matches("json_textcontains(doc, '$.a', val)")
    }

    @Test
    fun notMatchesJsonTextcontainsIfPathIsNotString() {
        assertThat(p).notMatches("json_textcontains(doc, path, val)")
    }

}
