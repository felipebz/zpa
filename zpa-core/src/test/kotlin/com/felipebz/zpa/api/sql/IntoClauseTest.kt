//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntoClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.INTO_CLAUSE)
    }

    @Test
    fun matchesSimpleIntoClause() {
        assertThat(p).matches("into var")
    }

    @Test
    fun matchesIntoClauseInMultipleVariables() {
        assertThat(p).matches("into var, var2")
    }

    @Test
    fun matchesBulkCollect() {
        assertThat(p).matches("bulk collect into var")
    }

    @Test
    fun matchesSimpleCollectionElement() {
        assertThat(p).matches("into col(1)")
    }

    @Test
    fun matchesSimpleIntoRecordItem() {
        assertThat(p).matches("into col.it")
    }

    @Test
    fun matchesSimpleIntoItemInRecordCollection() {
        assertThat(p).matches("into col(1).it")
    }

}
