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

class AlterSynonymTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_SYNONYM)
    }

    @Test
    fun matchesPrivateAndPublicSynonymActions() {
        assertThat(p).matches("alter synonym offices compile;")
        assertThat(p).matches("alter synonym offices noneditionable;")
        assertThat(p).matches("alter synonym app.offices editionable")
        assertThat(p).matches("alter synonym if exists app.offices compile")
        assertThat(p).matches("alter public synonym emp_table compile;")
        assertThat(p).matches("alter public synonym emp noneditionable")
        assertThat(p).matches("alter public synonym if exists emp editionable")
    }

    @Test
    fun rejectsInvalidAlterSynonymForms() {
        // ORA-00922: an action is required; public synonyms and private names take at most one qualifier.
        assertThat(p).notMatches("alter synonym offices")
        assertThat(p).notMatches("alter public synonym sch.emp editionable")
        assertThat(p).notMatches("alter synonym a.b.offices compile")
        assertThat(p).notMatches("alter synonym offices@remote compile")
        assertThat(p).notMatches("alter synonym offices recompile")
        // ORA-03049: exactly one action.
        assertThat(p).notMatches("alter synonym offices compile editionable")
        assertThat(p).notMatches("alter synonym offices editionable compile")
        assertThat(p).notMatches("alter synonym offices compile compile")
        assertThat(p).notMatches("alter synonym offices compile reuse settings")
        // ORA-11544 / ORA-00995 / ORA-00940
        assertThat(p).notMatches("alter synonym if not exists offices compile")
        assertThat(p).notMatches("alter synonym public offices compile")
        assertThat(p).notMatches("alter or replace synonym offices compile")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter synonym offices compile;")
        assertThat(p).matches("alter public synonym emp_table compile;")
    }
}
