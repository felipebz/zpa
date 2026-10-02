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

class MirrorCopyTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private val database = "alter database"
    private val pdb = "alter pluggable database"

    private fun matches(prefix: String, vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs("$prefix $tail").matches("$prefix $tail")
    }

    private fun notMatches(prefix: String, vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs("$prefix $tail").notMatches("$prefix $tail")
    }

    @Test
    fun matchesPrepareInBothStatements() {
        for (prefix in listOf(database, pdb)) {
            matches(
                prefix,
                "prepare mirror copy m1",
                "prepare mirror copy m1;",
                "prepare mirror copy \"m1\"",
                "prepare mirror copy m1 with unprotected redundancy",
                "prepare mirror copy m1 with mirror redundancy",
                "prepare mirror copy m1 with high redundancy",
                "d1 prepare mirror copy m1 with high redundancy",
                "\"d1\" prepare mirror copy m1"
            )
        }
        matches(pdb, "prepare prepare mirror copy m1")
    }

    @Test
    fun matchesForDatabaseOnlyInPdb() {
        matches(
            pdb,
            "prepare mirror copy m1 for database cdb1",
            "prepare mirror copy m1 for database \"cdb1\"",
            "prepare mirror copy m1 with high redundancy for database cdb1",
            "p1 prepare mirror copy m1 with mirror redundancy for database cdb1"
        )
        notMatches(
            database,
            "prepare mirror copy m1 for database cdb1",
            "prepare mirror copy m1 with high redundancy for database cdb1",
            "d1 prepare mirror copy m1 for database cdb1"
        )
        notMatches(
            pdb,
            "prepare mirror copy m1 for database",
            "prepare mirror copy m1 for database 'cdb1'",
            "prepare mirror copy m1 for database a.b",
            "prepare mirror copy m1 for pluggable database p1",
            "prepare mirror copy m1 for database a for database b",
            "prepare mirror copy m1 for database cdb1 with high redundancy"
        )
    }

    @Test
    fun rejectsMalformedPrepare() {
        for (prefix in listOf(database, pdb)) {
            notMatches(
                prefix,
                "prepare",
                "prepare mirror",
                "prepare mirror copy",
                "prepare copy m1",
                "prepare mirror m1",
                "prepare mirror copy 'm1'",
                "prepare mirror copy a.b",
                "prepare mirror copy m1 with high",
                "prepare mirror copy m1 with redundancy",
                "prepare mirror copy m1 high redundancy",
                "prepare mirror copy m1 with external redundancy",
                "prepare mirror copy m1 with normal redundancy",
                "prepare mirror copy m1 with parity redundancy",
                "prepare mirror copy m1 with double redundancy",
                "prepare mirror copy m1 with high redundancy with mirror redundancy",
                "prepare mirror copy m1 with high redundancy redundancy",
                "prepare mirror copy m1, m2",
                "prepare mirror copy m1 x",
                "m1 d1 prepare mirror copy m1"
            )
        }
        notMatches(database, "prepare prepare mirror copy m1")
    }

    @Test
    fun matchesAndRejectsDropMirrorCopy() {
        for (prefix in listOf(database, pdb)) {
            matches(prefix, "drop mirror copy m1", "drop mirror copy m1;", "drop mirror copy \"m1\"", "d1 drop mirror copy m1")
            notMatches(
                prefix,
                "drop mirror copy",
                "drop mirror copy 'm1'",
                "drop mirror copy m1 for database cdb1",
                "drop mirror copy m1 with high redundancy",
                "drop mirror copy m1, m2",
                "drop mirror copy m1 x",
                "drop mirror m1",
                "drop copy m1"
            )
        }
    }

    @Test
    fun keepsOtherClausesAndTargetsSeparate() {
        notMatches(pdb, "p1, p2 prepare mirror copy m1", "all prepare mirror copy m1", "p1 open prepare mirror copy m1")
        matches(database, "drop logfile group 3", "add logfile 'a.log'", "drop supplemental log data")
        matches(pdb, "p1 open", "p1 unplug into '/tmp/p.xml'", "enable lost write protection")
        notMatches(pdb, "p1 save state")
    }

    @Test
    fun exposesSharedNodesUnderBothStatements() {
        val tree = p.parse("alter database d1 prepare mirror copy m1 with high redundancy; " +
            "alter database drop mirror copy m1; " +
            "alter pluggable database p1 prepare mirror copy m2 for database cdb1; " +
            "alter pluggable database drop mirror copy m2; " +
            "alter database drop logfile group 3;")
        val databases = tree.getDescendants(DdlGrammar.ALTER_DATABASE)
        assertThatAst(databases.map { it.getChildren(DdlGrammar.PREPARE_CLAUSE).size }).containsExactly(1, 0, 0)
        assertThatAst(databases.map { it.getChildren(DdlGrammar.DROP_MIRROR_COPY).size }).containsExactly(0, 1, 0)
        val pdbs = tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
        assertThatAst(pdbs.map { it.getChildren(DdlGrammar.PREPARE_CLAUSE).size }).containsExactly(1, 0)
        assertThatAst(pdbs.map { it.getChildren(DdlGrammar.DROP_MIRROR_COPY).size }).containsExactly(0, 1)
        assertThatAst(tree.getDescendants(DdlGrammar.PREPARE_CLAUSE)).hasSize(2)
    }
}
