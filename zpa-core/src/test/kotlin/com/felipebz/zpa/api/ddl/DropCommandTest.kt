//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class DropCommandTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.DROP_COMMAND)
    }

    @Test
    fun matchesDropTable() {
        assertThat(p).matches("drop table x;")
        assertThat(p).matches("drop table x")
    }

    @Test
    fun exposesUnitNameForNamedObjects() {
        listOf(
            "drop table t", "drop index i", "drop view v", "drop sequence s", "drop procedure p",
            "drop function f", "drop trigger tg", "drop package pk", "drop package body pk",
            "drop type ty", "drop type body ty", "drop synonym sy", "drop public synonym sy",
            "drop materialized view mv", "drop cluster c"
        ).forEach {
            val node = p.parse("$it;")
            assertThatAst(node.getChildren(PlSqlGrammar.UNIT_NAME)).describedAs(it).hasSize(1)
        }
    }

    @Test
    fun exposesSchemaAndNameInUnitName() {
        val unitName = p.parse("drop table hr.employees;").getFirstChild(PlSqlGrammar.UNIT_NAME)
        assertThatAst(unitName.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { it.tokenOriginalValue })
            .containsExactly("hr", "employees")

        val unqualified = p.parse("drop table employees").getFirstChild(PlSqlGrammar.UNIT_NAME)
        assertThatAst(unqualified.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { it.tokenOriginalValue })
            .containsExactly("employees")
    }

    @Test
    fun skipsIfExistsAndKeepsTrailingOptions() {
        val node = p.parse("drop table if exists hr.t cascade constraints purge;")
        assertThatAst(node.getFirstChild(PlSqlGrammar.UNIT_NAME).tokens.map { it.originalValue })
            .containsExactly("hr", ".", "t")
        assertThatAst(node.tokens.map { it.originalValue })
            .containsExactly("drop", "table", "if", "exists", "hr", ".", "t", "cascade", "constraints", "purge", ";")
    }

    @Test
    fun keepsOtherDropsUnstructured() {
        listOf(
            "drop user u cascade", "drop role r", "drop directory d", "drop database link l",
            "drop public database link l", "drop materialized view log on t", "drop synonym",
            "drop table"
        ).forEach {
            assertThat(p).matches(it)
            assertThatAst(p.parse(it).getChildren(PlSqlGrammar.UNIT_NAME)).describedAs(it).isEmpty()
        }
    }
}
