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
import org.junit.jupiter.api.Test

class CreateEditionTest : RuleTest() {

    @Test
    fun matchesCreateEdition() {
        setRootRule(DdlGrammar.CREATE_EDITION)
        assertThat(p).matches("create edition test_ed;")
        assertThat(p).matches("create edition test_ed")
        assertThat(p).matches("create edition \"Test Ed\"")
        assertThat(p).matches("create edition if not exists test_ed")
        assertThat(p).matches("create edition test_ed as child of ora\$base")
        assertThat(p).matches("create edition test_ed as child of \"Parent Ed\"")
        assertThat(p).matches("create edition if not exists test_ed as child of parent_ed;")
    }

    @Test
    fun rejectsQualifiedNames() {
        setRootRule(DdlGrammar.CREATE_EDITION)
        assertThat(p).notMatches("create edition s.test_ed")
        assertThat(p).notMatches("create edition a.b.c")
        assertThat(p).notMatches("create edition test_ed@dbl")
        assertThat(p).notMatches("create edition test_ed as child of s.p")
        assertThat(p).notMatches("create edition test_ed as child of a.b.c")
        assertThat(p).notMatches("create edition test_ed as child of p@dbl")
        assertThat(p).notMatches("create edition test_ed, test_ed2")
    }

    @Test
    fun rejectsMalformedClauses() {
        setRootRule(DdlGrammar.CREATE_EDITION)
        assertThat(p).notMatches("create edition")
        assertThat(p).notMatches("create edition as child of p")
        assertThat(p).notMatches("create edition test_ed as child of")
        assertThat(p).notMatches("create edition test_ed as child")
        assertThat(p).notMatches("create edition test_ed as of p")
        assertThat(p).notMatches("create edition test_ed child of p")
        assertThat(p).notMatches("create edition test_ed as child of p as child of q")
        assertThat(p).notMatches("create edition test_ed garbage")
    }

    @Test
    fun rejectsUnsupportedModifiers() {
        setRootRule(DdlGrammar.CREATE_EDITION)
        assertThat(p).notMatches("create edition if exists test_ed")
        assertThat(p).notMatches("create edition if not test_ed")
        assertThat(p).notMatches("create edition test_ed if not exists")
        assertThat(p).notMatches("create if not exists edition test_ed")
        assertThat(p).notMatches("create or replace edition test_ed")
        assertThat(p).notMatches("create editionable edition test_ed")
        assertThat(p).notMatches("create noneditionable edition test_ed")
        assertThat(p).notMatches("create edition test_ed editionable")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create edition test_ed;")
        assertThat(p).matches("create edition if not exists test_ed as child of parent_ed;")
    }
}
