//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CursorDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CURSOR_DECLARATION)
    }

    @Test
    fun matchesSimpleCursor() {
        assertThat(p).matches("cursor cur is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithParameter() {
        assertThat(p).matches("cursor cur(x number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithMultipleParameters() {
        assertThat(p).matches("cursor cur(x number, y number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithExplicitInParameter() {
        assertThat(p).matches("cursor cur(x in number) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithDefaultParameter() {
        assertThat(p).matches("cursor cur(x number default 1) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithDefaultParameterAlternative() {
        assertThat(p).matches("cursor cur(x number := 1) is select 1 from dual;")
    }

    @Test
    fun matchesCursorWithReturnType() {
        assertThat(p).matches("cursor cur return my_type is select 1 from dual;")
    }

    @Test
    fun matchesCursorSpecification() {
        assertThat(p).matches("cursor cur return my_type;")
    }

}
