//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class InmemoryJoinGroupTest : RuleTest() {

    @Test
    fun matchesCreateMembersAndHeaders() {
        setRootRule(DdlGrammar.CREATE_INMEMORY_JOIN_GROUP)
        assertThat(p).matches("create inmemory join group jg (t1(c))")
        assertThat(p).matches("create inmemory join group jg (t1(c), t2(c), t3(c));")
        assertThat(p).matches("create inmemory join group if not exists app.jg " +
            "(app.t1(c), other.t2(c));")
    }

    @Test
    fun rejectsInvalidCreateStructure() {
        setRootRule(DdlGrammar.CREATE_INMEMORY_JOIN_GROUP)
        assertThat(p).notMatches("create inmemory join group jg ()")
        assertThat(p).notMatches("create inmemory join group jg (t1())")
        assertThat(p).notMatches("create inmemory join group jg (t1(c, d), t2(c))")
        assertThat(p).notMatches("create inmemory join group jg (t1(c),)")
        assertThat(p).notMatches("create inmemory join group jg (app.t1.extra(c))")
        assertThat(p).notMatches("create inmemory join group if exists jg (t1(c))")
        assertThat(p).notMatches("create or replace inmemory join group jg (t1(c))")
        assertThat(p).notMatches("create inmemory join group jg if not exists (t1(c))")
    }

    @Test
    fun matchesAlterActionsAndMembers() {
        setRootRule(DdlGrammar.ALTER_INMEMORY_JOIN_GROUP)
        assertThat(p).matches("alter inmemory join group jg add (t1(c));")
        assertThat(p).matches("alter inmemory join group jg remove (t1(c), t2(c))")
        assertThat(p).matches("alter inmemory join group if exists app.jg add (other.t1(c))")
    }

    @Test
    fun rejectsInvalidAlterStructure() {
        setRootRule(DdlGrammar.ALTER_INMEMORY_JOIN_GROUP)
        assertThat(p).notMatches("alter inmemory join group jg")
        assertThat(p).notMatches("alter inmemory join group jg add ()")
        assertThat(p).notMatches("alter inmemory join group jg remove (t1())")
        assertThat(p).notMatches("alter inmemory join group jg add (t1(c, d))")
        assertThat(p).notMatches("alter inmemory join group jg add (t1(c)) remove (t2(c))")
        assertThat(p).notMatches("alter inmemory join group if not exists jg add (t1(c))")
    }

    @Test
    fun routesBothStatementsThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create inmemory join group jg (t1(c), t2(c));")
        assertThat(p).matches("alter inmemory join group jg add (t3(c));")
    }
}
