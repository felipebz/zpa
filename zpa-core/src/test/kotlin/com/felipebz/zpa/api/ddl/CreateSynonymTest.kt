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
