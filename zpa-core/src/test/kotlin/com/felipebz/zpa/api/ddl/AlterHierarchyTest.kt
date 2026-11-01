//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class AlterHierarchyTest : RuleTest() {

    @Test
    fun matchesActions() {
        setRootRule(DdlGrammar.ALTER_HIERARCHY)
        assertThat(p).matches("alter hierarchy product_hier rename to myproduct_hier;")
        assertThat(p).matches("alter hierarchy h compile")
        assertThat(p).matches("alter hierarchy h rename to \"New Hierarchy\"")
    }

    @Test
    fun matchesNamesAndIfExists() {
        setRootRule(DdlGrammar.ALTER_HIERARCHY)
        assertThat(p).matches("alter hierarchy if exists h compile")
        assertThat(p).matches("alter hierarchy if exists s.h compile")
        assertThat(p).matches("alter hierarchy if exists h rename to h2")
        assertThat(p).matches("alter hierarchy s.h rename to h2")
        assertThat(p).matches("alter hierarchy \"My Hier\" compile")
        assertThat(p).matches("alter hierarchy \"ZPA_PROBE\".\"My Hier\" compile")
    }

    @Test
    fun rejectsInvalidNames() {
        setRootRule(DdlGrammar.ALTER_HIERARCHY)
        assertThat(p).notMatches("alter hierarchy a.b.c compile")
        assertThat(p).notMatches("alter hierarchy h@dbl compile")
        assertThat(p).notMatches("alter hierarchy compile")
        // The rename target is a single identifier (ORA-03048).
        assertThat(p).notMatches("alter hierarchy h rename to s.h2")
        assertThat(p).notMatches("alter hierarchy h rename to a.b.h2")
        assertThat(p).notMatches("alter hierarchy h rename to h2@dbl")
        assertThat(p).notMatches("alter hierarchy h rename to h2, h3")
    }

    @Test
    fun rejectsInvalidIfExists() {
        setRootRule(DdlGrammar.ALTER_HIERARCHY)
        assertThat(p).notMatches("alter hierarchy if not exists h compile")
        assertThat(p).notMatches("alter hierarchy h if exists compile")
        assertThat(p).notMatches("alter if exists hierarchy h compile")
        assertThat(p).notMatches("alter or replace hierarchy h compile")
    }

    @Test
    fun rejectsMissingOrExtraActions() {
        setRootRule(DdlGrammar.ALTER_HIERARCHY)
        assertThat(p).notMatches("alter hierarchy h")
        assertThat(p).notMatches("alter hierarchy h rename")
        assertThat(p).notMatches("alter hierarchy h rename to")
        assertThat(p).notMatches("alter hierarchy h rename h2")
        assertThat(p).notMatches("alter hierarchy h compile compile")
        assertThat(p).notMatches("alter hierarchy h rename to h2 compile")
        assertThat(p).notMatches("alter hierarchy h compile rename to h2")
        assertThat(p).notMatches("alter hierarchy h compile debug")
        assertThat(p).notMatches("alter hierarchy h compile reuse settings")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter hierarchy product_hier rename to myproduct_hier;")
        assertThat(p).matches("alter hierarchy if exists s.h compile;")
    }
}
