/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.plsqlopen.api.ddl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

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
