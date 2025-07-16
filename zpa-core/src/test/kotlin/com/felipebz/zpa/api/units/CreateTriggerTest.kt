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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

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
}
