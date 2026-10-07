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
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateIndexOrganizedTableTest : RuleTest() {

    private val iot = "create table t (id number primary key, c number) organization index"

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_TABLE)
    }

    private fun assertMatches(vararg tails: String) =
        tails.forEach { assertThat(p).describedAs(it).matches("$iot $it;") }

    private fun assertNotMatches(vararg tails: String) =
        tails.forEach { assertThat(p).describedAs(it).notMatches("$iot $it;") }

    @Test
    fun matchesIncludingWithoutOverflow() {
        assertMatches(
            "including c", "including id", "including c pctthreshold 20", "pctthreshold 20 including c",
            "including c tablespace users", "including c read only", "including c nocompress",
            "including c mapping table", "including c parallel",
        )
    }

    @Test
    fun matchesIncludingAroundOverflow() {
        assertMatches(
            "including c overflow", "including c parallel overflow", "parallel including c overflow",
            "overflow including c", "including c overflow including c",
            "pctthreshold 20 including c tablespace users overflow tablespace users",
        )
    }

    @Test
    fun rejectsMalformedIncluding() {
        assertNotMatches("including", "including 1", "including c, id")
    }

    @Test
    fun matchesParallelInsideAndAfterTheAttributes() {
        assertMatches(
            "parallel", "parallel 2", "noparallel", "parallel overflow", "parallel 2 overflow", "noparallel overflow",
            "read only parallel overflow", "parallel read only overflow", "pctthreshold 20 parallel overflow",
            "parallel pctthreshold 20 overflow", "mapping table parallel overflow", "tablespace users parallel overflow",
            "pctfree 10 parallel overflow", "nocompress parallel overflow", "cache parallel overflow",
            "parallel flashback archive overflow", "parallel no flashback archive overflow",
            "overflow parallel", "overflow parallel 2", "overflow tablespace users parallel",
            "overflow parallel tablespace users", "overflow read only parallel",
        )
    }

    @Test
    fun matchesAttributesAfterOverflow() {
        assertMatches(
            "overflow pctfree 10 read only", "overflow mapping table", "overflow nocompress", "overflow including c",
            "overflow mapping table parallel", "overflow parallel mapping table", "overflow read only mapping table",
        )
        assertNotMatches("overflow pctthreshold 20", "overflow overflow")
    }

    @Test
    fun keepsInternalAndTrailingAttributesInTheClauseNode() {
        fun clause(sql: String) = p.parse(sql).getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)

        val internal = clause("$iot parallel 2 including c overflow")
        assertThatAst(internal.hasDirectChildren(DdlGrammar.INDEX_PARALLEL_CLAUSE)).isTrue()
        assertThatAst(internal.getChildren(PlSqlKeyword.INCLUDING)).hasSize(1)
        assertThatAst(internal.hasDirectChildren(DdlGrammar.INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE)).isTrue()

        val withoutOverflow = clause("$iot including c")
        assertThatAst(withoutOverflow.hasDirectChildren(PlSqlKeyword.INCLUDING)).isTrue()
        assertThatAst(withoutOverflow.hasDirectChildren(DdlGrammar.INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE)).isFalse()

        val trailing = clause("$iot overflow parallel mapping table")
        assertThatAst(trailing.hasDirectChildren(DdlGrammar.INDEX_PARALLEL_CLAUSE)).isTrue()
        assertThatAst(trailing.hasDirectChildren(DdlGrammar.KEY_COMPRESSION)).isTrue()

        val outer = p.parse("$iot overflow annotations (a 'b') parallel")
        assertThatAst(outer.hasDirectChildren(DdlGrammar.INDEX_PARALLEL_CLAUSE)).isTrue()
        assertThatAst(outer.getFirstDescendant(DdlGrammar.INDEX_ORGANIZED_TABLE_CLAUSE)
            .hasDirectChildren(DdlGrammar.INDEX_PARALLEL_CLAUSE)).isFalse()
    }
}
