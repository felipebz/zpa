//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
