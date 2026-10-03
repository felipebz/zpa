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

class ManagedStandbyRecoveryTest : RuleTest() {

    private val managed = "recover managed standby database"

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("alter database $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("alter database $tail")
    }

    @Test
    fun matchesStopOptions() {
        matches(managed, "$managed;", "$managed cancel", "$managed finish", "d1 $managed", "RECOVER MANAGED STANDBY DATABASE FINISH")
    }

    @Test
    fun matchesEachStartOption() {
        for (option in listOf(
            "using archived logfile", "disconnect", "disconnect from session", "nodelay", "until change 5",
            "until consistent", "using instances all", "using instances 2", "parallel", "parallel 4", "noparallel"
        )) matches("$managed $option")
    }

    @Test
    fun matchesCombinedStartOptionsInAnyOrder() {
        matches(
            "$managed disconnect nodelay parallel 2 until change 5 using archived logfile",
            "$managed using archived logfile disconnect from session nodelay",
            "$managed using instances all using archived logfile noparallel",
            "$managed nodelay nodelay", "$managed until change 5 until change 6", "$managed parallel 4 parallel 2"
        )
    }

    @Test
    fun rejectsMalformedManagedStandbyRecovery() {
        notMatches(
            "recover managed standby", "recover managed database", "recover managed", "recover standby managed database",
            "$managed database", "$managed x", "$managed nodelay x", "$managed,", "$managed nodelay,",
            "$managed disconnect session", "$managed disconnect from", "$managed until change", "$managed until change x",
            "$managed until consistent change 5", "$managed until time 'x'", "$managed until cancel",
            "$managed using archived", "$managed using logfile", "$managed using instances",
            "$managed using instances all 2", "$managed using instances 'x'", "$managed using instance 2",
            "$managed noparallel 4", "$managed parallel 'x'", "$managed parallel parallel x",
            "$managed cancel nodelay", "$managed finish nodelay", "$managed cancel cancel", "$managed finish finish",
            "$managed nodelay finish", "$managed nodelay cancel", "$managed finish cancel", "$managed finish parallel 2",
            "$managed cancel parallel", "$managed delay 5", "$managed next 5", "$managed new primary"
        )
    }

    @Test
    fun matchesDeprecatedFinishAndCancelForms() {
        matches(
            "$managed finish force", "$managed finish wait", "$managed finish nowait",
            "$managed cancel immediate", "$managed cancel wait", "$managed cancel nowait",
            "$managed cancel immediate wait", "$managed cancel immediate nowait",
            "$managed cancel wait immediate", "$managed cancel nowait immediate", "$managed cancel nowait;"
        )
    }

    @Test
    fun rejectsInvalidDeprecatedCombinations() {
        notMatches(
            "$managed cancel wait nowait", "$managed cancel nowait wait", "$managed cancel immediate immediate",
            "$managed cancel nowait nowait", "$managed cancel wait wait", "$managed cancel force",
            "$managed cancel immediate force", "$managed cancel immediate x", "$managed immediate", "$managed nodelay immediate",
            "$managed finish force nowait", "$managed finish force wait", "$managed finish nowait force",
            "$managed finish nowait wait", "$managed finish immediate", "$managed finish skip", "$managed finish wait wait",
            "$managed nowait", "$managed wait", "$managed parallel wait", "$managed disconnect nowait"
        )
    }

    @Test
    fun matchesUsingCurrentLogfile() {
        matches(
            "$managed using current logfile", "$managed using current logfile disconnect",
            "$managed disconnect using current logfile", "$managed using current logfile using archived logfile",
            "$managed using archived logfile using current logfile",
            "$managed using current logfile nodelay parallel 2 until change 5", "$managed using current logfile;"
        )
        notMatches(
            "$managed using current", "$managed using current logfile finish", "$managed using current logfile x",
            "$managed using logfile current"
        )
    }

    @Test
    fun matchesRecoverToLogicalStandby() {
        matches(
            "recover to logical standby d1", "recover to logical standby keep identity", "recover to logical standby \"d1\";",
            "recover to logical standby identity", "d1 recover to logical standby d2"
        )
        notMatches(
            "recover to logical standby", "recover to logical standby d1 keep identity", "recover to logical standby keep",
            "recover to logical standby a.b", "recover to logical standby 'd1'", "recover to logical standby d1 x",
            "recover to logical standby d1 d2", "recover to logical standby keep identity x", "recover to logical d1",
            "recover to standby d1", "recover logical standby d1"
        )
    }

    @Test
    fun keepsGeneralRecoveryUnchanged() {
        matches("recover standby database", "recover database until cancel", "recover automatic database", "recover logfile 'a.log'",
            "recover tablespace t parallel 2", "recover continue default", "recover cancel", "recover")
        notMatches("recover database managed", "recover automatic managed standby database")
    }

    @Test
    fun buildsDedicatedNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "alter database recover managed standby database disconnect parallel 2;\n" +
                "alter database recover to logical standby keep identity;\n" +
                "alter database recover automatic database;\n" +
                "alter database recover managed standby database;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_DATABASE)).hasSize(4)
        assertThatAst(tree.getDescendants(DdlGrammar.MANAGED_STANDBY_RECOVERY)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.RECOVER_TO_LOGICAL_STANDBY)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.GENERAL_RECOVERY)).hasSize(1)
        val first = tree.getFirstDescendant(DdlGrammar.MANAGED_STANDBY_RECOVERY)!!
        assertThatAst(first.children.map { it.tokenOriginalValue.lowercase() })
            .containsExactly("recover", "managed", "standby", "database", "disconnect", "parallel", "2")
    }
}
