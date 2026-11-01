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

class JsonSerializeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonSerialize() {
        assertThat(p).matches("json_serialize(val)")
    }

    @Test
    fun matchesJsonSerializeWithReturning() {
        assertThat(p).matches("json_serialize(val returning clob)")
    }

    @Test
    fun matchesJsonSerializeSqlNullOnError() {
        assertThat(p).matches("json_serialize(val null on error)")
    }

    @Test
    fun matchesJsonSerializeSqlErrorOnError() {
        assertThat(p).matches("json_serialize(val error on error)")
    }

    @Test
    fun matchesJsonSerializeSqlEmptyArrayOnError() {
        assertThat(p).matches("json_serialize(val empty array on error)")
    }

    @Test
    fun matchesJsonSerializeSqlEmptyObjectOnError() {
        assertThat(p).matches("json_serialize(val empty object on error)")
    }

    @Test
    fun matchesLongJsonSerialize() {
        assertThat(p).matches("json_serialize(val returning blob pretty ascii ordered truncate empty object on error)")
    }

}
