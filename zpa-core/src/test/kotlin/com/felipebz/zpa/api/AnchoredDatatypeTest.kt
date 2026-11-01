//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnchoredDatatypeTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ANCHORED_DATATYPE)
    }

    @Test
    fun matchesSimpleTypeAttribute() {
        assertThat(p).matches("name%type")
    }

    @Test
    fun matchesTableColumn() {
        assertThat(p).matches("tab.name%type")
    }

    @Test
    fun matchesTableColumnWithExplicitSchema() {
        assertThat(p).matches("schema.tab.name%type")
    }

    @Test
    fun matchesSimpleRowtypeAttribute() {
        assertThat(p).matches("name%rowtype")
    }

    @Test
    fun matchesTypeAsColumnName() {
        assertThat(p).matches("tab.type%type")
    }

}
