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

class AlterDiskgroupMaintenanceTest : RuleTest() {

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
    fun matchesCheck() {
        matches(
            "dg check",
            "dg check;",
            "dg check repair",
            "dg check norepair",
            "dg check all",
            "dg check all repair",
            "dg check all norepair",
            "dg check disk d1",
            "dg check disk d1, \"d2\" repair",
            "dg check file '+dg/f1.1.1'",
            "dg check file '+dg/f1.1.1', '+dg/f2.2.2' norepair"
        )
        notMatches(
            "dg check repair norepair",
            "dg check repair repair",
            "dg check repair all",
            "dg check all all",
            "dg check all force",
            "dg check disk",
            "dg check disk '+d1'",
            "dg check disk d1 d2",
            "dg check disk d1,",
            "dg check disks in failgroup f1",
            "dg check file",
            "dg check file f1",
            "dg check file '+dg/f.1.1',",
            "dg check rebalance",
            "dg check x",
            "dg1, dg2 check",
            "all check"
        )
    }

    @Test
    fun matchesTemplates() {
        matches(
            "dg add template t1 attributes (unprotected coarse)",
            "dg add template t1 attribute (unprotected coarse)",
            "dg modify template t1 attributes (fine)",
            "dg add template t1 attributes ()",
            "dg add template t1 attributes (mirror)",
            "dg add template t1 attributes (high fine)",
            "dg add template t1 attributes (parity)",
            "dg add template t1 attributes (double coarse)",
            "dg add template t1 attributes (coarse mirror)",
            "dg add template \"t1\" attributes (fine);",
            "dg add template t1 attributes (fine), t2 attributes (coarse)",
            "dg modify template t1 attributes (high), t2 attributes (coarse)",
            "dg alter template t1 attributes (fine)",
            "dg alter template t1 attribute (coarse)",
            "dg alter template t1 attributes (fine), t2 attributes (coarse)",
            "dg alter template t1 attributes (fine) alter template t2 attributes (coarse)",
            "dg alter template t1 attributes (fine) add template t2 attributes (coarse)",
            "dg drop template t1",
            "dg drop template t1, t2",
            "dg add template t1 attributes (fine) add template t2 attributes (coarse)",
            "dg add template t1 attributes (fine) drop template t2",
            "dg drop template t1 drop template t2"
        )
        notMatches(
            "dg add template t1",
            "dg add template t1 attributes",
            "dg add template t1 attributes fine",
            "dg add template t1 attributes (fine",
            "dg add template t1 attributes (mirror, fine)",
            "dg add template t1 attributes (fine) attributes (coarse)",
            "dg add template t1 attributes (random)",
            "dg add template 't1' attributes (fine)",
            "dg add template attributes (fine)",
            "dg add template t1 attributes (fine),",
            "dg add template t1 attributes (fine), add template t2 attributes (fine)",
            "dg alter template t1",
            "dg alter template 't1' attributes (fine)",
            "dg alter template t1 attributes (fine),",
            "dg alter template t1 attributes (fine), alter template t2 attributes (coarse)",
            "dg add template t1 attributes (fine), drop template t2",
            "dg add template t1 attributes (fine) t2 attributes (coarse)",
            "dg drop template",
            "dg drop template t1,",
            "dg drop template 't1'",
            "dg drop template t1, drop template t2",
            "dg drop template t1 attributes (fine)",
            "dg rename template t1 to t2",
            "dg add template t1 attributes (fine) rebalance"
        )
    }

    @Test
    fun matchesDirectories() {
        matches(
            "dg add directory '+dg/d1'",
            "dg add directory '+dg/d1', '+dg/d2'",
            "dg drop directory '+dg/d1'",
            "dg drop directory '+dg/d1' force",
            "dg drop directory '+dg/d1' noforce",
            "dg drop directory '+dg/d1' force, '+dg/d2' noforce, '+dg/d3'",
            "dg rename directory '+dg/d1' to '+dg/d2'",
            "dg rename directory '+dg/d1' to '+dg/d2', '+dg/d3' to '+dg/d4'",
            "dg add directory '+dg/d1' add directory '+dg/d2'",
            "dg add directory '+dg/d1' drop directory '+dg/d2' force"
        )
        notMatches(
            "dg add directory",
            "dg add directory d1",
            "dg add directory '+dg/d1',",
            "dg add directory '+dg/d1' force",
            "dg add directory '+dg/d1', drop directory '+dg/d2'",
            "dg drop directory",
            "dg drop directory d1",
            "dg drop directory '+dg/d1' force force",
            "dg drop directory '+dg/d1' force noforce",
            "dg rename directory '+dg/d1'",
            "dg rename directory '+dg/d1' to",
            "dg rename directory d1 to d2",
            "dg rename directory '+dg/d1' to '+dg/d2' force",
            "dg rename directory '+dg/d1' to '+dg/d2',",
            "dg rename directory '+dg/d1' '+dg/d2'",
            "dg directory '+dg/d1'",
            "dg add directory '+dg/d1' rebalance"
        )
    }

    @Test
    fun matchesAliases() {
        matches(
            "dg add alias '+dg/d1/a1' for '+dg.261.1'",
            "dg add alias '+dg/d1/a1' for '+dg.261.1', '+dg/d1/a2' for '+dg.262.1'",
            "dg drop alias '+dg/d1/a1'",
            "dg drop alias '+dg/d1/a1', '+dg/d1/a2'",
            "dg rename alias '+dg/d1/a1' to '+dg/d1/a2'",
            "dg rename alias '+dg/d1/a1' to '+dg/d1/a2', '+dg/d1/a3' to '+dg/d1/a4'",
            "dg add alias '+dg/a' for '+dg.1.1' add alias '+dg/b' for '+dg.2.1'",
            "dg drop alias '+dg/a' drop alias '+dg/b'"
        )
        notMatches(
            "dg add alias",
            "dg add alias '+dg/a'",
            "dg add alias '+dg/a' for",
            "dg add alias a1 for f1",
            "dg add alias '+dg/a' for '+dg.1.1',",
            "dg add alias '+dg/a' for '+dg.1.1', drop alias '+dg/b'",
            "dg drop alias",
            "dg drop alias a1",
            "dg drop alias '+dg/a' force",
            "dg drop alias '+dg/a',",
            "dg rename alias '+dg/a'",
            "dg rename alias '+dg/a' to",
            "dg alias '+dg/a'",
            "dg add alias '+dg/a' for '+dg.1.1' rebalance"
        )
    }

    @Test
    fun matchesScrub() {
        matches(
            "dg scrub",
            "dg scrub;",
            "dg scrub repair wait",
            "dg scrub norepair",
            "dg scrub file '+dg/f.1.1'",
            "dg scrub disk d1",
            "dg scrub power auto",
            "dg scrub power low",
            "dg scrub power high",
            "dg scrub power max",
            "dg scrub wait",
            "dg scrub nowait",
            "dg scrub force",
            "dg scrub noforce",
            "dg scrub stop",
            "dg scrub file '+dg/f.1.1' repair power low wait force",
            "dg scrub disk d1 norepair power max nowait noforce",
            "dg scrub repair force",
            "dg scrub power auto wait noforce"
        )
        notMatches(
            "dg scrub file",
            "dg scrub file f1",
            "dg scrub file 5",
            "dg scrub disk",
            "dg scrub disk '+d1'",
            "dg scrub disk d1, d2",
            "dg scrub file '+dg/f.1.1', '+dg/f.2.2'",
            "dg scrub file '+dg/f.1.1' disk d1",
            "dg scrub disk d1 file '+dg/f.1.1'",
            "dg scrub power",
            "dg scrub power 5",
            "dg scrub power medium",
            "dg scrub repair norepair",
            "dg scrub wait nowait",
            "dg scrub force noforce",
            "dg scrub power low power high",
            "dg scrub stop stop",
            "dg scrub wait repair",
            "dg scrub power low repair",
            "dg scrub force wait",
            "dg scrub wait power low",
            "dg scrub repair file '+dg/f.1.1'",
            "dg scrub file '+dg/f.1.1' stop",
            "dg scrub repair stop",
            "dg scrub force stop",
            "dg scrub stop force",
            "dg scrub rebalance",
            "dg scrub x"
        )
    }

    @Test
    fun matchesAvailability() {
        matches(
            "dg mount",
            "dg mount;",
            "dg mount restricted",
            "dg mount normal",
            "dg mount force",
            "dg mount noforce",
            "dg mount restricted force",
            "dg mount normal noforce",
            "dg dismount",
            "dg dismount force",
            "dg dismount noforce",
            "dg1, dg2 mount",
            "dg1, dg2 dismount force",
            "all mount",
            "all mount restricted force",
            "all dismount noforce"
        )
        notMatches(
            "dg mount force restricted",
            "dg mount restricted normal",
            "dg mount force force",
            "dg mount force noforce",
            "dg mount x",
            "dg mount rebalance",
            "dg dismount restricted",
            "dg dismount normal",
            "dg dismount force force",
            "dg1, mount",
            "dg1,, dg2 mount",
            "all, dg mount",
            "dg, all dismount",
            "mount"
        )
    }

    @Test
    fun keepsOtherFamiliesDeferredAndEarlierClausesWorking() {
        notMatches(
            "dg replace disk d1 with '/d/d2'",
            "dg rename disk d1 to d2",
            "dg online all",
            "dg offline disk d1",
            "dg add volume v1 size 1g",
            "dg set attribute 'x' = 'y'",
            "dg add quotagroup q1",
            "dg drop file '+dg/f.1.1'",
            "dg add disk '/d/d1' check",
            "dg check scrub",
            "dg mount mount"
        )
        matches(
            "dg add disk '/d/d1' rebalance power 3",
            "dg undrop disks",
            "dg1, dg2 undrop disks",
            "dg resize all size 10g",
            "mount mount",
            "scrub scrub",
            "template drop template template",
            "stop scrub stop"
        )
    }

    @Test
    fun separatesFamilyNodesUnderTheStatement() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter diskgroup dg check all repair; " +
            "alter diskgroup dg add template t1 attributes (unprotected coarse), t2 attributes (fine); " +
            "alter diskgroup dg drop template t3; " +
            "alter diskgroup dg add directory '+dg/d'; " +
            "alter diskgroup dg rename alias '+dg/a' to '+dg/b'; " +
            "alter diskgroup dg scrub file '+dg/f.1.1' repair power low wait; " +
            "alter diskgroup dg1, dg2 dismount force; " +
            "alter diskgroup dg mount restricted; " +
            "alter diskgroup dg undrop disks; " +
            "create table t (mount number, scrub number, alias number, fine number, stop number, repair number);")
        val statements = tree.getDescendants(DdlGrammar.ALTER_DISKGROUP)
        assertThatAst(statements).hasSize(9)
        assertThatAst(statements.map { s ->
            listOf(
                DdlGrammar.CHECK_DISKGROUP_CLAUSE, DdlGrammar.DISKGROUP_TEMPLATE_CLAUSES,
                DdlGrammar.DISKGROUP_DIRECTORY_CLAUSES, DdlGrammar.DISKGROUP_ALIAS_CLAUSES,
                DdlGrammar.SCRUB_CLAUSE, DdlGrammar.DISKGROUP_AVAILABILITY, DdlGrammar.UNDROP_DISK_CLAUSE
            ).sumOf { s.getChildren(it).size }
        }).containsOnly(1)
        assertThatAst(statements[1].getChildren(DdlGrammar.DISKGROUP_TEMPLATE_CLAUSES)).hasSize(1)
        assertThatAst(statements[6].getChildren(DdlGrammar.DISKGROUP_AVAILABILITY)).hasSize(1)
        assertThatAst(statements[7].getChildren(DdlGrammar.DISKGROUP_AVAILABILITY)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.UNDROP_DISK_CLAUSE)).hasSize(1)
    }
}
