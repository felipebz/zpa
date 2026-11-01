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

class AlterSequenceTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_SEQUENCE)
    }

    @Test
    fun matchesDocumentedExamples() {
        assertThat(p).matches("alter sequence customers_seq maxvalue 1500;")
        assertThat(p).matches("alter sequence customers_seq cycle cache 5;")
    }

    @Test
    fun matchesHeaderForms() {
        assertThat(p).matches("alter sequence if exists app.customers_seq nocache")
        assertThat(p).matches("alter sequence app.customers_seq increment by -2")
    }

    @Test
    fun matchesSharedAndAlterOnlyOptions() {
        assertThat(p).matches("alter sequence s restart")
        assertThat(p).matches("alter sequence s restart start with 10")
        assertThat(p).matches("alter sequence s start with 10 restart")
        assertThat(p).matches("alter sequence s nomaxvalue minvalue 1 nocycle noorder")
        assertThat(p).matches("alter sequence s scale extend session")
        assertThat(p).matches("alter sequence s scale global keep")
        assertThat(p).matches("alter sequence s noscale nokeep order")
        assertThat(p).matches("alter sequence s shard")
        assertThat(p).matches("alter sequence s shard extend")
        assertThat(p).matches("alter sequence s shard noextend noshard")
        // Duplicate or conflicting options fail only after parsing (ORA-02280/ORA-02281/ORA-64601).
        assertThat(p).matches("alter sequence s cache 5 cache 6 cycle nocycle restart restart")
        // ORA-02283: START WITH without RESTART is a semantic error.
        assertThat(p).matches("alter sequence s start with 10")
    }

    @Test
    fun rejectsInvalidAlterSequenceForms() {
        // ORA-02286: at least one option; SHARING is not an ALTER option.
        assertThat(p).notMatches("alter sequence s")
        assertThat(p).notMatches("alter sequence s sharing = none")
        // ORA-11544
        assertThat(p).notMatches("alter sequence if not exists s cycle")
        // ORA-03048 / ORA-03049
        assertThat(p).notMatches("alter sequence s restart with 10")
        assertThat(p).notMatches("alter sequence s scale foo")
        assertThat(p).notMatches("alter sequence s shard foo")
        assertThat(p).matches("alter sequence s shard scale extend")
        assertThat(p).matches("alter sequence s scale shard noextend restart")
        assertThat(p).notMatches("alter sequence s shard extend scale extend")
        assertThat(p).notMatches("alter sequence s scale noextend shard extend")
        assertThat(p).notMatches("alter sequence s noshard extend")
        assertThat(p).notMatches("alter sequence s maxvalue")
        assertThat(p).notMatches("alter sequence s cache 1.5")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter sequence customers_seq maxvalue 1500;")
        assertThat(p).matches("create sequence if not exists email_seq;")
    }
}
