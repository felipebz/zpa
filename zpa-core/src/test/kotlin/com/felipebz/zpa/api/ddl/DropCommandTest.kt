/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

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
