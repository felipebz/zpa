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

class PdbSaveOrDiscardStateTest : RuleTest() {

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
    fun matchesTargetlessAndNamedTargets() {
        matches(
            "save state", "discard state", "SAVE STATE;", "pdb1 save state", "pdb1 discard state",
            "pdb1, pdb2 save state", "pdb1, pdb2, pdb3 discard state", "all save state", "all discard state",
            "all except pdb1 save state", "all except pdb1, pdb2 discard state", "\"Pdb1\" save state"
        )
    }

    @Test
    fun matchesEveryInstancesForm() {
        for (verb in listOf("save", "discard")) {
            matches(
                "$verb state instances = ('a')",
                "$verb state instances = ('a', 'b')",
                "$verb state instances = all",
                "$verb state instances = all except ('a')",
                "$verb state instances = all except ('a', 'b')",
                "pdb1 $verb state instances = ('a');",
                "all except pdb1 $verb state instances = all except ('a')"
            )
        }
    }

    @Test
    fun matchesTargetsNamedLikeTheKeywords() {
        matches(
            "save save state", "discard discard state", "save discard state", "discard save state",
            "save, discard save state", "save, discard discard state", "all except save discard state",
            "open save state", "close discard state", "state save state"
        )
    }

    @Test
    fun rejectsMalformedOrdering() {
        notMatches(
            "save state open", "save state close", "save state save state", "save state discard state",
            "state save", "save states", "save", "discard", "state", "save, state", "save state,",
            "save state x", "pdb1 pdb2 save state", "instances = ('a') save state",
            "open save state open", "save instances = ('a')", "pdb1 save instances = ('a') state"
        )
    }

    @Test
    fun rejectsMalformedAndRepeatedInstancesClauses() {
        notMatches(
            "save state instances", "save state instances =", "save state instances = ()",
            "save state instances = ('a',)", "save state instances = 'a'", "save state instances = a",
            "save state instances = all except", "save state instances = all except ()",
            "save state instances = all except 'a'", "save state instances = all, ('a')",
            "save state instances = ('a') instances = ('b')", "save state instances = all all",
            "discard state instances = ('a') x"
        )
    }

    @Test
    fun rejectsOtherStateOptions() {
        notMatches(
            "save state restricted", "save state force", "save state immediate", "save state services = all",
            "save state instances = ('a') services = all", "discard state restricted force",
            "save state relocate", "save state abort", "save state read only"
        )
    }

    @Test
    fun keepsOpenAndCloseUnchanged() {
        matches("pdb1 open read only instances = all", "close immediate instances = ('a')", "open")
        matches("save open", "discard close")
    }

    @Test
    fun buildsDedicatedNodeOnlyForSaveAndDiscard() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter pluggable database pdb1 save state instances = all;\n" +
                "alter pluggable database discard state;\n" +
                "alter pluggable database save save state;\n" +
                "alter pluggable database pdb1 open;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_SAVE_OR_DISCARD_STATE)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CHANGE_STATE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_OPEN)).hasSize(1)
        val statements = tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)
        val first = statements[0].getFirstChild(DdlGrammar.PDB_CHANGE_STATE)
        assertThatAst(first.getChildren(DdlGrammar.PDB_SAVE_OR_DISCARD_STATE)).hasSize(1)
        assertThatAst(first.getFirstChild(DdlGrammar.PDB_SAVE_OR_DISCARD_STATE).tokenOriginalValue.lowercase())
            .isEqualTo("save")
    }
}
