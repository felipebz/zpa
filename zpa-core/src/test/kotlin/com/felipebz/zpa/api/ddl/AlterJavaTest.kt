//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class AlterJavaTest : RuleTest() {

    @Test
    fun matchesActions() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        assertThat(p).matches("alter java class \"Agent\" resolver ((\"/usr/bin/bfile_dir/*\" pm)(* public)) resolve;")
        assertThat(p).matches("alter java class \"Agent\" resolve")
        assertThat(p).matches("alter java source \"Agent\" compile")
        // SOURCE/CLASS compatibility with COMPILE/RESOLVE is checked after parsing.
        assertThat(p).matches("alter java class \"Agent\" compile")
        assertThat(p).matches("alter java source \"Agent\" resolve")
        assertThat(p).matches("alter java class \"Agent\" authid current_user")
        assertThat(p).matches("alter java source \"Agent\" authid definer")
    }

    @Test
    fun matchesResolverWithEachAction() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        assertThat(p).matches("alter java class \"Agent\" resolver ((* public)) compile")
        assertThat(p).matches("alter java class \"Agent\" resolver ((* public)) authid definer")
        assertThat(p).matches("alter java class \"Agent\" resolver ((* public) (\"x/*\" -)) resolve")
        assertThat(p).matches("alter java class \"Agent\" resolver ((* , public)(\"x/*\", -)) resolve")
        assertThat(p).matches("alter java class \"Agent\" resolver ((* zpa_probe)(* \"ZPA_PROBE\")) resolve")
        assertThat(p).matches("alter java class \"Agent\" resolver () resolve;")
        assertThat(p).matches("alter java source \"Agent\" resolver () compile;")
    }

    @Test
    fun matchesNamesAndIfExists() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        assertThat(p).matches("alter java class if exists \"Agent\" resolve")
        assertThat(p).matches("alter java source if exists nx_src compile")
        assertThat(p).matches("alter java class zpa_probe.\"Agent\" resolve")
        assertThat(p).matches("alter java class \"ZPA_PROBE\".\"Agent\" resolve")
        assertThat(p).matches("alter java class \"a/b/Agent\" resolve")
    }

    @Test
    fun rejectsInvalidHeader() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        assertThat(p).notMatches("alter java resource \"Agent\" resolve")
        assertThat(p).notMatches("alter java \"Agent\" resolve")
        // IF EXISTS follows SOURCE/CLASS, not JAVA (ORA-02000).
        assertThat(p).notMatches("alter java if exists class \"Agent\" resolve")
        assertThat(p).notMatches("alter java class if not exists \"Agent\" resolve")
        assertThat(p).notMatches("alter java class \"Agent\" if exists resolve")
        assertThat(p).notMatches("alter java class a.b.c resolve")
        assertThat(p).notMatches("alter java class \"Agent\"@dbl resolve")
        assertThat(p).notMatches("alter java class resolve")
        assertThat(p).notMatches("alter or replace java class \"Agent\" resolve")
        assertThat(p).notMatches("alter java class \"Agent\" sharing = none resolve")
    }

    @Test
    fun rejectsInvalidActions() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        assertThat(p).notMatches("alter java class \"Agent\"")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* public))")
        assertThat(p).notMatches("alter java class \"Agent\" resolve resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolve compile")
        assertThat(p).notMatches("alter java class \"Agent\" compile resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolve authid definer")
        assertThat(p).notMatches("alter java class \"Agent\" authid definer resolve")
        assertThat(p).notMatches("alter java class \"Agent\" authid definer authid current_user")
        assertThat(p).notMatches("alter java class \"Agent\" authid definer resolver ((* public))")
        assertThat(p).notMatches("alter java class \"Agent\" resolve resolver ((* public))")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* public)) resolver ((* public)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolve noforce")
        assertThat(p).notMatches("alter java class \"Agent\" compile debug")
    }

    @Test
    fun rejectsMalformedResolver() {
        setRootRule(DdlGrammar.ALTER_JAVA)
        // ORA-29512: incorrectly formed name resolver specification.
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* public), (\"x/*\" -)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* public),) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((*)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver (* public) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* a.b)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((* public public)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver ((x/y/* public)) resolve")
        assertThat(p).notMatches("alter java class \"Agent\" resolver (('x/*' public)) resolve")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter java class \"Agent\" resolver ((\"/usr/bin/bfile_dir/*\" pm)(* public)) resolve;")
        assertThat(p).matches("alter java source if exists \"Agent\" authid definer;")
    }
}
