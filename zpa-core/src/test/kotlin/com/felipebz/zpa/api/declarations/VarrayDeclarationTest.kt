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

class VarrayDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.VARRAY_DECLARATION)
    }

    @Test
    fun matchesSimpleVarray() {
        assertThat(p).matches("type foo is varray(5) of number(2);")
    }

    @Test
    fun matchesSimpleVarrayNotNull() {
        assertThat(p).matches("type foo is varray(5) of number(2) not null;")
    }

    @Test
    fun matchesVaryingArray() {
        assertThat(p).matches("type foo is varying array(5) of number(2);")
    }

    @Test
    fun matchesArray() {
        assertThat(p).matches("type foo is array(5) of number(2);")
    }


    @Test
    fun matchesVarrayWithSizeGivenByAConstant() {
        assertThat(p).matches("type foo is varray(co_limit) of number;")
    }

    @Test
    fun doesNotMatchVarrayWithoutSize() {
        assertThat(p).notMatches("type foo is varray() of number;")
    }
}
