//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class SqlPlusCommandTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(SqlPlusGrammar.SQLPLUS_COMMAND)
    }

    @Test
    fun matchesEveryCurrentCommandSpelling() {
        COMMANDS.forEach { command ->
            assertThat(p).describedAs(command).matches(inputFor(command))
        }
    }

    @Test
    fun acceptsCaseVariantsAndBothLineEndpoints() {
        assertThat(p).matches("rUn script.sql")
        assertThat(p).matches("sEt pagesize 0\n")
        assertThat(p).matches("@script.sql\n")
        assertThat(p).matches("? tables")
    }

    @Test
    fun matchesHostAliasesAndLineEndpoints() {
        assertThat(p).matches("! rm params.sql.tmp")
        assertThat(p).matches("$ del params.sql.tmp")
        assertThat(p).matches("! echo foo\n")
        assertThat(p).matches("$ dir\n")
    }

    @Test
    fun preservesOrdinaryDollarIdentifiersAndNotEqualsExpressions() {
        setRootRule(PlSqlGrammar.EXPRESSION)
        assertThat(p).matches("foo\$bar != foo\$bar")
    }

    @Test
    fun rejectsIdentifierPrefixesOfCommands() {
        listOf("RUNNER", "STARTED", "CONNECTED", "APPENDING", "foo\$bar").forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun preservesCommandNodeAndTokenHierarchy() {
        val node = p.parse("rUn script.sql\n")

        assertThatAst(node.type).isEqualTo(SqlPlusGrammar.SQLPLUS_COMMAND)
        assertThatAst(node.tokenOriginalValue).isEqualTo("rUn")
        assertThatAst(node.children).hasSize(4)
        assertThatAst(node.children.map(AstNode::tokenOriginalValue))
            .containsExactly("rUn", "script", ".", "sql")
    }

    private fun inputFor(command: String): String = when (command) {
        "@" -> "@script.sql"
        "?" -> "? tables"
        else -> "$command argument"
    }

    private companion object {
        val COMMANDS = listOf(
            "@",
            "A", "APPEND",
            "ACC", "ACCEPT",
            "ARCHIVE",
            "ATTR", "ATTRIBUTE",
            "BRE", "BREAK",
            "BTI", "BTITLE",
            "C", "CHANGE",
            "CL", "CLEAR",
            "COL", "COLUMN",
            "COMP", "COMPUTE",
            "CONN", "CONNECT",
            "COPY",
            "DEF", "DEFINE",
            "DEL",
            "DESC", "DESCRIBE",
            "DISC", "DISCONNECT",
            "ED", "EDIT",
            "EXEC", "EXECUTE",
            "EXIT", "QUIT",
            "GET",
            "HELP", "?",
            "HO", "HOST", "!", "$",
            "I", "INPUT",
            "L", "LIST",
            "PASSW", "PASSWORD",
            "PAU", "PAUSE",
            "PRINT",
            "PRO", "PROMPT",
            "RECOVER",
            "REM", "REMARK",
            "REPF", "REPFOOTER",
            "REPH", "REPHEADER",
            "R", "RUN",
            "SAV", "SAVE",
            "SET",
            "SHO", "SHOW",
            "SHUTDOWN",
            "SPO", "SPOOL",
            "STA", "START", "STARTUP",
            "STORE",
            "TIMI", "TIMING",
            "TTI", "TTITLE",
            "UNDEF", "UNDEFINE",
            "VAR", "VARIABLE",
            "WHENEVER",
            "XQUERY"
        )
    }
}
