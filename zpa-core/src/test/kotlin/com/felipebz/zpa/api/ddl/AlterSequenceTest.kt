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
        assertThat(p).notMatches("alter sequence s shard")
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
