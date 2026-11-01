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

class CreateContextTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_CONTEXT)
    }

    @Test
    fun matchesCreateContext() {
        assertThat(p).matches("create or replace context xx_ctx using apps.xx_pkg;")
        assertThat(p).matches("create context xx_ctx using xx_pkg initialized externally;")
        assertThat(p).matches("create context xx_ctx using xx_pkg accessed globally;")
    }

    @Test
    fun doesNotMatchCreateContextWithoutAPackage() {
        assertThat(p).notMatches("create context xx_ctx;")
    }
}
