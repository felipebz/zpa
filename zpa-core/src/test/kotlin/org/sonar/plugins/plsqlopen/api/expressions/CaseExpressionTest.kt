/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api.expressions

import org.junit.Before
import org.junit.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest
import org.sonar.sslr.tests.Assertions.assertThat

class CaseExpressionTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.CASE_EXPRESSION)
    }

    @Test
    fun matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then 1 end")
    }

    @Test
    fun matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then 1 end")
    }

    @Test
    fun matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then 1 when 2 then 2 end")
    }

    @Test
    fun matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then 1 else 2 end")
    }

    @Test
    fun matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then 1 end")
    }

    @Test
    fun matchesCaseWithSelectorExpression() {
        assertThat(p).matches("case foo + bar when 1 then 1 end")
    }

    @Test
    fun matchesBooleanSearchedCase() {
        assertThat(p).matches("case when foo is not null and bar is null then 1 end")
    }

}
