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

class JsonExistsConditionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(ConditionsGrammar.CONDITION)
    }

    @Test
    fun matchesSimpleJsonExists() {
        assertThat(p).matches("json_exists(doc, '$.a')")
    }

    @Test
    fun matchesJsonExistsFormatJson() {
        assertThat(p).matches("json_exists(doc format json, '$.a')")
    }

    @Test
    fun matchesJsonExistsWithPassing() {
        assertThat(p).matches("json_exists(doc, '$.a' passing var1 as a, var2 as b)")
    }

    @Test
    fun matchesJsonExistsOnError() {
        assertThat(p).matches("json_exists(doc, '$.a' error on error)")
    }

    @Test
    fun matchesJsonExistsStrict() {
        assertThat(p).matches("json_exists(doc, '$.a' type (strict))")
    }

    @Test
    fun matchesJsonExistsOnEmpty() {
        assertThat(p).matches("json_exists(doc, '$.a' error on empty)")
    }

    @Test
    fun matchesLongJsonExists() {
        assertThat(p).matches("""
            json_exists(doc format json, '$.a'
            passing var1 as a, var2 as b
            false on error
            true on empty
            type (lax))
            """)
    }

}
