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

class GlobalNameAndBlockTrackingTest : RuleTest() {

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
    fun matchesRenameGlobalName() {
        matches(
            "rename global_name to demo", "rename global_name to demo.world", "rename global_name to demo.world.example.com",
            "rename global_name to demo.a.b.c.d.e.f", "rename global_name to \"demo\"",
            "rename global_name to \"demo\".\"world\"", "rename global_name to demo.\"world\".com",
            "rename global_name to demo_1.w\$rld#.c1", "rename global_name to demo.world;", "RENAME GLOBAL_NAME TO DEMO",
            "d1 rename global_name to demo"
        )
    }

    @Test
    fun rejectsMalformedRenameGlobalName() {
        notMatches(
            "rename global_name to 'demo'", "rename global_name to demo.'world'", "rename global_name to demo.",
            "rename global_name to .demo", "rename global_name to demo..world", "rename global_name to 1demo",
            "rename global_name to demo.1world", "rename global_name to demo.world-x", "rename global_name to demo@world",
            "rename global_name to select", "rename global_name to demo.select", "rename global_name to a b",
            "rename global_name to a, b", "rename global_name to", "rename global_name demo", "rename global_name",
            "rename global to demo", "rename globalname to demo", "rename name to demo", "rename to demo",
            "rename global_name to demo rename global_name to x"
        )
    }

    @Test
    fun matchesEnableBlockChangeTracking() {
        matches(
            "enable block change tracking", "enable block change tracking;",
            "enable block change tracking using file 'f'", "enable block change tracking using file 'f' reuse",
            "enable block change tracking using file '+dg/f' reuse;", "enable block change tracking using file q'[f]'",
            "enable block change tracking using file N'f'", "ENABLE BLOCK CHANGE TRACKING USING FILE 'F' REUSE",
            "d1 enable block change tracking"
        )
    }

    @Test
    fun matchesDisableBlockChangeTracking() {
        matches("disable block change tracking", "disable block change tracking;", "d1 disable block change tracking")
    }

    @Test
    fun rejectsMalformedBlockChangeTracking() {
        notMatches(
            "enable block change tracking reuse", "enable block change tracking using 'f'",
            "enable block change tracking using file", "enable block change tracking using file f",
            "enable block change tracking using file \"f\"", "enable block change tracking using file 'f' reuse reuse",
            "enable block change tracking using file 'f', 'g'", "enable block change tracking file 'f'",
            "enable block change tracking using file 'f' using file 'g'", "enable block change tracking using file 'f' noreuse",
            "enable block tracking", "enable change tracking", "enable block change", "enable tracking",
            "enable block change tracking x", "enable change block tracking",
            "disable block change tracking reuse", "disable block change tracking x", "disable block change",
            "disable tracking", "disable block change tracking using file 'f'",
            "enable block change tracking disable block change tracking",
            "enable block change tracking enable block change tracking",
            "enable block change tracking rename global_name to x",
            "enable block change tracking, disable block change tracking"
        )
    }

    @Test
    fun keepsOtherEnableDisableAndRenameClausesSeparate() {
        matches("enable lost write protection", "rename file 'a.dbf' to 'b.dbf'", "datafile 'a.dbf' online")
        notMatches("enable thread 2", "enable", "disable", "rename", "flashback on")
    }

    @Test
    fun buildsDedicatedNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter database rename global_name to demo.world.example.com;\n" +
                "alter database enable block change tracking using file 'f' reuse;\n" +
                "alter database disable block change tracking;\n" +
                "alter database open read only;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATABASE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.RENAME_GLOBAL_NAME_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.BLOCK_CHANGE_TRACKING_CLAUSE)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.STARTUP_CLAUSES)).hasSize(1)
        val global = tree.getFirstDescendant(DdlGrammar.RENAME_GLOBAL_NAME_CLAUSE)!!
        assertThatAst(global.children.map { it.tokenOriginalValue }).containsExactly(
            "rename", "global_name", "to", "demo", ".", "world", ".", "example", ".", "com")
        val tracking = tree.getDescendants(DdlGrammar.BLOCK_CHANGE_TRACKING_CLAUSE)
        assertThatAst(tracking.map { it.firstChild.tokenOriginalValue.lowercase() }).containsExactly("enable", "disable")
        assertThatAst(tracking[0].children.map { it.tokenOriginalValue.lowercase() })
            .containsExactly("enable", "block", "change", "tracking", "using", "file", "'f'", "reuse")
    }
}
