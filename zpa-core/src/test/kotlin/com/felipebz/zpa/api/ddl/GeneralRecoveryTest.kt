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

class GeneralRecoveryTest : RuleTest() {

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
    fun matchesFullDatabaseRecovery() {
        matches(
            "recover database",
            "recover database;",
            "recover automatic database",
            "recover standby database",
            "recover automatic standby database",
            "recover from '/backup' database",
            "recover automatic from '/backup' database",
            "recover database until cancel",
            "recover database until time '2001-10-27:14:00:00'",
            "recover database until change 100",
            "recover database until consistent",
            "recover database using backup controlfile",
            "recover database until cancel using backup controlfile",
            "recover database using backup controlfile until cancel",
            "recover standby database until consistent using backup controlfile",
            "payable recover database",
            "\"payable\" recover automatic database"
        )
    }

    @Test
    fun matchesRecoveryWithoutTheDatabaseKeyword() {
        matches(
            "recover automatic until time '2001-10-27:14:00:00'",
            "recover until cancel",
            "recover automatic until change 5",
            "recover using backup controlfile",
            "recover automatic",
            "recover"
        )
    }

    @Test
    fun matchesPartialRecovery() {
        matches(
            "recover tablespace tbs_03",
            "recover tablespace tbs_03 parallel",
            "recover tablespace t1, t2",
            "recover automatic tablespace t1",
            "recover datafile 'a.dbf'",
            "recover datafile 17",
            "recover datafile 'a.dbf', 'b.dbf'",
            "recover datafile 17, 18",
            "recover datafile 'a.dbf', 17, 'c.dbf'",
            "recover datafile 17, 'a.dbf'",
            "recover from '/backup' datafile 17"
        )
    }

    @Test
    fun matchesLogfileRecovery() {
        matches(
            "recover logfile 'diskc:log3.log'",
            "recover automatic logfile 'a.log'",
            "recover from '/backup' logfile 'a.log'",
            "recover logfile 'a.log' test allow 1 corruption"
        )
    }

    @Test
    fun matchesRecoveryOptions() {
        matches(
            "recover database test",
            "recover database allow 1 corruption",
            "recover database parallel",
            "recover database parallel 4",
            "recover database noparallel",
            "recover database test allow 5 corruption parallel 2",
            "recover database parallel allow 1 corruption test",
            "recover database until cancel test",
            "recover database using backup controlfile parallel 2 test",
            "recover tablespace t1 test parallel 2",
            "recover datafile 17 allow 2 corruption noparallel",
            "recover database test test",
            "recover database allow 1 corruption allow 2 corruption"
        )
    }

    @Test
    fun matchesContinueAndCancel() {
        matches(
            "recover continue",
            "recover continue default",
            "recover cancel",
            "recover automatic continue",
            "recover from '/backup' continue"
        )
    }

    @Test
    fun rejectsMalformedRecovery() {
        notMatches(
            "recover from '/backup' automatic database",
            "recover automatic automatic database",
            "recover from backup database",
            "recover from database",
            "recover database standby",
            "recover standby",
            "recover standby until cancel",
            "recover standby tablespace t1",
            "recover standby datafile 17",
            "recover standby logfile 'a.log'",
            "recover database x",
            "recover x"
        )
    }

    @Test
    fun rejectsMalformedUntilAndUsing() {
        notMatches(
            "recover database until",
            "recover database until time",
            "recover database until time sysdate",
            "recover database until time 5",
            "recover database until change",
            "recover database until change 1 + 1",
            "recover database until change 'x'",
            "recover database until cancel time '2001-10-27'",
            "recover database until x",
            "recover database using",
            "recover database using backup",
            "recover database using controlfile",
            "recover database using backup controlfile x"
        )
    }

    @Test
    fun rejectsMalformedPartialAndLogfile() {
        notMatches(
            "recover tablespace",
            "recover tablespace t1,",
            "recover tablespace t1,, t2",
            "recover tablespace 5",
            "recover tablespace t1 tablespace t2",
            "recover tablespace t1 until cancel",
            "recover tablespace t1 using backup controlfile",
            "recover database tablespace t1",
            "recover datafile",
            "recover datafile a",
            "recover datafile 17,",
            "recover datafile 17 18",
            "recover datafile 17 database",
            "recover datafile 17 until cancel",
            "recover logfile",
            "recover logfile a",
            "recover logfile 'a.log', 'b.log'",
            "recover logfile 'a.log' database"
        )
    }

    @Test
    fun rejectsMalformedOptions() {
        notMatches(
            "recover database allow corruption",
            "recover database allow 1",
            "recover database allow 1 corruptions",
            "recover database allow x corruption",
            "recover database noparallel 4",
            "recover database parallel x",
            "recover database parallel 4 5",
            "recover database test 1",
            "recover continue test",
            "recover continue parallel",
            "recover continue database",
            "recover cancel test",
            "recover cancel default",
            "recover continue default default"
        )
    }

    @Test
    fun keepsManagedStandbyRecoveryAndOtherClausesSeparate() {
        notMatches(
            "recover managed standby database",
            "recover managed standby database cancel",
            "begin backup",
            "end backup",
            "open read only",
            "recover database managed"
        )
        matches(
            "datafile 'a.dbf' online",
            "add logfile 'a.log'",
            "enable lost write protection",
            "payable archivelog"
        )
    }

    @Test
    fun exposesGeneralRecoveryAsOwnNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter database recover automatic until time '2001-10-27:14:00:00'; " +
            "alter database payable recover tablespace t1, t2 parallel; " +
            "alter database datafile 'a.dbf' online; " +
            "create table recover (automatic number, test number, cancel number, controlfile number, corruption number);")
        val statements = tree.getDescendants(DdlGrammar.ALTER_DATABASE)
        assertThatAst(statements).hasSize(3)
        assertThatAst(statements.map { it.getChildren(DdlGrammar.GENERAL_RECOVERY).size }).containsExactly(1, 1, 0)
        assertThatAst(tree.getDescendants(DdlGrammar.GENERAL_RECOVERY)).hasSize(2)
    }
}
