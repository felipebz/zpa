//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.UPDATE_STATEMENT)
    }

    @Test
    fun matchesSimpleUpdate() {
        assertThat(p).matches("update tab set x = 1;")
    }

    @Test
    fun matchesUpdateWithWhere() {
        assertThat(p).matches("update tab set x = 1 where y = 1;")
    }

    @Test
    fun matchesUpdateWithWhereCurrentOf() {
        assertThat(p).matches("update tab set x = 1 where current of cur;")
    }

    @Test
    fun matchesUpdateMultipleColumns() {
        assertThat(p).matches("update tab set x = 1, y = 1;")
    }

    @Test
    fun matchesUpdateWithAlias() {
        assertThat(p).matches("update tab t set t.x = 1;")
    }

    @Test
    fun matchesUpdateWithSchema() {
        assertThat(p).matches("update sch.tab set sch.tab.x = 1;")
    }

    @Test
    fun matchesLabeledUpdate() {
        assertThat(p).matches("<<foo>> update tab set x = 1;")
    }

    @Test
    fun matchesUpdateWithReturningInto() {
        assertThat(p).matches("update tab set x = 1 returning x into y;")
    }

    @Test
    fun matchesUpdateDefaultValue() {
        assertThat(p).matches("update tab set x = default;")
    }


    @Test
    fun matchesUpdateWithErrorLoggingClause() {
        assertThat(p).matches("update tab set col = 1 where id = 2 log errors into err\$_tab (tag) reject limit unlimited;")
        assertThat(p).matches("update tab set col = 1 log errors;")
    }

    @Test
    fun matchesUpdateOfColumnNamedBegin() {
        assertThat(p).matches("update tab set begin = fix;")
    }

    @Test
    fun doesNotMatchUpdateWithErrorLoggingBeforeWhere() {
        assertThat(p).notMatches("update tab set col = 1 log errors where id = 2;")
    }
}
