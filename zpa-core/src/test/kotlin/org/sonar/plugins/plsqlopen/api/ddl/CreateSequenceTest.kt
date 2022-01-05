/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.DdlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class CreateSequenceTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_SEQUENCE)
    }

    @Test
    fun matchesSimpleCreateSequence() {
        assertThat(p).matches("create sequence seq_name;")
    }

    @Test
    fun matchesCreateSequenceStart() {
        assertThat(p).matches("create sequence seq_name start with 1;")
    }

    @Test
    fun matchesCreateSequenceIncrement() {
        assertThat(p).matches("create sequence seq_name increment by 1;")
    }

    @Test
    fun matchesCreateSequenceStartIncrement() {
        assertThat(p).matches("create sequence seq_name start with 1 increment by 1;")
    }

    @Test
    fun matchesCreateSequenceCache() {
        assertThat(p).matches("create sequence seq_name cache 10;")
    }

    @Test
    fun matchesCreateSequenceNoCache() {
        assertThat(p).matches("create sequence seq_name nocache;")
    }

    @Test
    fun matchesCreateSequenceXXX() {
        assertThat(p).matches("create sequence seq_name start with 1 MAXVALUE 9999 MINVALUE 1 NOCYCLE CACHE 20 ORDER;")
    }
}
