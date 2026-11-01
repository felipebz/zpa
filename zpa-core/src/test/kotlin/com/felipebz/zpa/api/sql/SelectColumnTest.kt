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

class SelectColumnTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.SELECT_COLUMN)
    }

    @Test
    fun matchesAllColumns() {
        assertThat(p).matches("*")
    }

    @Test
    fun matchesFunctionCall() {
        assertThat(p).matches("func(var)")
    }

    @Test
    fun matchesCount() {
        assertThat(p).matches("count(*)")
    }

    @Test
    fun matchesColumnName() {
        assertThat(p).matches("name")
    }

    @Test
    fun matchesTableColumn() {
        assertThat(p).matches("tab.name")
    }

    @Test
    fun matchesAllTableColumns() {
        assertThat(p).matches("tab.*")
    }

    @Test
    fun matchesTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.name")
    }

    @Test
    fun matchesAllTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.*")
    }

    @Test
    fun matchesLiterals() {
        assertThat(p).matches("'x'")
        assertThat(p).matches("1")
        assertThat(p).matches("null")
    }

    @Test
    fun matchesSequenceCurrentValue() {
        assertThat(p).matches("seq.currval")
    }

    @Test
    fun matchesSequenceNextValue() {
        assertThat(p).matches("seq.nextval")
    }

    @Test
    fun matchesColumnWithAlias() {
        assertThat(p).matches("null alias")
    }

    @Test
    fun matchesColumnWithAliasWithAsKeyword() {
        assertThat(p).matches("null as alias")
    }

    @Test
    fun matchesSubqueryAsColumn() {
        assertThat(p).matches("(select 1 from dual) col")
    }

    @Test
    fun matchesColumnWithConnectByRoot() {
        assertThat(p).matches("connect_by_root foo")
        assertThat(p).matches("connect_by_root foo alias")
    }


}
