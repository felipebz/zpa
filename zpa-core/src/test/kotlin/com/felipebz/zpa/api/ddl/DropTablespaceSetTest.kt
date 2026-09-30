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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.Test

class DropTablespaceSetTest : RuleTest() {

    private val invalidOptions = listOf(
        "drop",
        "keep",
        "quota",
        "drop quota keep quota",
        "keep quota drop quota",
        "including",
        "including datafiles",
        "and datafiles",
        "keep datafiles",
        "cascade constraints",
        "including contents and",
        "including contents keep",
        "including contents cascade",
        "including contents and datafiles keep datafiles",
        "including contents keep datafiles and datafiles",
        "including contents cascade constraints and datafiles",
        "including contents drop quota",
        "including contents including contents",
        "including contents cascade constraints cascade constraints",
        "purge"
    )

    private fun assertMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).matches(source)
        }
    }

    private fun assertNotMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    private fun assertOptionCombinations(prefix: String) {
        for (quota in listOf("", " drop quota", " keep quota")) {
            assertMatches("$prefix$quota;")
            for (datafiles in listOf("", " and datafiles", " keep datafiles")) {
                for (cascade in listOf("", " cascade constraints")) {
                    assertMatches("$prefix$quota including contents$datafiles$cascade;")
                }
            }
        }
    }

    @Test
    fun matchesSetDropWithEveryDocumentedOptionCombination() {
        setRootRule(DdlGrammar.DROP_TABLESPACE_SET)
        assertMatches("drop tablespace set ts", "drop tablespace set \"Shard Space\";")
        assertOptionCombinations("drop tablespace set ts")
    }

    @Test
    fun matchesOrdinaryDropWithOptionalExistenceCheck() {
        setRootRule(DdlGrammar.DROP_TABLESPACE)
        assertMatches("drop tablespace ts", "drop tablespace \"SET\";", "drop tablespace if exists \"User Space\";")
        assertOptionCombinations("drop tablespace ts")
        assertOptionCombinations("drop tablespace if exists ts")
    }

    @Test
    fun rejectsMalformedSetHeadersAndSuffixes() {
        setRootRule(DdlGrammar.DROP_TABLESPACE_SET)
        assertNotMatches(
            "drop tablespace set;",
            "drop tablespace set if exists ts;",
            "drop tablespace if exists set ts;",
            "drop tablespace set schema.ts;"
        )
        for (options in invalidOptions) {
            assertNotMatches("drop tablespace set ts $options;")
        }
    }

    @Test
    fun validatesOrdinaryDropInsteadOfAcceptingArbitraryTokens() {
        setRootRule(DdlGrammar.DROP_TABLESPACE)
        assertNotMatches(
            "drop tablespace;",
            "drop tablespace if exists;",
            "drop tablespace if not exists ts;",
            "drop tablespace ts if exists;",
            "drop tablespace schema.ts;",
            "drop tablespace set;",
            "drop tablespace set ts;"
        )
        for (options in invalidOptions) {
            assertNotMatches("drop tablespace ts $options;")
        }
    }

    @Test
    fun cannotFallBackToGenericDropOrSplitMalformedSuffixIntoAnotherStatement() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertNotMatches(
            "drop tablespace;",
            "drop tablespace if exists;",
            "drop tablespace if not exists ts;",
            "drop tablespace ts if exists;",
            "drop tablespace schema.ts;",
            "drop tablespace set;",
            "drop tablespace set schema.ts;",
            "drop tablespace set if exists ts;",
            "drop tablespace if exists set ts;",
            "drop tablespace set ts drop quota drop quota;",
            "drop tablespace ts drop quota drop quota;",
            "drop tablespace set ts including contents drop quota;"
        )
        for (prefix in listOf("drop tablespace ts", "drop tablespace set ts")) {
            for (options in invalidOptions) {
                assertNotMatches("$prefix $options;")
            }
        }
    }

    @Test
    fun keepsOtherGenericDropsAndParsesConsecutiveStatements() {
        setRootRule(DdlGrammar.DROP_COMMAND)
        assertMatches("drop table t;", "drop index i", "drop user u cascade;", "drop materialized view mv;")
        assertNotMatches("drop tablespace ts;", "drop tablespace set ts;", "drop tablespace set;")

        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertMatches(
            "drop tablespace set ts",
            "drop tablespace ts",
            "drop table t; drop tablespace set ts keep quota including contents keep datafiles cascade constraints; drop tablespace if exists tbs; drop index i;",
            "drop tablespace set ts\n/\ndrop tablespace tbs\n/"
        )
    }

    @Test
    fun exposesOnlyIntendedDropStatementBoundaries() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val node = p.parse("drop tablespace set ts drop quota including contents and datafiles cascade constraints; drop tablespace tbs keep quota including contents keep datafiles;")
        assertThatAst(node.getDescendants(DdlGrammar.DROP_TABLESPACE_SET)).hasSize(1)
        assertThatAst(node.getDescendants(DdlGrammar.DROP_TABLESPACE)).hasSize(1)
        assertThatAst(node.getDescendants(DdlGrammar.TABLESPACE_DROP_OPTIONS)).isEmpty()
        assertThatAst(node.getDescendants(DdlGrammar.DROP_COMMAND)).isEmpty()
    }
}
