/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest

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
