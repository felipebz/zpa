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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest

class IntoClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.INTO_CLAUSE)
    }

    @Test
    fun matchesSimpleIntoClause() {
        assertThat(p).matches("into var")
    }

    @Test
    fun matchesIntoClauseInMultipleVariables() {
        assertThat(p).matches("into var, var2")
    }

    @Test
    fun matchesBulkCollect() {
        assertThat(p).matches("bulk collect into var")
    }

    @Test
    fun matchesSimpleCollectionElement() {
        assertThat(p).matches("into col(1)")
    }

    @Test
    fun matchesSimpleIntoRecordItem() {
        assertThat(p).matches("into col.it")
    }

    @Test
    fun matchesSimpleIntoItemInRecordCollection() {
        assertThat(p).matches("into col(1).it")
    }

}
