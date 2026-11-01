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

class CreateDirectoryTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_DIRECTORY)
    }

    @Test
    fun matchesSimpleCreateDirectory() {
        assertThat(p).matches("create directory foo as 'path';")
    }

    @Test
    fun matchesSimpleCreateOrReplaceDirectory() {
        assertThat(p).matches("create or replace directory foo as 'path';")
    }

    @Test
    fun matchesCreateIfNotExistsDirectory() {
        assertThat(p).matches("create directory if not exists foo as 'path';")
    }

    @Test
    fun matchesLongCreateDirectory() {
        assertThat(p).matches("create directory if not exists foo sharing = metadata as 'path';")
    }

}
