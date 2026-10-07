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
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterAssertionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_ASSERTION)
    }

    private fun assertMatches(vararg statements: String) =
        statements.forEach { assertThat(p).describedAs(it).matches(it) }

    private fun assertNotMatches(vararg statements: String) =
        statements.forEach { assertThat(p).describedAs(it).notMatches(it) }

    @Test
    fun matchesEachState() {
        assertMatches(
            "alter assertion a enable;", "alter assertion a disable;", "alter assertion a validate;",
            "alter assertion a novalidate;", "alter assertion a initially immediate;", "alter assertion a initially deferred;",
        )
    }

    @Test
    fun matchesStatesInAnyOrderOncePerPair() {
        assertMatches(
            "alter assertion a enable validate;", "alter assertion a enable novalidate;", "alter assertion a disable novalidate;",
            "alter assertion a disable validate;", "alter assertion a validate enable;", "alter assertion a novalidate enable;",
            "alter assertion a novalidate disable;", "alter assertion a enable initially deferred;",
            "alter assertion a initially deferred enable;", "alter assertion a enable validate initially immediate",
            "alter assertion a enable initially immediate validate", "alter assertion a initially deferred novalidate disable",
        )
        assertNotMatches(
            "alter assertion a enable enable;", "alter assertion a enable disable;", "alter assertion a disable enable;",
            "alter assertion a validate validate;", "alter assertion a validate novalidate;",
            "alter assertion a enable validate enable;", "alter assertion a novalidate enable validate;",
            "alter assertion a initially deferred initially immediate;", "alter assertion a initially deferred initially deferred;",
            "alter assertion a enable validate initially immediate enable;",
        )
    }

    @Test
    fun matchesStatelessAlterAndIfExists() {
        assertMatches(
            "alter assertion a;", "alter assertion if exists a;", "alter assertion if exists a enable;",
            "alter assertion if exists hr.a disable novalidate;", "alter assertion hr.a enable;", "alter assertion \"Hr\".\"a\" enable",
        )
    }

    @Test
    fun matchesDocumentationExamples() {
        assertMatches(
            "ALTER ASSERTION IF EXISTS company_must_have_a_president DISABLE;",
            "ALTER ASSERTION staff_earn_less_than_manager VALIDATE;",
            "ALTER ASSERTION hr.salary_within_job_limit ENABLE NOVALIDATE;",
        )
    }

    @Test
    fun rejectsCreateOnlyAndUnknownClauses() {
        assertNotMatches(
            "alter assertion a deferrable;", "alter assertion a not deferrable;", "alter assertion a enable deferrable;",
            "alter assertion a not;", "alter assertion a initially;", "alter assertion a initially foo;",
            "alter assertion a enable rely;", "alter assertion a norely;", "alter assertion a rename to b;",
            "alter assertion a enable, disable;", "alter assertion if not exists a enable;", "alter assertion if a enable;",
        )
    }

    @Test
    fun rejectsMalformedNames() {
        assertNotMatches(
            "alter assertion;", "alter assertion if exists;", "alter assertion a.b.c enable;", "alter assertion .a enable;",
        )
    }

    @Test
    fun buildsStatementWithoutWrapperNodes() {
        val tree = p.parse("alter assertion if exists hr.a enable novalidate;")
        assertThatAst(tree.children.map { it.name }).containsExactly(
            "ALTER", "ASSERTION", "IF", "EXISTS", "IDENTIFIER_NAME", "DOT", "IDENTIFIER_NAME", "ENABLE", "NOVALIDATE", "SEMICOLON")
        assertThatAst(tree.children.filter { it.name == "IDENTIFIER_NAME" }.map { it.tokenOriginalValue }).containsExactly("hr", "a")
    }
}
