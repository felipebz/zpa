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

class DeleteStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.DELETE_STATEMENT)
    }

    @Test
    fun matchesSimpleDelete() {
        assertThat(p).matches("delete tab;")
    }

    @Test
    fun matchesDeleteFrom() {
        assertThat(p).matches("delete from tab;")
    }

    @Test
    fun matchesDeleteWithWhere() {
        assertThat(p).matches("delete from tab where x = 1;")
    }

    @Test
    fun matchesDeleteWithAlias() {
        assertThat(p).matches("delete tab t;")
    }

    @Test
    fun matchesDeleteWithSchema() {
        assertThat(p).matches("delete sch.tab;")
    }

    @Test
    fun matchesDeleteCurrentOf() {
        assertThat(p).matches("delete tab where current of cur;")
    }

    @Test
    fun matchesLabeledDelete() {
        assertThat(p).matches("<<foo>> delete tab;")
    }

    @Test
    fun matchesDeleteFromQuery() {
        assertThat(p).matches("delete (select * from dual);")
    }

    @Test
    fun matchesDeleteWithReturningInto() {
        assertThat(p).matches("delete from tab returning x bulk collect into y;")
    }

    @Test
    fun matchesDeleteWithErrorLoggingClause() {
        assertThat(p).matches("delete from tab where id = 1 log errors into err\$_tab (tag) reject limit 100;")
        assertThat(p).matches("delete from tab log errors;")
    }

    @Test
    fun doesNotMatchDeleteWithErrorLoggingBeforeWhere() {
        assertThat(p).notMatches("delete from tab log errors where id = 1;")
    }
}
