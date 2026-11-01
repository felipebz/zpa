//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.session

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SessionControlGrammar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionControlGrammarTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(SessionControlGrammar.SESSION_CONTROL_COMMAND)
    }

    @Test
    fun matchesAlterSession() {
        assertThat(p).matches("alter session set nls_date_format = 'dd/mm/yyyy';")
    }

    @Test
    fun matchesSetRole() {
        assertThat(p).matches("set role foo;")
        assertThat(p).matches("set role foo, bar, baz;")
    }

    @Test
    fun matchesSetRoleNone() {
        assertThat(p).matches("set role none;")
    }

    @Test
    fun matchesSetRoleAll() {
        assertThat(p).matches("set role all;")
    }

    @Test
    fun matchesSetRoleAllExcept() {
        assertThat(p).matches("set role all except foo;")
        assertThat(p).matches("set role all except foo, bar, baz;")
    }

    @Test
    fun matchesSetRoleWithPassword() {
        assertThat(p).matches("set role foo identified by pass;")
        assertThat(p).matches("set role foo identified by pass, bar, baz;")
    }

}
