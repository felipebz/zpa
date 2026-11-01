//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DdlCommentTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.DDL_COMMENT)
    }

    @Test
    fun matchesCommentOnTableWithSemicolon() {
        assertThat(p).matches("comment on table tab is 'test';")
    }

    @Test
    fun matchesCommentOnTable() {
        assertThat(p).matches("comment on table tab is 'test'")
    }

    @Test
    fun matchesCommentOnTableWithSchema() {
        assertThat(p).matches("comment on table sch.tab is 'test'")
    }

    @Test
    fun matchesCommentOnColumn() {
        assertThat(p).matches("comment on column tab.col is 'test'")
    }

    @Test
    fun matchesCommentOnColumnWithSchema() {
        assertThat(p).matches("comment on column sch.tab.col is 'test'")
    }

    @Test
    fun matchesCommentOnOperator() {
        assertThat(p).matches("comment on operator foo is 'test'")
    }

    @Test
    fun matchesCommentOnOperatorWithSchema() {
        assertThat(p).matches("comment on operator sch.foo is 'test'")
    }

    @Test
    fun matchesCommentOnIndextype() {
        assertThat(p).matches("comment on indextype foo is 'test'")
    }

    @Test
    fun matchesCommentOnIndextypeWithSchema() {
        assertThat(p).matches("comment on indextype sch.foo is 'test'")
    }

    @Test
    fun matchesCommentOnMaterializedView() {
        assertThat(p).matches("comment on materialized view foo is 'test'")
    }

    @Test
    fun matchesCommentOnMaterializedViewWithSchema() {
        assertThat(p).matches("comment on materialized view sch.foo is 'test'")
    }

    @Test
    fun matchesCommentOnMiningModelView() {
        assertThat(p).matches("comment on materialized view foo is 'test'")
    }

    @Test
    fun matchesCommentOnMiningModelWithSchema() {
        assertThat(p).matches("comment on materialized view sch.foo is 'test'")
    }

}
