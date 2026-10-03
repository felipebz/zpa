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

class PdbContainersClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("alter pluggable database $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("alter pluggable database $tail")
    }

    @Test
    fun matchesHost() {
        matches(
            "containers host='myhost.example.com'", "containers host = 'h'", "containers host=''", "containers host=q'[h]'",
            "containers host=N'h'", "CONTAINERS HOST='h';"
        )
    }

    @Test
    fun matchesPort() {
        matches("containers port=1599", "containers port = 1599", "containers port=0", "containers port=65536", "containers port=1e3",
            "containers port=1.5")
    }

    @Test
    fun matchesDefaultTarget() {
        matches(
            "containers default target = (c1)", "containers default target = none", "containers default target = (\"C1\")",
            "containers default target = (none)", "containers default target=(c1);"
        )
    }

    @Test
    fun matchesNamedTargets() {
        matches(
            "pdb1 containers host='h'", "pdb1 containers port=1", "pdb1 containers default target = none",
            "\"Pdb1\" containers host='h'", "containers containers host='h'", "host containers host='h'",
            "port containers port=1"
        )
    }

    @Test
    fun rejectsMalformedHostAndPort() {
        notMatches(
            "containers host=h", "containers host=\"h\"", "containers host=1", "containers host='a' || 'b'", "containers host",
            "containers host=", "containers host 'h'", "containers port=-1", "containers port=+5", "containers port='1599'",
            "containers port=p", "containers port", "containers port=", "containers port 1599", "containers port=(1599)",
            "containers port=1+1", "containers hosts='h'", "containers ports=1", "container host='h'"
        )
    }

    @Test
    fun rejectsMalformedDefaultTarget() {
        notMatches(
            "containers default target = (c1, c2)", "containers default target = (a.b)", "containers default target = ()",
            "containers default target (c1)", "containers default = (c1)", "containers target = (c1)",
            "containers default target =", "containers default target"
        )
    }

    @Test
    fun rejectsCombinedAndRepeatedProperties() {
        notMatches(
            "containers", "containers host='h' port=1", "containers host='h', port=1", "containers port=1 host='h'",
            "containers host='h' host='g'", "containers port=1 port=2", "containers host='h' default target = none",
            "containers host='h' x", "containers host='h',"
        )
    }

    @Test
    fun rejectsInvalidTargets() {
        notMatches(
            "pdb1, pdb2 containers host='h'", "all containers host='h'", "all except pdb1 containers host='h'",
            "pdb1 pdb2 containers host='h'", "default containers host='h'", "pdb1 open containers host='h'",
            "containers host='h' open", "containers host='h' pdb1"
        )
    }

    @Test
    fun keepsOtherClausesWorking() {
        matches("pdb1 open", "save state", "pdb1 unplug into '/tmp/p.xml'", "enable lost write protection")
    }

    @Test
    fun buildsDedicatedNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter pluggable database containers host='h';\n" +
                "alter pluggable database pdb1 containers port=1599;\n" +
                "alter pluggable database pdb1 open;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CONTAINERS_CLAUSE)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CHANGE_STATE)).hasSize(1)
        val clause = tree.getDescendants(DdlGrammar.PDB_CONTAINERS_CLAUSE)[1]
        assertThatAst(clause.children.map { it.tokenOriginalValue.lowercase() })
            .containsExactly("containers", "port", "=", "1599")
    }
}
