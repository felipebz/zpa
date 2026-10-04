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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class ForallStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FORALL_STATEMENT)
    }

    @Test
    fun matchesForallWithFixedRangeInsert() {
        assertThat(p).matches("forall foo in 1 .. 2 insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallWithFixedRangeUpdate() {
        assertThat(p).matches("forall foo in 1 .. 2 update tab set value = var(foo).value;")
    }

    @Test
    fun matchesForallWithFixedRangeDelete() {
        assertThat(p).matches("forall foo in 1 .. 2 delete tab where value = var(foo).value;")
    }

    @Test
    fun matchesForallWithFixedRangeMerge() {
        assertThat(p).matches("forall foo in 1 .. 2 merge into dest_tab " +
            "using source_tab on (1 = 2) " +
            "when matched then update set col = val " +
            "when not matched then insert values (val);")
    }

    @Test
    fun matchesForallWithFixedRangeExecuteImmediate() {
        assertThat(p).matches("forall foo in 1 .. 2 execute immediate 'insert into tab values (:1)' " +
            "using var(foo).value;")
    }

    @Test
    fun matchesForallWithVariablesRange() {
        assertThat(p).matches("forall x in foo .. bar.count insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesOf() {
        assertThat(p).matches("forall foo in indices of bar insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesOfWithRange() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2 insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallValuesOf() {
        assertThat(p).matches("forall foo in values of bar insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallValuesWithSaveExceptions() {
        assertThat(p).matches("forall foo in values of bar save exceptions insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesForallIndicesWithSaveExceptions() {
        assertThat(p).matches("forall foo in indices of bar between 1 and 2 save exceptions insert into tab values (var(foo).value);")
    }

    @Test
    fun matchesCollectionOperandsInIndicesOfAndValuesOf() {
        listOf("indices of", "values of").forEach { kind ->
            listOf(
                "ns", "ns(i)", "pkg.ns", "r.ns", "ns(i).x", "f()", "f", "ns(i)(j)", "(ns)", "(ns(i))", "ns.first", "ns + 1", "1", "'text'",
            ).forEach {
                val source = "forall j in $kind $it insert into tab values (j);"
                assertThat(p).describedAs(source).matches(source)
            }
            listOf(
                "", "ns(", "ns(i", "ns,", "ns ns",
            ).forEach {
                val source = "forall j in $kind $it insert into tab values (j);"
                assertThat(p).describedAs(source).notMatches(source)
            }
        }
        listOf("ns", "ns(i)", "r.ns", "ns(i).x", "f()", "ns + 1").forEach {
            val source = "forall j in indices of $it between 1 and k + 1 save exceptions insert into tab values (j);"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf("ns", "ns(i)", "r.ns").forEach {
            val source = "forall j in values of $it save exceptions delete tab where v = j;"
            assertThat(p).describedAs(source).matches(source)
        }
        listOf("ns between 1 and 2", "ns(i) between 1 and 2", "ns(i) between").forEach {
            val source = "forall j in values of $it insert into tab values (j);"
            assertThat(p).describedAs(source).notMatches(source)
        }
        assertThat(p).notMatches("forall j in indices of ns(i) between insert into tab values (j);")
    }

    @Test
    fun buildsCollectionOperandAndBoundsStructure() {
        val indices = p.parse("forall j in indices of ns(i) between 1 and 2 insert into tab values (ns(i)(j));")
        val header = indices.tokens.map { it.originalValue }.takeWhile { it != "insert" }
        org.assertj.core.api.Assertions.assertThat(header).containsExactly(
            "forall", "j", "in", "indices", "of", "ns", "(", "i", ")", "between", "1", "and", "2")
        org.assertj.core.api.Assertions.assertThat(indices.getChildren(PlSqlGrammar.INSERT_STATEMENT)).hasSize(1)
        val range = p.parse("forall j in 1 .. ns.count insert into tab values (j);")
        org.assertj.core.api.Assertions.assertThat(range.tokens.map { it.originalValue }).doesNotContain("indices", "values-of")
        org.assertj.core.api.Assertions.assertThat(range.children.map { it.tokenOriginalValue }).doesNotContain("indices")
    }
}
