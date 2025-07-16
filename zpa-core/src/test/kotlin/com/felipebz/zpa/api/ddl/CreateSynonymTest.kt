/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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

class CreateSynonymTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_SYNONYM)
    }

    @Test
    fun matchesSimpleCreateSynonym() {
        assertThat(p).matches("create synonym foo for bar;")
    }

    @Test
    fun matchesCreatePublicSynonym() {
        assertThat(p).matches("create public synonym foo for bar;")
    }

    @Test
    fun matchesEditionableSynonym() {
        assertThat(p).matches("create editionable synonym foo for bar;")
    }

    @Test
    fun matchesNonEditionableSynonym() {
        assertThat(p).matches("create noneditionable synonym foo for bar;")
    }

    @Test
    fun matchesSynonymWithSharingMetada() {
        assertThat(p).matches("create synonym foo sharing = metadata for bar;")
    }

    @Test
    fun matchesSynonymWithSharingNone() {
        assertThat(p).matches("create synonym foo sharing = none for bar;")
    }

    @Test
    fun matchesCreateorReplaceSynonym() {
        assertThat(p).matches("create or replace synonym foo for bar;")
    }

    @Test
    fun matchesLongSynonym() {
        assertThat(p).matches("create or replace public synonym sch.foo for sch.bar@link;")
        assertThat(p).matches("create or replace public synonym sch.foo for sch.bar@link.domain.com;")
    }

}
