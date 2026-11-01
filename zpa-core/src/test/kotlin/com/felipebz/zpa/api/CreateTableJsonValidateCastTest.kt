//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CreateTableJsonValidateCastTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    @Test
    fun matchesColumnValidateForms() {
        assertThat(p).matches("create table t (j json validate '{}')")
        assertThat(p).matches("create table t (j json validate using '{}')")
        assertThat(p).matches("create table t (j json validate cast '{\"type\":\"object\"}')")
        assertThat(p).matches("create table t (j json validate cast using '{}')")
        assertThat(p).matches("create table t (j json validate cast q'[{}]')")
        assertThat(p).matches("create table t (a json validate cast using '{}', b json validate using '{}')")
    }

    @Test
    fun matchesColumnValidatePlacement() {
        assertThat(p).matches("create table t (j json sort validate cast '{}')")
        assertThat(p).matches("create table t (j json sort default null validate cast using '{}')")
        assertThat(p).matches("create table t (j json default null validate '{}')")
        assertThat(p).matches("create table t (j json default on null null validate '{}')")
        assertThat(p).matches("create table t (j json validate cast '{}' not null)")
        assertThat(p).matches("create table t (j json(object) validate cast using '{}' not null)")
        assertThat(p).matches("create table t (j json validate cast '{}' constraint c1 check (j is json), k number)")
    }

    @Test
    fun matchesValidateAfterNonJsonDatatypeWhichOracleRejectsOnlyAtRuntime() {
        assertThat(p).matches("create table t (n number validate '{}')")
        assertThat(p).matches("create table t (v varchar2(20) validate cast using '{}')")
        assertThat(p).matches("create table t (u my_type validate '{}')")
    }

    @Test
    fun rejectsColumnValidateAfterItsPosition() {
        assertThat(p).notMatches("create table t (j json validate cast '{}' sort)")
        assertThat(p).notMatches("create table t (j json validate '{}' sort)")
        assertThat(p).notMatches("create table t (j json default null sort validate '{}')")
        assertThat(p).notMatches("create table t (j json validate cast '{}' default null)")
        assertThat(p).notMatches("create table t (j json validate cast '{}' visible)")
        assertThat(p).notMatches("create table t (j json cast validate '{}')")
        assertThat(p).notMatches("create table t (j json validate '{}' cast)")
        assertThat(p).notMatches("create table t (j json validate cast '{}' validate cast '{}' default null)")
    }

    @Test
    fun buildsColumnDefinitionTokens() {
        val tree = p.parse("create table t (j json validate cast using '{}' not null)")
        val column = tree.getFirstDescendant(DdlGrammar.TABLE_COLUMN_DEFINITION)
        assertThatAst(column.children.map { it.tokenOriginalValue.lowercase() })
            .containsSubsequence("j", "json", "validate", "cast", "using", "'{}'", "not")
    }
}
