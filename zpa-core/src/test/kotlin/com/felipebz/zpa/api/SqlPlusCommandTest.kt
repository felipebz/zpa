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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun rejectsIdentifierPrefixesOfCommands() {
        listOf("RUNNER", "STARTED", "CONNECTED", "APPENDING").forEach { source ->
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    @Test
    fun preservesCommandNodeAndTokenHierarchy() {
        val node = p.parse("rUn script.sql\n")

        assertThatAst(node.type).isEqualTo(SqlPlusGrammar.SQLPLUS_COMMAND)
        assertThatAst(node.fromIndex).isEqualTo(0)
        assertThatAst(node.toIndex).isEqualTo(4)
        assertThatAst(node.tokenOriginalValue).isEqualTo("rUn")
        assertThatAst(node.children).hasSize(4)
        assertThatAst(node.children.map(AstNode::tokenOriginalValue))
            .containsExactly("rUn", "script", ".", "sql")
        assertThatAst(node.children.map { it.fromIndex to it.toIndex })
            .containsExactly(0 to 1, 1 to 2, 2 to 3, 3 to 4)
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
            "HO", "HOST",
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
