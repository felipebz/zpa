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

class AlterDiskgroupTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DISKGROUP)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).matches("alter diskgroup $tail")
        }
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).notMatches("alter diskgroup $tail")
        }
    }

    @Test
    fun matchesAddDisk() {
        matches(
            "dg add disk '/d/d1'",
            "dg add disk '/d/d1';",
            "\"dg\" add disk '/d/d1'",
            "dg add disk '/d/d1' name n1",
            "dg add disk '/d/d1' size 10g",
            "dg add disk '/d/d1' size 10 m force",
            "dg add disk '/d/d1' name n1 size 10g noforce",
            "dg add disk '/d/d1' name n1 force",
            "dg add disk '/d/d1', '/d/d2' name n2, '/d/d3' size 1g",
            "dg add failgroup fg1 disk '/d/d1'",
            "dg add quorum disk '/d/d1'",
            "dg add regular disk '/d/d1'",
            "dg add quorum failgroup fg1 disk '/d/d1'",
            "dg add regular failgroup fg1 disk '/d/d1', '/d/d2'",
            "dg add site s1 disk '/d/d1'",
            "dg add site s1 failgroup fg1 disk '/d/d1'",
            "dg add site s1 quorum failgroup fg1 disk '/d/d1'",
            "dg add site s1 regular disk '/d/d1'",
            "dg add failgroup f1 disk '/d/d1' failgroup f2 disk '/d/d2'",
            "dg add site s1 failgroup f1 disk '/d/d1' site s2 failgroup f2 disk '/d/d2' name n"
        )
    }

    @Test
    fun rejectsMalformedAddDisk() {
        notMatches(
            "dg add",
            "dg add disks '/d/d1'",
            "dg add disk",
            "dg add disk d1",
            "dg add disk \"d1\"",
            "dg add disk '/d/d1' name",
            "dg add disk '/d/d1' name 'n'",
            "dg add disk '/d/d1' size",
            "dg add disk '/d/d1' size 'x'",
            "dg add disk '/d/d1' size 10g name n1",
            "dg add disk '/d/d1' force name n1",
            "dg add disk '/d/d1' force size 1g",
            "dg add disk '/d/d1' name a name b",
            "dg add disk '/d/d1' size 1g size 2g",
            "dg add disk '/d/d1' force noforce",
            "dg add disk '/d/d1' force force",
            "dg add disk '/d/d1',",
            "dg add disk '/d/d1', , '/d/d2'",
            "dg add failgroup disk '/d/d1'",
            "dg add failgroup fg1 site s1 disk '/d/d1'",
            "dg add quorum site s1 disk '/d/d1'",
            "dg add failgroup fg1 quorum disk '/d/d1'",
            "dg add quorum regular disk '/d/d1'",
            "dg add site disk '/d/d1'",
            "dg add site s1 s2 disk '/d/d1'",
            "dg add failgroup f1",
            "dg add disk '/d/d1', add disk '/d/d2'",
            "dg add disk '/d/d1', drop disk d2"
        )
    }

    @Test
    fun matchesDropDisk() {
        matches(
            "dg drop disk d1",
            "dg drop disk d1;",
            "dg drop disk d1 force",
            "dg drop disk d1 noforce",
            "dg drop disk d1, d2 force, d3 noforce, d4",
            "dg drop quorum disk d1",
            "dg drop regular disk d1 force",
            "dg drop disks in failgroup f1",
            "dg drop disks in quorum failgroup f1 force",
            "dg drop disks in regular failgroup f1, f2 noforce",
            "dg drop disks in failgroup f1 force, f2",
            "dg drop disk \"d1\""
        )
    }

    @Test
    fun rejectsMalformedDropDisk() {
        notMatches(
            "dg drop",
            "dg drop disk",
            "dg drop disk '/d/d1'",
            "dg drop disk d1,",
            "dg drop disk d1, , d2",
            "dg drop disk d1 force force",
            "dg drop disk d1 force noforce",
            "dg drop disk quorum d1",
            "dg drop disks d1",
            "dg drop disks in failgroup",
            "dg drop disks failgroup f1",
            "dg drop quorum disks in failgroup f1",
            "dg drop disks in failgroup f1 f2",
            "dg drop disk d1 name n",
            "dg drop disk d1 size 1g"
        )
    }

    @Test
    fun matchesAddAndDropCombinations() {
        matches(
            "dg drop disk d1, add disk '/d/d2'",
            "dg drop disk d1 add disk '/d/d2'",
            "dg drop disk d1, drop disk d2",
            "dg drop disk d1 force, drop disks in failgroup f1 noforce",
            "dg add disk '/d/d1' drop disk d2",
            "dg add disk '/d/d1' add disk '/d/d2'",
            "dg drop disk d1, add disk '/d/d2' size 1g drop disk d3 add failgroup f disk '/d/d4' rebalance power 3"
        )
        notMatches(
            "dg drop disk d1, add",
            "dg drop disk d1,, add disk '/d/d2'",
            "dg add disk '/d/d1', drop disk d2",
            "dg drop disk d1, resize all",
            "dg add disk '/d/d1' resize all",
            "dg resize all, add disk '/d/d1'",
            "dg resize all add disk '/d/d1'"
        )
    }

    @Test
    fun matchesUndropDisks() {
        matches(
            "dg undrop disks",
            "dg undrop disks;",
            "dg1, dg2 undrop disks",
            "dg1, dg2, dg3 undrop disks",
            "all undrop disks"
        )
        notMatches(
            "undrop disks",
            "dg undrop disk",
            "dg undrop",
            "dg undrop disks rebalance",
            "dg undrop disks x",
            "dg1, undrop disks",
            "dg1,, dg2 undrop disks",
            "all, dg undrop disks",
            "dg, all undrop disks",
            "all dg undrop disks",
            "dg add disk '/d/d1' undrop disks"
        )
    }

    @Test
    fun matchesResizeAll() {
        matches(
            "dg resize all",
            "dg resize all;",
            "dg resize all size 36g",
            "dg resize all size 36 G",
            "dg resize all size 1024",
            "dg resize all size 10k",
            "dg resize all size 2t",
            "dg resize all size 1g rebalance power 2 wait"
        )
        notMatches(
            "dg resize",
            "dg resize size 1g",
            "dg resize all size",
            "dg resize all size 1g size 2g",
            "dg resize all size '1g'",
            "dg resize all size -1g",
            "dg resize all 1g",
            "dg resize all all",
            "dg resize disk d1 size 1g",
            "dg1, dg2 resize all"
        )
    }

    @Test
    fun matchesRebalance() {
        matches(
            "dg rebalance",
            "dg rebalance;",
            "dg rebalance power 11",
            "dg rebalance power 11 wait",
            "dg rebalance power 0 nowait",
            "dg rebalance wait",
            "dg rebalance nowait",
            "dg rebalance with balance",
            "dg rebalance with restore, balance, prepare, compact",
            "dg rebalance without compact",
            "dg rebalance without balance, prepare power 3 nowait",
            "dg rebalance with balance prepare",
            "dg rebalance with compact power 1024 wait",
            "dg rebalance modify power",
            "dg rebalance modify power 5",
            "dg rebalance modify power 0"
        )
    }

    @Test
    fun rejectsMalformedRebalance() {
        notMatches(
            "dg rebalance with",
            "dg rebalance without",
            "dg rebalance with resync",
            "dg rebalance with rebuild",
            "dg rebalance with balance,",
            "dg rebalance with balance, , prepare",
            "dg rebalance with balance without compact",
            "dg rebalance with balance with compact",
            "dg rebalance power",
            "dg rebalance power -1",
            "dg rebalance power 1.5",
            "dg rebalance power 'x'",
            "dg rebalance power 1 power 2",
            "dg rebalance wait power 5",
            "dg rebalance wait nowait",
            "dg rebalance power 1 wait power 2",
            "dg rebalance power 5 with balance",
            "dg rebalance modify",
            "dg rebalance modify power 5 wait",
            "dg rebalance modify power 1 2",
            "dg rebalance with balance modify power",
            "dg rebalance modify power with balance",
            "dg rebalance x",
            "dg1, dg2 rebalance",
            "all rebalance"
        )
    }

    @Test
    fun matchesRebalanceAfterStructuralClauses() {
        matches(
            "dg add disk '/d/d1' rebalance power 3 wait",
            "dg drop disk d1 force rebalance wait",
            "dg drop disks in failgroup f1 rebalance with balance",
            "dg resize all size 10g rebalance modify power 4",
            "dg add disk '/d/d1' rebalance"
        )
        notMatches(
            "dg add disk '/d/d1' rebalance rebalance",
            "dg add disk '/d/d1' rebalance power 3 add disk '/d/d2'"
        )
    }

    @Test
    fun rejectsMalformedNamesAndDeferredClauses() {
        notMatches(
            "add disk '/d/d1'",
            "a.b add disk '/d/d1'",
            "+dg add disk '/d/d1'",
            "'dg' add disk '/d/d1'",
            "dg dg2 add disk '/d/d1'",
            "dg1, dg2 add disk '/d/d1'",
            "dg",
            "dg mount",
            "dg dismount force",
            "dg replace disk d1 with '/d/d2'",
            "dg rename disk d1 to d2",
            "dg online all",
            "dg offline disk d1",
            "dg check all",
            "dg add template t attributes (fine)",
            "dg add directory '+dg/d'",
            "dg add alias '+dg/a' for '+dg.1.1'",
            "dg scrub",
            "dg add filegroup fg database none"
        )
    }

    @Test
    fun separatesStatementAndClauseNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter diskgroup dg add failgroup f1 disk '/d/d1' name n1, '/d/d2' force " +
            "drop disk d3 force, drop disks in failgroup f4 rebalance power 3 wait; " +
            "alter diskgroup dg resize all size 36g; " +
            "alter diskgroup dg1, dg2 undrop disks; " +
            "alter diskgroup dg rebalance modify power 5; " +
            "create table diskgroup (disk number, disks number, power number, site number, quorum number, balance number);")
        val statements = tree.getDescendants(DdlGrammar.ALTER_DISKGROUP)
        assertThatAst(statements).hasSize(4)
        val first = statements[0]
        assertThatAst(first.getChildren(DdlGrammar.ADD_DISK_CLAUSE)).hasSize(1)
        assertThatAst(first.getChildren(DdlGrammar.DROP_DISK_CLAUSE)).hasSize(2)
        assertThatAst(first.getChildren(DdlGrammar.REBALANCE_DISKGROUP_CLAUSE)).hasSize(1)
        assertThatAst(first.getDescendants(DdlGrammar.QUALIFIED_DISK_CLAUSE)).hasSize(2)
        assertThatAst(statements[1].getChildren(DdlGrammar.RESIZE_DISK_CLAUSE)).hasSize(1)
        assertThatAst(statements[1].getDescendants(DdlGrammar.INDEX_SIZE_CLAUSE)).hasSize(1)
        assertThatAst(statements[2].getChildren(DdlGrammar.UNDROP_DISK_CLAUSE)).hasSize(1)
        assertThatAst(statements[3].getChildren(DdlGrammar.REBALANCE_DISKGROUP_CLAUSE)).hasSize(1)
    }
}
