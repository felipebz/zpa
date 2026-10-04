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
package com.felipebz.zpa.api

import com.felipebz.flr.tests.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExecutablePragmaTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.BLOCK_STATEMENT)
    }

    @Test
    fun matchesDeprecateInTheExecutableSection() {
        listOf(
            "begin pragma deprecate(bar); null; end;", "begin null; pragma deprecate(bar); null; end;",
            "begin pragma deprecate(bar, 'use baz'); null; end;", "begin begin pragma deprecate(bar); null; end; end;",
            "begin null; pragma deprecate(bar); end;", "declare x number; begin pragma deprecate(x); x := 1; end;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "begin pragma deprecate(bar) null; end;", "begin pragma deprecate bar; null; end;", "begin pragma deprecate(); null; end;",
            "begin pragma deprecate(bar,); null; end;", "begin pragma deprecate(bar; null; end;",
            "begin pragma deprecate(bar, 1); null; end;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesSuppressesWarning6009InTheExecutableSection() {
        listOf(
            "begin pragma suppresses_warning_6009(bar); null; end;", "begin null; pragma suppresses_warning_6009(bar); null; end;",
            "begin begin pragma suppresses_warning_6009(bar); null; end; end;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "begin pragma suppresses_warning_6009(bar) null; end;", "begin pragma suppresses_warning_6009(); null; end;",
            "begin pragma suppresses_warning_6009('x'); null; end;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun reusesTheDeclarationPragmaRulesAndKeepsOtherPragmasOut() {
        val block = p.parse("begin pragma deprecate(bar, 'm'); pragma suppresses_warning_6009(bar); null; end;")
        assertThatAst(block.getDescendants(PlSqlGrammar.DEPRECATE_PRAGMA)).hasSize(1)
        assertThatAst(block.getDescendants(PlSqlGrammar.SUPPRESSES_WARNING_6009_PRAGMA)).hasSize(1)
        assertThatAst(block.getDescendants(PlSqlGrammar.PRAGMA_DECLARATION)).isEmpty()
        listOf(
            "begin pragma exception_init(e, -1); null; end;", "begin pragma serially_reusable; null; end;",
            "begin pragma restrict_references(f, wnds); null; end;", "begin pragma udf; null; end;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
        listOf(
            "declare pragma deprecate(bar); begin null; end;", "declare pragma suppresses_warning_6009(bar); begin null; end;",
            "begin pragma inline(f, 'yes'); null; end;", "begin pragma autonomous_transaction; null; end;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        val declaration = p.parse("declare pragma deprecate(bar); begin null; end;")
        assertThatAst(declaration.getDescendants(PlSqlGrammar.PRAGMA_DECLARATION)).hasSize(1)
    }
}
