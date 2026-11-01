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

class WhereClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.WHERE_CLAUSE)
    }

    @Test
    fun matchesSimpleWhere() {
        assertThat(p).matches("where 1 = 1")
    }

    @Test
    fun matchesColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2")
    }

    @Test
    fun matchesMultipleColumnComparation() {
        assertThat(p).matches("where tab.col = tab2.col2 and tab.col = tab2.col2")
    }

    @Test
    fun matchesComparationWithSubquery() {
        assertThat(p).matches("where tab.col = (select 1 from dual)")
    }

    @Test
    fun matchesOuterJoin() {
        assertThat(p).matches("where tab.col(+) = tab2.col2")
    }

    @Test
    fun matchesTupleInSelect() {
        assertThat(p).matches("where (foo, bar, baz) in (select 1, 2, 3 from dual)")
    }

    @Test
    fun matchesTupleInValues() {
        assertThat(p).matches("where (foo, bar) in ((1, 2), (2, 3))")
    }

    @Test
    fun matchesExists() {
        assertThat(p).matches("where exists (select 1 from dual)")
    }

    @Test
    fun matchesExistsWithUnionInSubquery() {
        assertThat(p).matches("where exists ((select 1 from dual) union (select 2 from dual))")
    }

    @Test
    fun matchesNotExists() {
        assertThat(p).matches("where not exists (select 1 from dual)")
    }

    @Test
    fun matchesAny() {
        assertThat(p).matches("where data = any (select 1,2,3 from dual)")
    }
}
