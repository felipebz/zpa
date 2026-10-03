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

class AlterPluggableDatabaseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).matches("alter pluggable database $tail")
        }
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).notMatches("alter pluggable database $tail")
        }
    }

    @Test
    fun matchesTargets() {
        matches(
            "open;",
            "close",
            "pdb1 open",
            "\"Pdb 1\" close",
            "pdb1, pdb2, pdb3 open read only",
            "all open",
            "all except pdb1 close immediate",
            "all except pdb1, pdb2 open",
            "open open",
            "close close",
            "open close abort",
            "restricted open restricted"
        )
    }

    @Test
    fun matchesOpenModesAndModifiers() {
        for (mode in listOf("", " read write", " read only", " hybrid read only", " upgrade", " read write upgrade", " resetlogs")) {
            for (restricted in listOf("", " restricted")) {
                for (force in listOf("", " force")) {
                    matches("pdb1 open$mode$restricted$force")
                }
            }
        }
        matches(
            "pdb1 open services = none",
            "pdb1 open read only restricted force services = none",
            "pdb1 open instances = all services = none;"
        )
    }

    @Test
    fun matchesOpenInstancesAndServices() {
        matches(
            "pdb1 open instances = ('ORCLDB_1')",
            "pdb1 open read write instances = ('ORCLDB_1', 'ORCLDB_2')",
            "pdb1 open instances = all",
            "pdb1 open instances = all except ('ORCLDB_1', 'ORCLDB_2')",
            "pdb1 open services = ('s1', 's2')",
            "pdb1 open services = all",
            "pdb1 open services = all except ('s1')",
            "all except pdb1 open read only restricted force instances = ('A') services = all except ('s1', 's2');"
        )
    }

    @Test
    fun matchesCloseForms() {
        matches(
            "pdb1 close",
            "pdb1 close immediate",
            "pdb1 close abort",
            "pdb1 close instances = all",
            "pdb1 close immediate instances = ('A')",
            "pdb1 close abort instances = all except ('A')",
            "pdb1 close relocate",
            "pdb1 close relocate to 'ORCLDB_3'",
            "pdb1 close immediate relocate to 'ORCLDB_3'",
            "pdb1 close norelocate",
            "pdb1 close immediate norelocate",
            "all close relocate to 'X'"
        )
    }

    @Test
    fun rejectsMalformedTargets() {
        notMatches(
            "pdb1 pdb2 open",
            "(pdb1, pdb2) open",
            "pdb1, open",
            "all except open",
            "all except (pdb1) open",
            "s.pdb1 open",
            "all, pdb1 open",
            "pdb1"
        )
    }

    @Test
    fun rejectsMalformedOpen() {
        notMatches(
            "pdb1 open force restricted",
            "pdb1 open restricted read write",
            "pdb1 open read write read only",
            "pdb1 open restricted restricted",
            "pdb1 open force force",
            "pdb1 open read only upgrade",
            "pdb1 open hybrid read only upgrade",
            "pdb1 open upgrade read write",
            "pdb1 open upgrade upgrade",
            "pdb1 open read write resetlogs",
            "pdb1 open upgrade resetlogs",
            "pdb1 open hybrid",
            "pdb1 open hybrid read write",
            "pdb1 open read",
            "pdb1 open write",
            "pdb1 open open",
            "pdb1 open close",
            "pdb1 open instances = all read write",
            "pdb1 open services = all instances = all",
            "pdb1 open instances = all instances = all",
            "pdb1 open services = all services = all",
            "pdb1 open read only banana"
        )
    }

    @Test
    fun rejectsMalformedInstancesAndServices() {
        notMatches(
            "pdb1 open instances = 'A'",
            "pdb1 open instances = all except 'A'",
            "pdb1 open instances = (a)",
            "pdb1 open instances = ()",
            "pdb1 open instances ('A')",
            "pdb1 open instances = ('A',)",
            "pdb1 open instances = all except ()",
            "pdb1 open instances =",
            "pdb1 open services = (s1)",
            "pdb1 open services = 's1'",
            "pdb1 open services = ()",
            "pdb1 open instances = none",
            "pdb1 open services = none services = all",
            "pdb1 open services none",
            "pdb1 open services = none ('s1')",
            "pdb1 open services = none all"
        )
    }

    @Test
    fun rejectsMalformedClose() {
        notMatches(
            "pdb1 close normal",
            "pdb1 close immediate abort",
            "pdb1 close abort immediate",
            "pdb1 close immediate immediate",
            "pdb1 close abort relocate to 'X'",
            "pdb1 close abort norelocate",
            "pdb1 close instances = all relocate to 'X'",
            "pdb1 close relocate to 'X' instances = all",
            "pdb1 close relocate to x",
            "pdb1 close relocate to",
            "pdb1 close relocate to 'X' relocate to 'Y'",
            "pdb1 close relocate norelocate",
            "pdb1 close instances = all instances = all",
            "pdb1 close services = all",
            "pdb1 close restricted",
            "pdb1 close force",
            "pdb1 close immediate banana"
        )
    }

    @Test
    fun matchesUnplug() {
        matches(
            "pdb1 unplug into '/oracle/data/pdb1.xml'",
            "pdb1 unplug into '/tmp/pdb1.pdb';",
            "\"pdb1\" unplug into '/tmp/pdb1.xml'",
            "pdb1 unplug into '/tmp/pdb1.pdb' encrypt using transport_secret",
            "pdb1 unplug into '/tmp/pdb1.pdb' encrypt using \"Secret\"",
            "unplug unplug into '/tmp/pdb1.xml'",
            "open unplug into '/tmp/pdb1.xml'",
            "enable unplug into '/tmp/pdb1.xml'",
            "close unplug into '/tmp/pdb1.xml' encrypt using s"
        )
    }

    @Test
    fun rejectsMalformedUnplug() {
        notMatches(
            "unplug into '/tmp/pdb1.xml'",
            "pdb1 unplug",
            "pdb1 unplug '/tmp/pdb1.xml'",
            "pdb1 unplug into",
            "pdb1 unplug into pdb1",
            "pdb1 unplug into 5",
            "pdb1 unplug into ('/tmp/pdb1.xml')",
            "pdb1 unplug into '/a.xml', '/b.xml'",
            "pdb1, pdb2 unplug into '/tmp/pdb1.xml'",
            "all unplug into '/tmp/pdb1.xml'",
            "all except pdb1 unplug into '/tmp/pdb1.xml'",
            "a.pdb1 unplug into '/tmp/pdb1.xml'",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using 'secret'",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using :s",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using 5",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using a b",
            "pdb1 unplug into '/tmp/pdb1.xml' encrypt using a encrypt using b",
            "pdb1 unplug encrypt using s into '/tmp/pdb1.xml'",
            "pdb1 unplug into '/tmp/pdb1.xml' using s",
            "pdb1 unplug into '/tmp/pdb1.xml' close",
            "pdb1 unplug into '/tmp/pdb1.xml' x",
            "pdb1 open unplug into '/tmp/pdb1.xml'"
        )
    }

    @Test
    fun exposesUnplugAsOwnNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter pluggable database pdb1 unplug into '/tmp/p.pdb' encrypt using s; " +
            "alter pluggable database pdb1 open;")
        val statements = tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
        assertThatAst(statements.map { it.getChildren(DdlGrammar.PDB_UNPLUG_CLAUSE).size }).containsExactly(1, 0)
        assertThatAst(statements[1].getChildren(DdlGrammar.PDB_CHANGE_STATE)).hasSize(1)
    }

    @Test
    fun keepsOtherOperationsUnsupported() {
        notMatches(
            "pdb1 unplug",
            "pdb1 storage (maxsize 500m)",
            "pdb1 datafile all offline",
            "application all except hrapp sync"
        )
    }

    @Test
    fun separatesStatementAndStateChangeNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("alter pluggable database pdb1 open read only; " +
            "alter pluggable database all close immediate relocate to 'X'; " +
            "create table hybrid (instances number, services number, restricted number, relocate number, " +
            "norelocate number, resetlogs number, abort number);")
        val statements = tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
        assertThatAst(statements).hasSize(2)
        assertThatAst(statements.map { it.getFirstChild(DdlGrammar.PDB_CHANGE_STATE) }).doesNotContainNull()
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_OPEN)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CLOSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_INSTANCES_CLAUSE)).isEmpty()
    }
}
