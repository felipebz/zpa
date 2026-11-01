//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DropDirectoryTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.DROP_DIRECTORY)
    }

    @Test
    fun matchesSimpleDropDirectory() {
        assertThat(p).matches("drop directory foo;")
    }

    @Test
    fun matchesDropDirectoryIfExists() {
        assertThat(p).matches("drop directory if exists foo;")
    }

}
