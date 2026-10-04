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
package com.felipebz.zpa.lexer

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

class WrappedSourceChannelTest {

    private val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

    private fun resource(name: String) = File("src/test/resources/wrapped/$name.plb").readText()

    private fun tokens(source: String) = lexer.lex(source).dropLast(1)

    private fun payloadOf(source: String) =
        tokens(source).single { it.type == PlSqlTokenType.WRAPPED_SOURCE }.originalValue

    @Test
    fun turnsEveryGeneratedWrapperOutputIntoOneOpaqueToken() {
        mapOf(
            "proc" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.PROCEDURE),
            "func" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.FUNCTION),
            "pkgs" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.PACKAGE),
            "pkgb" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.PACKAGE, PlSqlKeyword.BODY),
            "types" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.TYPE),
            "typeb" to listOf(PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.TYPE, PlSqlKeyword.BODY),
        ).forEach { (name, header) ->
            val source = resource(name)
            val tokens = tokens(source)
            assertThat(tokens.map { it.type }).describedAs(name).containsExactly(
                *header.toTypedArray(), GenericTokenType.IDENTIFIER, PlSqlKeyword.WRAPPED,
                PlSqlTokenType.WRAPPED_SOURCE, PlSqlPunctuator.DIVISION)
            val payload = tokens.single { it.type == PlSqlTokenType.WRAPPED_SOURCE }
            assertThat(payload.originalValue).startsWith("a000000\n").endsWith(source.trimEnd().removeSuffix("/").trimEnd().takeLast(20))
            assertThat(payload.originalValue).contains("+").contains("abcd")
            assertThat(payload.line).isEqualTo(2)
            assertThat(tokens.last().line).isEqualTo(source.trimEnd().lines().size)
        }
    }

    @Test
    fun keepsSlashesAndEncodedCharactersInsideThePayload() {
        val source = "create or replace procedure p wrapped\na000000\npayload/with/slashes\n/inside-a-line\nabc+\nabc=\nabc/def\n  x /y\n\n/\nselect 1 from dual;"
        val tokens = tokens(source)
        assertThat(payloadOf(source)).isEqualTo("a000000\npayload/with/slashes\n/inside-a-line\nabc+\nabc=\nabc/def\n  x /y")
        assertThat(tokens.map { it.type }).containsExactly(
            PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE, PlSqlKeyword.PROCEDURE, GenericTokenType.IDENTIFIER,
            PlSqlKeyword.WRAPPED, PlSqlTokenType.WRAPPED_SOURCE, PlSqlPunctuator.DIVISION,
            PlSqlKeyword.SELECT, PlSqlTokenType.INTEGER_LITERAL, PlSqlKeyword.FROM, GenericTokenType.IDENTIFIER, PlSqlPunctuator.SEMICOLON)
    }

    @Test
    fun endsAtALineHoldingOnlyASlash() {
        listOf("/", "   /", "/   ", "   /   ", "\t/\t").forEach { terminator ->
            val source = "create procedure p wrapped\na000000\nabc/def\n$terminator\nselect 1 from dual;"
            val tokens = tokens(source)
            assertThat(payloadOf(source)).describedAs(terminator).isEqualTo("a000000\nabc/def")
            assertThat(tokens.map { it.type }.subList(4, 7)).describedAs(terminator)
                .containsExactly(PlSqlTokenType.WRAPPED_SOURCE, PlSqlPunctuator.DIVISION, PlSqlKeyword.SELECT)
        }
    }

    @Test
    fun endsAtTheEndOfInputWithoutASlash() {
        val source = "create procedure p wrapped\na000000\nabc/def\nxyz=\n"
        val tokens = tokens(source)
        assertThat(payloadOf(source)).isEqualTo("a000000\nabc/def\nxyz=")
        assertThat(tokens.last().type).isEqualTo(PlSqlTokenType.WRAPPED_SOURCE)
        assertThat(payloadOf("create procedure p wrapped\na000000\nabc/")).isEqualTo("a000000\nabc/")
    }

    @Test
    fun acceptsCrLfLineEndingsAndSameLinePayload() {
        val source = "create or replace function f wrapped\r\na000000\r\nabc/def\r\n  \r\n/\r\nselect 1 from dual;"
        assertThat(payloadOf(source)).isEqualTo("a000000\r\nabc/def")
        assertThat(tokens(source).map { it.type }).contains(PlSqlPunctuator.DIVISION, PlSqlKeyword.SELECT)
        assertThat(payloadOf("create procedure p wrapped a000000\nabc\n/")).isEqualTo("a000000\nabc")
    }

    @Test
    fun handlesSeveralUnitsWithSqlAroundThem() {
        val source = "select count(*) from employees\n/\n" + resource("proc") + "\nselect 2 from dual;\n" + resource("func") + "\nselect 3 from dual;"
        val tokens = tokens(source)
        assertThat(tokens.count { it.type == PlSqlTokenType.WRAPPED_SOURCE }).isEqualTo(2)
        assertThat(tokens.count { it.type == PlSqlKeyword.SELECT }).isEqualTo(3)
        assertThat(tokens.map { it.type }).doesNotContain(PlSqlPunctuator.PLUS, PlSqlPunctuator.EQUALS)
        assertThat(tokens.filter { it.type == PlSqlPunctuator.DIVISION }).hasSize(3)
    }

    @Test
    fun acceptsTheHeaderFormsWrapperKeeps() {
        listOf(
            "create procedure p wrapped\nabc\n/",
            "create or replace editionable procedure p wrapped\nabc\n/",
            "create or replace nonEditionable function s.f wrapped\nabc\n/",
            "create procedure if not exists p wrapped\nabc\n/",
            "create or replace procedure \"p\" sharing = metadata wrapped\nabc\n/",
            "create or replace procedure p sharing = none wrapped\nabc\n/",
            "create or replace package sharing_pkg wrapped\nabc\n/",
            "CREATE OR REPLACE PACKAGE BODY pkg WRAPPED\nabc\n/",
            "create type t wrapped\nabc\n/",
            "create or replace type body s.t wrapped\nabc\n/",
        ).forEach {
            assertThat(tokens(it).map { token -> token.type }).describedAs(it).contains(PlSqlTokenType.WRAPPED_SOURCE)
        }
    }

    @Test
    fun leavesOrdinaryUsesOfTheWordWrappedAlone() {
        listOf(
            "select wrapped from t;",
            "select 1 wrapped from t where wrapped = 1;",
            "create table wrapped (x number);",
            "create procedure wrapped is begin null; end;",
            "create type wrapped as object (a number);",
            "create view wrapped as select 1 x from dual;",
            "create procedure p (wrapped number) is begin null; end;",
            "create function f return wrapped is begin null; end;",
            "create procedure p authid definer wrapped\nabc\n/",
            "create procedure p (a number) wrapped\nabc\n/",
            "create procedure p is wrapped\nabc\n/",
            "create procedure p wrapped\n/",
            "create procedure p wrapped",
            "create procedure p wrapped;",
            "create index wrapped on t (x);",
            "wrapped\nabc\n/",
            "begin wrapped := 1; end;",
        ).forEach {
            assertThat(tokens(it).map { token -> token.type }).describedAs(it).doesNotContain(PlSqlTokenType.WRAPPED_SOURCE)
        }
        val identifier = tokens("select wrapped from t;")[1]
        assertThat(identifier.type).isEqualTo(PlSqlKeyword.WRAPPED)
        assertThat(identifier.originalValue).isEqualTo("wrapped")
    }

    @Test
    fun keepsLaterUnwrappedUnitsOrdinary() {
        val source = resource("proc") + "\ncreate or replace procedure q is begin null; end;\n/\n"
        val types = tokens(source).map { it.type }
        assertThat(types).containsSubsequence(
            PlSqlTokenType.WRAPPED_SOURCE, PlSqlPunctuator.DIVISION, PlSqlKeyword.CREATE, PlSqlKeyword.OR, PlSqlKeyword.REPLACE,
            PlSqlKeyword.PROCEDURE, GenericTokenType.IDENTIFIER, PlSqlKeyword.IS, PlSqlKeyword.BEGIN, PlSqlKeyword.NULL,
            PlSqlPunctuator.SEMICOLON, PlSqlKeyword.END, PlSqlPunctuator.SEMICOLON, PlSqlPunctuator.DIVISION)
    }
}
