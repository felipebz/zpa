//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DmlTableExpressionClauseTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)
    }

    @Test
    fun matchesTable() {
        assertThat(p).matches("tab")
    }

    @Test
    fun matchesTableInSchema() {
        assertThat(p).matches("sch.tab")
    }

    @Test
    fun matchesTableWithDbLink() {
        assertThat(p).matches("tab@link")
        assertThat(p).matches("tab@link.domain.com")
    }

    @Test
    fun matchesTableWithAlias() {
        assertThat(p).matches("tab alias")
        assertThat(p).matches("tab as alias")
        assertThat(p).matches("tab log")
    }

    @Test
    fun matchesSubquery() {
        assertThat(p).matches("(select 1 from dual)")
    }

    @Test
    fun matchesFunctionCall() {
        assertThat(p).matches("func(var)")
        assertThat(p).matches("table(xmlsequence(x))")
    }

    @Test
    fun matchesNestedClause() {
        assertThat(p).matches("tab nested doc columns (foo path foo) alias")
    }

    @Test
    fun matchesNestedClauseWithExplicitPath() {
        assertThat(p).matches("tab nested path doc columns (foo path foo)")
    }

    @Test
    fun matchesNestedClauseWithRelativeObjectAccess() {
        assertThat(p).matches("tab nested doc.c1[*].c2 columns (foo path foo)")
    }

    @Test
    fun matchesNestedClauseWithSimplePathAccess() {
        assertThat(p).matches("tab nested doc, '$.c1[*].c2' columns (foo path foo)")
    }


    @Test
    fun matchesTableFunctionCallWithoutTableOperator() {
        assertThat(p).matches("apps.pkg.get_lines()")
        assertThat(p).matches("pkg.get_lines(1) l")
    }

    @Test
    fun matchesParenthesizedTableReference() {
        assertThat(p).matches("((select id from foo) bar)")
        assertThat(p).matches("(foo bar)")
    }

    @Test
    fun matchesOuterJoinedLateralInlineView() {
        assertThat(p).matches("lateral (select id from foo)(+) bar")
    }

    @Test
    fun matchesAliasBeforeUnpivot() {
        assertThat(p).matches("tab st unpivot exclude nulls (val for col in (a as 'A')) stt")
    }

    @Test
    fun doesNotMatchAnEmptyParenthesizedTableReference() {
        assertThat(p).notMatches("()")
    }
}
