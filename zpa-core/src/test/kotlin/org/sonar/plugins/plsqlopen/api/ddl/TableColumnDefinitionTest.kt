/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.ddl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import com.felipebz.flr.tests.Assertions.assertThat

class TableColumnDefinitionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.TABLE_COLUMN_DEFINITION)
    }

    @Test
    fun matchesSimpleDefinition() {
        assertThat(p).matches("id number")
    }

    @Test
    fun matchesSort() {
        assertThat(p).matches("id number sort")
    }

    @Test
    fun matcheseDefault() {
        assertThat(p).matches("id number default 1")
    }

    @Test
    fun matchesSimpleEncrypt() {
        assertThat(p).matches("id number encrypt")
    }

    @Test
    @Disabled
    fun matchesEncryptWithSpecification() {
        assertThat(p).matches("id number encrypt using 'AES256'")
    }

    @Test
    fun matchesInlineConstraint() {
        assertThat(p).matches("id number constraint pktab primary key")
    }

    @Test
    fun matchesMultipleConstraints() {
        assertThat(p).matches("id number constraint pktab primary key check (id > 0)")
    }

    @Test
    @Disabled
    fun matchesInlineRefConstraint() {
        assertThat(p).matches("id number with rowid")
    }

}
