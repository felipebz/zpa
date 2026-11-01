//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class PdbApplicationSyncTest : RuleTest() {

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
    fun matchesApplicationNames() {
        matches(
            "application a sync", "application a sync;", "application \"A\" sync", "application a, b sync",
            "application hrapp, payrollapp, employeesapp sync", "application a , b , c sync", "application a ,b ,c sync",
            "application \"A\", \"b\" sync"
        )
    }

    @Test
    fun matchesAllAndAllExcept() {
        matches(
            "application all sync", "application all except a sync", "application all except hrapp, payrollapp sync",
            "application all except a, b sync", "application all except a, b, c sync", "application all except \"A\" sync"
        )
    }

    @Test
    fun matchesNamesWrittenLikeKeywords() {
        matches(
            "application sync sync", "application patch sync", "application sync, sync sync", "application a, sync sync",
            "application all except sync sync", "application all except a, sync sync", "application sync sync to 'v'",
            "application sync sync to patch 1"
        )
    }

    @Test
    fun matchesSyncToVersionAndPatch() {
        matches(
            "application a sync to '1.0'", "application a sync to 'v 1,0.'", "application a sync to ''",
            "application a sync to q'[1.0]'", "application a sync to N'1'", "application a sync to patch 101",
            "application a sync to patch 0", "application a sync to patch 1.5", "application a sync to patch 1e2",
            "application \"A\" sync to patch 5;"
        )
    }

    @Test
    fun rejectsWhitespaceSeparatedNames() {
        notMatches(
            "application hrapp payrollapp employeesapp sync", "application all except hrapp payrollapp sync",
            "application a b sync", "application a b c sync", "application a, b c sync", "application a b, c sync",
            "application \"A\" \"b\" sync", "application all except a b sync", "application all except a, b c sync",
            "application sync sync sync", "application a sync sync", "application a b sync to 'v'"
        )
    }

    @Test
    fun rejectsMalformedNames() {
        notMatches(
            "application sync", "application a.b sync", "application 'a' sync", "application 1a sync",
            "application to sync", "application a to sync", "application a, sync", "application , a sync",
            "application a,, b sync", "application a, b, sync", "application", "application a"
        )
    }

    @Test
    fun rejectsMalformedAllExcept() {
        notMatches(
            "application all except sync", "application all except (a) sync", "application all except (a b) sync",
            "application all except all sync", "application all a sync", "application all all sync", "application a all sync",
            "application all except a, sync", "application all except"
        )
    }

    @Test
    fun rejectsMalformedSyncTo() {
        notMatches(
            "application all sync to 'v'", "application all sync to patch 5", "application all except a sync to 'v'",
            "application a, b sync to 'v'", "application a, b sync to patch 1",
            "application a sync to v1", "application a sync to 1", "application a sync to", "application a sync to to",
            "application a sync to patch", "application a sync to patch 'x'", "application a sync to patch x",
            "application a sync to patch -1", "application a sync to patch '1'", "application a sync to patch 1 2",
            "application a sync patch 1", "application a sync 'v'", "application a sync to 'v' 'w'",
            "application a sync to 'v', 'w'", "application a sync to 'v' patch 1", "application a sync to 'v' sync"
        )
    }

    @Test
    fun rejectsTrailingTokensAndTargets() {
        notMatches(
            "application a sync x", "application a sync,", "application a sync open",
            "application a sync a", "application a sync force", "application a sync pdb1",
            "pdb1 application a sync", "pdb1 application all sync", "pdb1, pdb2 application a sync",
            "all application a sync", "application a sync begin install '1'"
        )
    }

    @Test
    fun keepsOtherClausesWorking() {
        matches("pdb1 open", "save state", "containers host='h'", "pdb1 unplug into '/tmp/p.xml'")
    }

    @Test
    fun buildsDedicatedNode() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter pluggable database application a, b sync;\n" +
                "alter pluggable database application all except a sync;\n" +
                "alter pluggable database application a sync to patch 3;\n" +
                "alter pluggable database pdb1 open;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_PLUGGABLE_DATABASE)).hasSize(4)
        val clauses = tree.getDescendants(DdlGrammar.PDB_APPLICATION_SYNC_CLAUSE)
        assertThatAst(clauses).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CHANGE_STATE)).hasSize(1)
        assertThatAst(clauses.map { c -> c.children.map { it.tokenOriginalValue.lowercase() } }).containsExactly(
            listOf("application", "a", ",", "b", "sync"),
            listOf("application", "all", "except", "a", "sync"),
            listOf("application", "a", "sync", "to", "patch", "3"))
    }
}
