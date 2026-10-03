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

class StartupClausesTest : RuleTest() {

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
    fun matchesMount() {
        matches(
            "mount", "mount;", "mount standby database", "mount clone database", "MOUNT STANDBY DATABASE;",
            "d1 mount", "d1 mount clone database", "\"D1\" mount"
        )
    }

    @Test
    fun matchesEveryOpenCombination() {
        val readModes = listOf("", "read write")
        val logs = listOf("", "resetlogs", "noresetlogs")
        val upgrades = listOf("", "upgrade", "downgrade")
        for (r in readModes) for (l in logs) for (u in upgrades) {
            matches(("open $r $l $u").replace(Regex("\\s+"), " ").trim(), "d1 open $r $l $u;".replace(Regex("\\s+"), " "))
        }
        matches("open read only", "open read only;", "d1 open read only", "OPEN READ ONLY")
    }

    @Test
    fun rejectsMalformedMount() {
        notMatches(
            "mount database", "mount standby", "mount clone", "mount standby clone database",
            "mount standby database x", "mount nomount", "mount open", "mount read only", "mount resetlogs"
        )
    }

    @Test
    fun rejectsMalformedOpen() {
        notMatches(
            "open read", "open write", "open only", "open read write only", "open write read",
            "open resetlogs noresetlogs", "open resetlogs resetlogs", "open upgrade downgrade", "open upgrade upgrade",
            "open upgrade resetlogs", "open downgrade noresetlogs", "open resetlogs read write",
            "open read only resetlogs", "open read only noresetlogs", "open read only upgrade",
            "open read only downgrade", "open read only write", "open read only read write",
            "open read write read write", "open read write resetlogs x", "open read only x",
            "open open", "open mount", "open force", "a.b open", "d1 d2 open", "open read only mount"
        )
    }

    @Test
    fun keepsOtherAlterDatabaseClausesSeparate() {
        notMatches("rename global_name to demo.world.example.com", "enable block change tracking", "flashback on")
        notMatches("open add logfile group 3 ('a.log') size 1m")
    }

    @Test
    fun buildsStartupClausesNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter database open read write resetlogs upgrade;\nalter database mount standby database;\n" +
                "alter database open read only;\nalter database add logfile group 3 ('a.log') size 1m;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATABASE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.STARTUP_CLAUSES)).hasSize(3)
        val first = tree.getFirstDescendant(DdlGrammar.ALTER_DATABASE)!!
        assertThatAst(first.getChildren(DdlGrammar.STARTUP_CLAUSES)).hasSize(1)
        assertThatAst(first.getChildren(DdlGrammar.LOGFILE_CLAUSES)).isEmpty()
    }
}
