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

class AlterDatabaseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).matches("alter database $tail")
        }
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).notMatches("alter database $tail")
        }
    }

    @Test
    fun matchesOptionalDatabaseName() {
        matches(
            "add logfile ('a.log') size 10m;",
            "payable add logfile ('a.log') size 10m",
            "\"payable\" add logfile ('a.log')",
            "logfile add logfile ('a.log')",
            "payable archivelog",
            "payable rename file 'a' to 'b'",
            "payable clear logfile group 1",
            "payable switch all logfiles to blocksize 4096",
            "payable add supplemental log data",
            "payable datafile 'a.dbf' online",
            "\"payable\" tempfile 1 offline",
            "payable create datafile 1 as new",
            "payable move datafile 1 to '+DATA'",
            "\"datafile\" datafile 1 online"
        )
    }

    @Test
    fun rejectsMalformedDatabaseNames() {
        notMatches(
            "payable",
            "s.payable add logfile ('a.log')",
            "a b add logfile ('a.log')",
            "'payable' add logfile ('a.log')",
            "add add logfile ('a.log')",
            "no no force logging",
            "drop drop logfile group 1",
            "clear clear logfile group 1",
            "force force logging",
            "archivelog archivelog",
            "switch switch all logfiles to blocksize 512",
            "link add logfile ('a.log')",
            "datafile datafile 'a.dbf' online",
            "tempfile tempfile 1 offline",
            "create create datafile 1 as new",
            "move move datafile 1 to '+DATA'"
        )
    }

    @Test
    fun matchesAddLogfileGroups() {
        matches(
            "add logfile",
            "add logfile group 5",
            "add logfile size 10m",
            "add logfile 'a.log' size 10m",
            "add logfile group 3 ('a.log', 'b.log') size 50k",
            "add logfile thread 5 group 4 ('a.log', 'b.log')",
            "add logfile instance 'inst1' group 4 ('a.log')",
            "add logfile group 5 ('a.log') size 10m, group 6 ('b.log') size 10m",
            "add logfile ('a.log') size 10m, ('b.log') size 10m",
            "add logfile group 5 ('a.log') size 100m blocksize 4096 reuse",
            "add logfile ('a.log') size 10 k",
            "add logfile ('a.log') blocksize 4k",
            "add logfile ('a.log') reuse",
            "add standby logfile group 7 ('a.log') size 10m",
            "add standby logfile thread 1 ('a.log')"
        )
    }

    @Test
    fun rejectsMalformedAddLogfileGroups() {
        notMatches(
            "add logfile thread '5' group 4 ('a.log')",
            "add logfile instance inst1 ('a.log')",
            "add logfile instance 'i' thread 5 ('a.log')",
            "add logfile thread 1 group 5 ('a.log'), thread 2 group 6 ('b.log')",
            "add logfile group 4 thread 5 ('a.log')",
            "add logfile ('a.log') reuse size 10m",
            "add logfile ('a.log') blocksize 4096 size 10m",
            "add logfile ('a.log') size 10m size 10m",
            "add logfile ('a.log') reuse reuse",
            "add logfile ()",
            "add logfile ('a.log',)",
            "add logfile (a)",
            "add logfile (1)",
            "add logfile group 1+1 ('a.log')",
            "add logfile group g ('a.log')",
            "add logfile ('a.log'),",
            "add logfile ('a.log'), , ('b.log')",
            "add logfile ('a.log') size 10m autoextend on",
            "add logfile ('a.log') size 10m banana"
        )
    }

    @Test
    fun matchesAddLogfileMembers() {
        matches(
            "add logfile member 'c.log' to group 3",
            "add logfile member 'c.log' reuse to group 3",
            "add logfile member 'c.log', 'd.log' to group 3",
            "add logfile member 'c.log', 'd.log' reuse to group 3",
            "add logfile member 'c.log' to 'a.log'",
            "add logfile member 'c.log' to ('a.log', 'b.log')",
            "add standby logfile member 'c.log' to group 3"
        )
    }

    @Test
    fun rejectsMalformedAddLogfileMembers() {
        notMatches(
            "add logfile member 'c.log' reuse, 'd.log' to group 3",
            "add logfile member 'c.log' to group 3, group 4",
            "add logfile member 'c.log' to 'a.log', 'b.log'",
            "add logfile member ('c.log') to group 3",
            "add logfile member 'c.log' group 3",
            "add logfile member 'c.log' to",
            "add logfile member to group 3",
            "add logfile thread 1 member 'c.log' to group 3",
            "add logfile member 'c.log' size 10m to group 3",
            "add logfile member c to group 3",
            "add logfile member 'c.log', to group 3",
            "add logfile member 'c.log' to thread 1 group 3",
            "add logfile member 'c.log' to ()"
        )
    }

    @Test
    fun matchesDropLogfile() {
        matches(
            "drop logfile group 3",
            "drop logfile group 3, group 4",
            "drop logfile 'a.log'",
            "drop logfile ('a.log', 'b.log')",
            "drop logfile group 3, 'a.log', ('b.log')",
            "drop logfile member 'b.log'",
            "drop logfile member 'a.log', 'b.log'",
            "drop standby logfile group 3",
            "drop standby logfile member 'a'"
        )
    }

    @Test
    fun rejectsMalformedDropLogfile() {
        notMatches(
            "drop logfile",
            "drop logfile member ('a.log')",
            "drop logfile member group 3",
            "drop logfile thread 1 group 3",
            "drop logfile group 3 group 4",
            "drop logfile group 3,",
            "drop logfile member 'a' reuse",
            "drop logfile group g"
        )
    }

    @Test
    fun matchesClearLogfile() {
        matches(
            "clear logfile 'a.log'",
            "clear logfile group 3",
            "clear logfile ('a.log', 'b.log')",
            "clear logfile group 3, group 4",
            "clear unarchived logfile group 3",
            "clear unarchived logfile group 3 unrecoverable datafile",
            "clear logfile group 3 unrecoverable datafile"
        )
    }

    @Test
    fun rejectsMalformedClearLogfile() {
        notMatches(
            "clear logfile",
            "clear logfile member 'a'",
            "clear logfile group 3 unrecoverable",
            "clear logfile unarchived group 3",
            "clear standby logfile group 3",
            "clear logfile thread 1 group 3",
            "clear logfile group 3 reuse"
        )
    }

    @Test
    fun matchesRenameFile() {
        matches(
            "rename file 'a' to 'b'",
            "rename file 'a', 'b' to 'c', 'd'"
        )
        notMatches(
            "rename file ('a') to ('b')",
            "rename file a to b",
            "rename file 'a' 'b'",
            "rename 'a' to 'b'",
            "rename file 'a' to",
            "rename file 'a', to 'b'"
        )
    }

    @Test
    fun matchesArchivingLoggingAndSwitch() {
        matches(
            "archivelog",
            "archivelog manual",
            "noarchivelog",
            "force logging",
            "no force logging",
            "set standby nologging for data availability",
            "set standby nologging for load performance",
            "switch all logfiles to blocksize 4096",
            "switch all logfiles to blocksize 4k"
        )
        notMatches(
            "noarchivelog manual",
            "force logging force logging",
            "set standby nologging for load",
            "set standby nologging",
            "switch logfiles to blocksize 512",
            "switch logfile"
        )
    }

    @Test
    fun matchesSupplementalLogging() {
        matches(
            "add supplemental log data",
            "drop supplemental log data",
            "add supplemental log data (primary key, unique) columns",
            "drop supplemental log data (all, foreign key) columns",
            "add supplemental log data for procedural replication",
            "add supplemental log data subset database replication"
        )
        notMatches(
            "add supplemental log data () columns",
            "add supplemental log data (all)",
            "add supplemental log group g (a)",
            "add supplemental log"
        )
    }

    @Test
    fun keepsOtherAlterDatabaseClausesUnsupported() {
        notMatches(
            "open read only",
            "rename global_name to demo.world.example.com",
            "enable block change tracking",
            "flashback on"
        )
    }

    @Test
    fun reusesLogfileRenameWithinDatabaseFileFamily() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database rename file 'a.dbf' to 'b.dbf'; " +
            "alter database add logfile 'a.log';")
        val rename = tree.getDescendants(DdlGrammar.DATABASE_FILE_CLAUSES).single()
        assertThatAst(rename.getChildren(DdlGrammar.LOGFILE_CLAUSES)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.LOGFILE_CLAUSES)).hasSize(2)
        assertThatAst(rename.getDescendants(DdlGrammar.REDO_LOG_FILE_SPEC)).isEmpty()
    }

    @Test
    fun separatesStatementClauseAndFileNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database payable add logfile group 3 ('a.log', 'b.log') size 50k reuse; " +
            "alter database drop logfile member 'b.log'; " +
            "alter database clear unarchived logfile group 3, 'c.log'; " +
            "alter database link lnk connect to u identified by p; " +
            "create table logfile (file number, thread number, clear number, switch number);")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATABASE)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.LOGFILE_CLAUSES)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.ADD_LOGFILE_CLAUSES)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.DROP_LOGFILE_CLAUSES)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.REDO_LOG_FILE_SPEC)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.LOGFILE_DESCRIPTOR)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATABASE_LINK)).hasSize(1)
        assertThat(p).notMatches("alter database open read only;")
    }
}
