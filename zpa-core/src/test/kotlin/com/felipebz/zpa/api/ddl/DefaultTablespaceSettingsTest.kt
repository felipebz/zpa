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
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class DefaultTablespaceSettingsTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("alter database $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("alter database $tail")
    }

    @Test
    fun matchesDefaultFileTypes() {
        matches(
            "set default bigfile tablespace",
            "set default smallfile tablespace",
            "set default bigfile tablespace;",
            "payable set default smallfile tablespace"
        )
        notMatches(
            "set default tablespace",
            "set default mediumfile tablespace",
            "set bigfile tablespace",
            "set default bigfile",
            "set default bigfile smallfile tablespace",
            "set default bigfile tablespace x",
            "default bigfile tablespace"
        )
    }

    @Test
    fun matchesDefaultTablespace() {
        matches(
            "default tablespace tbs_01",
            "default tablespace \"Tbs\";",
            "payable default tablespace tbs_01"
        )
        notMatches(
            "default tablespace",
            "default tablespace 'tbs'",
            "default tablespace a.b",
            "default tablespace t1, t2",
            "default tablespace t1 x",
            "default default tablespace t1",
            "default local tablespace t1"
        )
    }

    @Test
    fun matchesDefaultTemporaryTablespace() {
        matches(
            "default temporary tablespace tbs_05",
            "default temporary tablespace tbs_grp_01",
            "default local temporary tablespace tbs_05",
            "default local temporary tablespace \"tbs\";",
            "payable default temporary tablespace tbs_05"
        )
        notMatches(
            "default temporary tablespace",
            "default temporary tablespace 'tbs'",
            "default temporary tablespace a.b",
            "default temporary tablespace t1, t2",
            "default temporary tablespace group t1",
            "default temporary tablespace t1 x",
            "default temporary local tablespace t1",
            "default local local temporary tablespace t1",
            "default temporary t1",
            "default local temporary t1"
        )
    }

    @Test
    fun keepsOtherClausesSeparate() {
        notMatches(
            "default",
            "default edition = e1",
            "set standby nologging for data availability tablespace",
            "set compatibility to '23.0.0'"
        )
        matches(
            "set standby nologging for data availability",
            "datafile 'a.dbf' online",
            "add logfile 'a.log'",
            "default tablespace tablespace",
            "default temporary tablespace temporary"
        )
    }

    @Test
    fun exposesTheClauseAsItsOwnNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database default temporary tablespace t1; " +
            "alter database set default bigfile tablespace; " +
            "alter database add logfile 'a.log'; " +
            "create user u default tablespace users temporary tablespace temp;")
        val statements = tree.getDescendants(DdlGrammar.ALTER_DATABASE)
        assertThatAst(statements.map { it.getChildren(DdlGrammar.DEFAULT_TABLESPACE_SETTINGS).size }).containsExactly(1, 1, 0)
        assertThatAst(tree.getDescendants(DdlGrammar.DEFAULT_TABLESPACE_SETTINGS)).hasSize(2)
    }
}
