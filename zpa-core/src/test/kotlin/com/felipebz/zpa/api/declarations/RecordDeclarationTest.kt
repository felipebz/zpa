//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class RecordDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.RECORD_DECLARATION)
    }

    @Test
    fun matchesSimpleRecord() {
        assertThat(p).matches("type foo is record(x number);")
    }

    @Test
    fun matchesRecordWithMultipleFields() {
        assertThat(p).matches("type foo is record(x number, y number);")
    }

    @Test
    fun matchesRecordWithDefaultField() {
        assertThat(p).matches("type foo is record(x number default 1);")
    }

    @Test
    fun matchesRecordWithDefaultFieldAlternative() {
        assertThat(p).matches("type foo is record(x number := 1);")
    }

    @Test
    fun matchesRecordWithNotNullField() {
        assertThat(p).matches("type foo is record(x number not null default 1);")
    }

}
