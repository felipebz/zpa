//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateTriggerTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_TRIGGER)
    }

    @Test
    fun matchesSimpleTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before insert on tab "
                + "begin null; end;")
    }

    @Test
    fun matchesSimpleCreateOrReplaceTrigger() {
        assertThat(p).matches(""
                + "create or replace trigger foo "
                + "before insert on tab "
                + "begin null; end;")
    }

    @Test
    fun matchesEditionableTrigger() {
        assertThat(p).matches(""
                + "create editionable trigger foo "
                + "before insert on tab "
                + "begin null; end;")
    }

    @Test
    fun matchesNoneditionableTrigger() {
        assertThat(p).matches(""
                + "create noneditionable trigger foo "
                + "before insert on tab "
                + "begin null; end;")
    }

    @Test
    fun matchesTriggerWithDeclareWithoutDeclarations() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before insert on tab "
                + "declare "
                + "begin null; end;")
    }

    @Test
    fun matchesSystemDdlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before alter or analyze or associate statistics or audit or comment or create "
                + "on foo.schema "
                + "begin null; end;")
    }

    @Test
    fun matchesSystemDatabaseTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "after startup or db_role_change or set container "
                + "on pluggable database "
                + "begin null; end;")
    }

    @Test
    fun matchesSystemTriggerWhoseEventsShareOneTiming() {
        assertThat(p).matches("create or replace trigger trg_b after logon or create on schema begin null; end;")
        assertThat(p).matches("create or replace trigger trg_d after servererror or logon on database begin null; end;")
        assertThat(p).matches("create or replace trigger trg_e before create or logoff on schema begin null; end;")
        assertThat(p).matches("create trigger trg_f after create or logon or alter on schema begin null; end;")
        assertThat(p).matches("create trigger trg_g before logoff or truncate or drop on schema begin null; end;")
        assertThat(p).matches("create trigger trg_h before shutdown or logoff on database begin null; end;")
        assertThat(p).matches("create trigger trg_i before unplug or logoff on pluggable database begin null; end;")
        assertThat(p).matches("create trigger trg_j after clone or logon on pluggable database begin null; end;")
        assertThat(p).matches("create trigger trg_k instead of create or logon on schema begin null; end;")
    }

    @Test
    fun doesNotMatchASystemTriggerWhoseTimingDoesNotFit() {
        // ORA-30509: a logoff trigger is BEFORE only; ORA-30501: a shutdown trigger too.
        assertThat(p).notMatches("create or replace trigger trg_a after logon or logoff on database begin null; end;")
        assertThat(p).notMatches("create or replace trigger trg_c after startup or shutdown on database begin null; end;")
        // ORA-30508: a logon trigger is AFTER only.
        assertThat(p).notMatches("create trigger trg_l before logon on schema begin null; end;")
        // ORA-04072: the timing is written once.
        assertThat(p).notMatches("create trigger trg_m after logon or after create on schema begin null; end;")
        assertThat(p).notMatches("create trigger trg_n after startup or before logoff on database begin null; end;")
    }

    @Test
    fun matchesInsteadOfDmlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "instead of insert or update on foo.bar "
                + "for each row enable "
                + "begin null; end;")
    }

    @Test
    fun matchesCompoundDmlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "for insert or update of bar on foo.bar "
                + "compound trigger "
                + "before each row is "
                + "begin null; end before each row;"
                + "after each row is "
                + "begin null; end after each row;"
                + "after statement is "
                + "begin null; end after statement;"
                + "end foo;")
    }

    @Test
    fun matchesCompoundTriggerWithDeclarationsInTimingSection() {
        assertThat(p).matches("create trigger trg for insert on tab compound trigger " +
                "g number; " +
                "before each row is l_data pkg.r_type; begin null; end before each row; " +
                "end;")
    }

    @Test
    fun doesNotMatchATimingSectionWithoutBegin() {
        assertThat(p).notMatches("create trigger trg for insert on tab compound trigger " +
                "before each row is l_data pkg.r_type; end before each row; end trg;")
    }
}
