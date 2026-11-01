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

class TruncateTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.TRUNCATE_TABLE)
    }

    @Test
    fun matchesSimpleTruncate() {
        assertThat(p).matches("truncate table foo;")
    }

    @Test
    fun matchesTruncatePreserveMaterializedViewLog() {
        assertThat(p).matches("truncate table foo preserve materialized view log;")
    }

    @Test
    fun matchesTruncatePurgeMaterializedViewLog() {
        assertThat(p).matches("truncate table foo purge materialized view log;")
    }

    @Test
    fun matchesTruncateDropStorage() {
        assertThat(p).matches("truncate table foo drop storage;")
        assertThat(p).matches("truncate table foo drop all storage;")
    }

    @Test
    fun matchesTruncateReuseStorage() {
        assertThat(p).matches("truncate table foo reuse storage;")
    }

    @Test
    fun matchesTruncateCascade() {
        assertThat(p).matches("truncate table foo cascade;")
    }

    @Test
    fun matchesLongTruncate() {
        assertThat(p).matches("truncate table sch.foo  purge materialized view log drop all storage cascade;")
    }

}
