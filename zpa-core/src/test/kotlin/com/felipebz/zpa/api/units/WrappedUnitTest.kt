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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class WrappedUnitTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private fun resource(name: String) = File("src/test/resources/wrapped/$name.plb").readText()

    @Test
    fun matchesTheExactOracleDocumentationFixture() {
        val payload1 = "abcd\n".repeat(15)
        val source = """
            SELECT COUNT(*) FROM EMPLOYEES
            /
            CREATE OR REPLACE PROCEDURE wraptest wrapped 
            a000000
            1
            ${payload1}7
            129 138
            qf4HggDBeNMPlWAsPn6pGf+2LGwwg+nwJK5qZ3SVWE4+GayDZaL1bF7RwYm2/zr1qjZY3FrN
            48M1bKc/MG5aY9YB+DrtT4SJN370Rpq7ck5D0sc1D5sKAwTyX13HYvRmjwkdXa0vEZ4q/mCU
            +7fE1Tv29fwc+aZq3S7O
            /
            CREATE OR REPLACE FUNCTION fibonacci wrapped 
            a000000
            1
            ${payload1}8
            150 ff
            BFDvTL9OR04SJbx+qOy5H/h8IcwwgxDcAJnWZ3TNz51mjAmegdQcpNJfq8hUuQtv1Y5xg7Wd
            SGJsz7M+bnhnp+xP4ww+SIlxx5LhDtnyPw==
            /
            """.trimIndent()
        assertThat(p).matches(source)
        val tree = p.parse(source)
        assertThatAst(tree.getDescendants(PlSqlGrammar.CREATE_PROCEDURE)).hasSize(1)
        assertThatAst(tree.getDescendants(PlSqlGrammar.CREATE_FUNCTION)).hasSize(1)
        assertThatAst(tree.getDescendants(PlSqlTokenType.WRAPPED_SOURCE)).hasSize(2)
    }

    @Test
    fun matchesEveryUnitKindWrapperGenerates() {
        listOf(
            "proc" to PlSqlGrammar.CREATE_PROCEDURE,
            "func" to PlSqlGrammar.CREATE_FUNCTION,
            "pkgs" to PlSqlGrammar.CREATE_PACKAGE,
            "pkgb" to PlSqlGrammar.CREATE_PACKAGE_BODY,
            "types" to PlSqlGrammar.CREATE_TYPE,
            "typeb" to PlSqlGrammar.CREATE_TYPE_BODY,
        ).forEach { (name, type) ->
            val source = resource(name)
            assertThat(p).describedAs(name).matches(source)
            assertThat(p).describedAs("$name at end of input").matches(source.trimEnd().removeSuffix("/"))
            val unit = p.parse(source).getFirstDescendant(type)
            assertThatAst(unit.children.map { it.type }).describedAs(name).endsWith(
                PlSqlGrammar.UNIT_NAME, PlSqlKeyword.WRAPPED, PlSqlTokenType.WRAPPED_SOURCE)
            assertThatAst(unit.getDescendants(PlSqlGrammar.WRAPPED_SOURCE_CLAUSE)).isEmpty()
            assertThatAst(unit.getDescendants(
                PlSqlGrammar.PARAMETER_DECLARATIONS, PlSqlGrammar.STATEMENTS_SECTION, PlSqlGrammar.DECLARE_SECTION,
                PlSqlGrammar.DATATYPE, PlSqlGrammar.STATEMENTS)).describedAs(name).isEmpty()
        }
    }

    @Test
    fun keepsTheHeaderVisibleInTheUnitNode() {
        val source = "create or replace editionable procedure s.p wrapped\nabc\n/"
        val unit = p.parse(source).getFirstDescendant(PlSqlGrammar.CREATE_PROCEDURE)
        assertThatAst(unit.getFirstChild(PlSqlGrammar.UNIT_NAME).tokens.map { it.originalValue }).containsExactly("s", ".", "p")
        assertThatAst(unit.hasDirectChildren(PlSqlKeyword.EDITIONABLE)).isTrue()
        assertThatAst(unit.getFirstChild(PlSqlTokenType.WRAPPED_SOURCE).tokenOriginalValue).isEqualTo("abc")
    }

    @Test
    fun matchesTheHeaderFormsWrapperKeeps() {
        listOf(
            "create procedure p wrapped\nabc\n/",
            "create or replace nonEditionable function s.f wrapped\nabc\n/",
            "create procedure if not exists p wrapped\nabc\n/",
            "create or replace procedure \"p\" sharing = metadata wrapped\nabc\n/",
            "create or replace function f sharing = none wrapped\nabc\n/",
            "create or replace package pkg sharing = metadata wrapped\nabc\n/",
            "create or replace package body pkg wrapped\nabc\n/",
            "create or replace type t sharing = metadata wrapped\nabc\n/",
            "create or replace type body t wrapped\nabc\n/",
            "select 1 from dual;\ncreate procedure p wrapped\nabc/def\n/\nselect 2 from dual;\ncreate type t wrapped\nxyz\n/",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun doesNotParseClausesAroundTheMarker() {
        listOf(
            "create procedure p (a number) wrapped\nabc\n/",
            "create procedure p authid definer wrapped\nabc\n/",
            "create function f return number wrapped\nabc\n/",
            "create procedure p wrapped\n/",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsOrdinaryUnitsAndNamesUnchanged() {
        listOf(
            "create or replace procedure p (a number) authid definer is begin null; end;\n/",
            "create or replace function f return number is begin return 1; end;\n/",
            "create or replace package pkg is end pkg;\n/",
            "create or replace type t as object (a number);\n/",
            "select wrapped from t;",
            "create table wrapped (x number);",
            "create procedure wrapped is begin null; end;\n/",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }
}
