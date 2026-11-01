//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CallSpecificationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CALL_SPECIFICATION)
    }

    @Test
    fun matchesJavaCallSpec() {
        assertThat(p).matches("language java name 'foo';")
    }

    @Test
    fun matchesLanguageCNameLibrary() {
        assertThat(p).matches("language c name \"foo\" library bar;")
    }

    @Test
    fun matchesLanguageCLibraryName() {
        assertThat(p).matches("language c library bar name \"foo\";")
    }

    @Test
    fun matchesExternal() {
        assertThat(p).matches("external name \"foo\" library bar;")
    }

    @Test
    fun matchesAgentIn() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent);")
    }

    @Test
    fun matchesAgentInMultiple() {
        assertThat(p).matches("language c library bar name \"foo\" agent in (agent, agent2);")
    }

    @Test
    fun matchesWithContext() {
        assertThat(p).matches("language c library bar name \"foo\" with context;")
    }

    @Test
    fun matchesParameterContext() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (context);")
    }

    @Test
    fun matchesParameterSelf() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (self);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self tdo);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator struct);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self indicator tdo);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self length);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self duration);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self maxlen);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetid);")
        assertThat(p).matches("language c library bar name \"foo\" parameters (self charsetform);")
    }

    @Test
    fun matchesParameterSimple() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int);")
    }

    @Test
    fun matchesMultipleParameters() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x int, y int);")
    }

    @Test
    fun matchesParameterReturn() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (return int);")
    }

    @Test
    fun matchesParameterWithProperty() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x length int);")
    }

    @Test
    fun matchesParameterByReference() {
        assertThat(p).matches("language c library bar name \"foo\" parameters (x by reference int);")
    }

}
