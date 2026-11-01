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

class TableOfDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.TABLE_OF_DECLARATION)
    }

    @Test
    fun matchesSimpleDeclaration() {
        assertThat(p).matches("type foo is table of bar;")
    }

    @Test
    fun matchesSimpleDeclarationNotNull() {
        assertThat(p).matches("type foo is table of bar not null;")
    }

    @Test
    fun matchesIndexedDeclaration() {
        assertThat(p).matches("type foo is table of bar index by binary_integer;")
    }

    @Test
    fun matchesDeclarationWithBuiltinDatatypes() {
        assertThat(p).matches("type foo is table of varchar2(10) index by number(5);")
    }

}
