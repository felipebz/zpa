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

class CreateLockdownProfileTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_LOCKDOWN_PROFILE)
    }

    private fun matches(vararg statements: String) {
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notMatches(vararg statements: String) {
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    @Test
    fun matchesBareAndBasedProfiles() {
        matches(
            "create lockdown profile hr", "create lockdown profile hr;", "CREATE LOCKDOWN PROFILE hr_prof;",
            "create lockdown profile hr from base", "create lockdown profile hr from base;",
            "create lockdown profile hr including base", "create lockdown profile hr_prof including private_dbaas;",
            "create lockdown profile hr from private_dbaas",
            "create lockdown profile \"Hr\"", "create lockdown profile \"Hr\" from \"Base\"",
            "create lockdown profile hr including \"Base\"", "create lockdown profile hr1_#\$ from base1_#\$"
        )
    }

    @Test
    fun matchesNonreservedNames() {
        matches(
            "create lockdown profile profile", "create lockdown profile lockdown", "create lockdown profile feature",
            "create lockdown profile statement", "create lockdown profile including", "create lockdown profile if",
            "create lockdown profile including from base", "create lockdown profile hr from profile",
            "create lockdown profile hr from including"
        )
    }

    @Test
    fun rejectsMalformedProfiles() {
        notMatches(
            "create lockdown profile", "create lockdown profile 'hr'", "create lockdown profile a.b",
            "create lockdown profile 1hr", "create lockdown profile select", "create lockdown profile option",
            "create lockdown profile hr x", "create lockdown profile hr,", "create lockdown profile hr, h2",
            "create lockdown profile hr from", "create lockdown profile hr including", "create lockdown profile hr from 'a'",
            "create lockdown profile hr from a.b", "create lockdown profile hr from 1base",
            "create lockdown profile hr from select", "create lockdown profile hr including from",
            "create lockdown profile hr from a, b", "create lockdown profile hr including a, b",
            "create lockdown hr", "create profile lockdown hr", "create lockdown profile from including base"
        )
    }

    @Test
    fun rejectsCombinedAndRepeatedBases() {
        notMatches(
            "create lockdown profile hr from base including base2", "create lockdown profile hr including base from base2",
            "create lockdown profile hr from a from b", "create lockdown profile hr including a including b"
        )
    }

    @Test
    fun rejectsOtherCreateModifiersAndOptions() {
        notMatches(
            "create or replace lockdown profile hr", "create public lockdown profile hr",
            "create editionable lockdown profile hr", "create lockdown profile hr using base",
            "create lockdown profile hr based on base", "create lockdown profile hr include base",
            "create lockdown profile hr container = all", "create lockdown profile hr sharing = none",
            "create lockdown profile hr from base disable feature = ('X')", "create lockdown profile hr if not exists"
        )
    }

    @Test
    fun buildsDedicatedNodeAndKeepsAlterSeparate() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "create lockdown profile hr;\n" +
                "create lockdown profile hr2 including private_dbaas;\n" +
                "alter lockdown profile hr disable feature = ('NETWORK_ACCESS');\n")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_LOCKDOWN_PROFILE)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_LOCKDOWN_PROFILE)).hasSize(1)
        val second = tree.getDescendants(DdlGrammar.CREATE_LOCKDOWN_PROFILE)[1]
        assertThatAst(second.children.map { it.tokenOriginalValue.lowercase() }).containsExactly(
            "create", "lockdown", "profile", "hr2", "including", "private_dbaas", ";")
    }
}
