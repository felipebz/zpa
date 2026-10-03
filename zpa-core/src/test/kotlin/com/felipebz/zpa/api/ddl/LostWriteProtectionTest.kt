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

class LostWriteProtectionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private fun matches(prefix: String, vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs("$prefix $tail").matches("$prefix $tail")
        }
    }

    private fun notMatches(prefix: String, vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs("$prefix $tail").notMatches("$prefix $tail")
        }
    }

    private val database = "alter database"
    private val pdb = "alter pluggable database"

    @Test
    fun matchesDatabaseAndPdbLevelStates() {
        for (prefix in listOf(database, pdb)) {
            matches(
                prefix,
                "enable lost write protection",
                "disable lost write protection",
                "enable lost write protection;",
                "disable lost write protection;",
                "payable enable lost write protection",
                "\"payable\" disable lost write protection"
            )
        }
        matches(pdb, "enable enable lost write protection", "disable disable lost write protection", "enable disable lost write protection")
        notMatches(database, "enable enable lost write protection", "disable disable lost write protection", "enable disable lost write protection")
    }

    @Test
    fun rejectsUnsupportedDatabaseLevelActionsAndMalformedClauses() {
        for (prefix in listOf(database, pdb)) {
            notMatches(
                prefix,
                "remove lost write protection",
                "suspend lost write protection",
                "lost write protection",
                "enable lost write",
                "enable lost",
                "enable write protection",
                "enable remove lost write protection",
                "enable lost write protection lost write protection",
                "enable lost write protection enable lost write protection",
                "enable lost write protection online",
                "enable lost write protection; enable",
                "enable lost write protection, disable lost write protection",
                "payable payable enable lost write protection",
                "s.payable enable lost write protection",
                "'payable' enable lost write protection"
            )
        }
    }

    @Test
    fun matchesEveryDatafileAction() {
        for (prefix in listOf(database, pdb)) {
            for (action in listOf("enable", "remove", "suspend")) {
                matches(
                    prefix,
                    "datafile 'a.dbf' $action lost write protection",
                    "datafile 7 $action lost write protection",
                    "datafile '+DATA/db/datafile/users.256.123' $action lost write protection",
                    "datafile 'a.dbf', 'b.dbf' $action lost write protection",
                    "datafile 7, 8 $action lost write protection;",
                    "datafile 'a.dbf', 8, 'c.dbf' $action lost write protection",
                    "datafile 7.0 $action lost write protection",
                    "payable datafile 'a.dbf' $action lost write protection"
                )
            }
        }
    }

    @Test
    fun rejectsMalformedDatafileForms() {
        for (prefix in listOf(database, pdb)) {
            notMatches(
                prefix,
                "datafile 'a.dbf' disable lost write protection",
                "datafile 'a.dbf' lost write protection",
                "datafile 'a.dbf' enable",
                "datafile 'a.dbf' enable lost",
                "datafile 'a.dbf' enable lost write",
                "datafile 'a.dbf' enable write protection",
                "datafile 'a.dbf' enable remove lost write protection",
                "datafile 'a.dbf' enable lost write protection lost write protection",
                "datafile 'a.dbf' enable lost write protection suspend lost write protection",
                "datafile 'a.dbf' enable lost write protection online",
                "datafile 'a.dbf' remove lost write protection resize 10m",
                "datafile 'a.dbf' remove lost write protection autoextend on",
                "datafile 'a.dbf' online enable lost write protection",
                "datafile 'a.dbf' offline remove lost write protection",
                "datafile 'a.dbf' resize 10m suspend lost write protection",
                "datafile , 'a.dbf' enable lost write protection",
                "datafile 'a.dbf', enable lost write protection",
                "datafile 'a.dbf', , 'b.dbf' enable lost write protection",
                "datafile 'a.dbf' 'b.dbf' enable lost write protection",
                "datafile enable lost write protection 'a.dbf'",
                "datafile 'a.dbf' enable lost write protection; x",
                "tempfile 'a.dbf' enable lost write protection"
            )
        }
        notMatches(
            database,
            "datafile enable lost write protection",
            "move datafile 1 enable lost write protection",
            "create datafile 1 enable lost write protection"
        )
    }

    @Test
    fun rejectsDatafileOperandsOracleRejectsAtParseTime() {
        for (prefix in listOf(database, pdb)) {
            notMatches(
                prefix,
                "datafile td_file.df enable lost write protection",
                "datafile tdfile remove lost write protection",
                "datafile a.b.c suspend lost write protection",
                "datafile \"td_file.df\" enable lost write protection",
                "datafile +DATA/x.dbf enable lost write protection",
                "datafile :file enable lost write protection",
                "datafile 1+1 enable lost write protection",
                "datafile -1 enable lost write protection",
                "datafile () enable lost write protection",
                "datafile ('a.dbf') enable lost write protection"
            )
        }
    }

    @Test
    fun keepsPdbTargetSingleAndSeparateFromStateChanges() {
        notMatches(
            pdb,
            "pdb1, pdb2 enable lost write protection",
            "all enable lost write protection",
            "all except pdb1 disable lost write protection",
            "(pdb1) enable lost write protection",
            "pdb1 pdb2 enable lost write protection",
            "pdb1 open enable lost write protection",
            "enable lost write protection open"
        )
        matches(
            pdb,
            "pdb1 enable lost write protection",
            "pdb1 datafile 'a.dbf' remove lost write protection",
            "open enable lost write protection",
            "close disable lost write protection",
            "pdb1 open read only",
            "all close immediate"
        )
    }

    @Test
    fun keepsOrdinaryDatafileOperationsAndOtherClausesUnaffected() {
        matches(
            database,
            "datafile 'a.dbf' online",
            "datafile 1, 'b.dbf' offline for drop",
            "datafile 1 resize 10m",
            "add logfile 'a.log'",
            "payable archivelog",
            "tempfile 1 offline"
        )
        notMatches(
            database,
            "enable thread 2",
            "enable"
        )
    }

    @Test
    fun separatesSharedLostWriteNodeUnderBothStatements() {
        val tree = p.parse("alter database payable enable lost write protection; " +
            "alter database datafile 'a.dbf', 2 remove lost write protection; " +
            "alter database datafile 'a.dbf' online; " +
            "alter pluggable database pdb1 disable lost write protection; " +
            "alter pluggable database datafile 3 suspend lost write protection; " +
            "alter pluggable database pdb1 open; " +
            "alter tablespace t suspend lost write protection;")
        val databases = tree.getDescendants(DdlGrammar.ALTER_DATABASE)
        assertThatAst(databases).hasSize(3)
        assertThatAst(databases.map { it.getChildren(DdlGrammar.LOST_WRITE_PROTECTION).size })
            .containsExactly(1, 1, 0)
        val pdbs = tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
        assertThatAst(pdbs).hasSize(3)
        assertThatAst(pdbs.map { it.getChildren(DdlGrammar.LOST_WRITE_PROTECTION).size })
            .containsExactly(1, 1, 0)
        assertThatAst(tree.getDescendants(DdlGrammar.LOST_WRITE_PROTECTION)).hasSize(4)
        val datafile = databases[1].getChildren(DdlGrammar.LOST_WRITE_PROTECTION).single()
        assertThatAst(datafile.getChildren(PlSqlGrammar.CHARACTER_LITERAL).map { it.tokenValue })
            .containsExactly("'a.dbf'")
        assertThatAst(databases[2].getDescendants(DdlGrammar.ALTER_DATAFILE_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATAFILE_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CHANGE_STATE)).hasSize(1)
    }

    @Test
    fun keepsAlterTablespaceLostWriteUnchanged() {
        setRootRule(DdlGrammar.ALTER_TABLESPACE)
        for (action in listOf("enable", "remove", "suspend")) {
            assertThat(p).matches("alter tablespace t $action lost write protection")
        }
        assertThat(p).notMatches("alter tablespace t disable lost write protection")
        assertThat(p).notMatches("alter tablespace t lost write protection")
        assertThat(p).notMatches("alter tablespace t enable lost write protection suspend lost write protection")
    }
}
