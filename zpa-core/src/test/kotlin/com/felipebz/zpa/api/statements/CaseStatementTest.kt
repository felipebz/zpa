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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CaseStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CASE_STATEMENT)
    }

    @Test
    fun matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then foo := bar; end case;")
    }

    @Test
    fun matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then foo := bar; when 2 then foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then foo := bar; else foo := bar; end case;")
    }

    @Test
    fun matchesCaseWithMultipleStataments() {
        assertThat(p).matches("case when x = 1 then foo := bar; bar := baz; end case;")
    }

    @Test
    fun matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesLabeledCase() {
        assertThat(p).matches("<<foo>> case when x = 1 then foo := bar; end case foo;")
    }

    @Test
    fun matchesCaseWithSelectorExpression() {
        assertThat(p).matches("case foo + bar when 1 then foo := bar; end case;")
    }

    @Test
    fun matchesBooleanExpressionSearchedCase() {
        assertThat(p).matches("case when foo is not null and bar is null then foo := bar; end case;")
    }


}
