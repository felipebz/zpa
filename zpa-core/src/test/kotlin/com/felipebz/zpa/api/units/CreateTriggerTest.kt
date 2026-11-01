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
                + "after startup or before logoff or after db_role_change "
                + "on pluggable database "
                + "begin null; end;")
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
