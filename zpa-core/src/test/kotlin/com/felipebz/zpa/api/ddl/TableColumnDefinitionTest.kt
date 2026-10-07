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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class TableColumnDefinitionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.TABLE_COLUMN_DEFINITION)
    }

    @Test
    fun matchesSimpleDefinition() {
        assertThat(p).matches("id number")
    }

    @Test
    fun matchesSort() {
        assertThat(p).matches("id number sort")
    }

    @Test
    fun matcheseDefault() {
        assertThat(p).matches("id number default 1")
    }

    @Test
    fun matchesSimpleEncrypt() {
        assertThat(p).matches("id number encrypt")
    }

    @Test
    fun matchesInlineConstraint() {
        assertThat(p).matches("id number constraint pktab primary key")
    }

    @Test
    fun matchesMultipleConstraints() {
        assertThat(p).matches("id number constraint pktab primary key check (id > 0)")
    }


    @Test
    fun matchesOrdinaryColumnVisibility() {
        listOf(
            "id number visible", "id number invisible", "id number invisible default 1", "id number invisible not null",
            "id number invisible constraint c not null", "id number invisible primary key", "id number invisible unique",
            "id varchar2(10) invisible default 'x' not null", "id number invisible default on null 1 not null check (id > 0)",
            "id number invisible annotations (a 'x')", "id number invisible encrypt",
            "id number invisible generated always as identity", "id number domain d invisible",
            "id number invisible sort", "id number sort invisible", "id json invisible",
            "id invisible", "id visible", "id invisible default 1", "id invisible not null",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
    }

    @Test
    fun rejectsOrdinaryColumnVisibilityOutsideItsPosition() {
        listOf(
            "id number default 1 invisible", "id number not null invisible", "id number null invisible",
            "id number primary key invisible", "id number encrypt invisible", "id number annotations (a 'x') invisible",
            "id number generated always as identity invisible", "id number invisible visible", "id number invisible invisible",
            "id number visible invisible", "id invisible number",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun keepsVirtualColumnsOutOfOrdinaryColumns() {
        listOf(
            "id number invisible as (a + 1)", "id number visible as (a + 1)", "id invisible as (a + 1)",
            "id number invisible generated always as (a + 1) virtual", "id number as (a + 1) invisible",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsVisibilityWithoutAWrapperNode() {
        val column = p.parse("id number invisible default 1")
        assertThatAst(column.children.map { it.name }).containsExactly("IDENTIFIER_NAME", "DATATYPE", "INVISIBLE", "DEFAULT", "LITERAL")
    }
}
