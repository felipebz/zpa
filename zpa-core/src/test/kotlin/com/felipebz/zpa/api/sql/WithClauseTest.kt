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

class WithClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.WITH_CLAUSE)
    }

    @Test
    fun matchesSimpleWith() {
        assertThat(p).matches("with q as (select 1 from dual)")
    }

    @Test
    fun matchesMultipleSubqueries() {
        assertThat(p).matches("with q as (select 1 from dual), q2 as (select 1 from dual)")
    }

    @Test
    fun matchesRecursiveSimple() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual)")
    }

    @Test
    fun matchesRecursiveWithSearch() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual) search depth first by a set order1")
    }

    @Test
    fun matchesRecursiveWithSearchAndCycle() {
        assertThat(p).matches("with q(id, parent) as (select 1 from dual) search depth first by a set order1 cycle id set cycle to 1 default 0")
    }

    @Test
    fun matchesWithValues() {
        assertThat(p).matches("with q(a, b) as (values (1, 'foo'), (2, 'bar')) as t(a, b)")
    }

    @Test
    fun matchesFunctionDeclaration() {
        assertThat(p).matches("with function func return number is begin return 1; end;")
    }

    @Test
    fun matchesProcedureDeclaration() {
        assertThat(p).matches("with procedure proc is begin null; end;")
    }

    @Test
    fun matchesFunctionAndProcedureDeclaration() {
        assertThat(p).matches("with function func return number is begin return 1; end; " +
            "procedure proc is begin null; end;")
    }

    @Test
    fun matchesFunctionDeclarationAndQuery() {
        assertThat(p).matches("with function func return number is begin return 1; end; " +
            "q as (select 1 from dual)")
    }

}
