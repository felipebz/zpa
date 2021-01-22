/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

class NumericExpressionTest : RuleTest() {

    @Before
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesNumericAddition() {
        assertThat(p).matches("1 + 1")
        assertThat(p).matches("1+1")
    }

    @Test
    fun matchesNumericSubtraction() {
        assertThat(p).matches("1 - 1")
        assertThat(p).matches("1-1")
    }

    @Test
    fun matchesNumericMultiplication() {
        assertThat(p).matches("1 * 1")
    }

    @Test
    fun matchesNumericDivision() {
        assertThat(p).matches("1 / 1")
    }

    @Test
    fun matchesNumericExponentiation() {
        assertThat(p).matches("1 ** 1")
    }

    @Test
    fun matchesModulo() {
        assertThat(p).matches("1 mod 1")
    }

    @Test
    fun matchesMathematicalOperationBetweenTwoQueries() {
        assertThat(p).matches("(select 1 from dual) + (select 1 from dual)")
    }

    @Test
    fun matchesMathematicalOperationBetweenTwoCaseExpressions() {
        assertThat(p).matches("(case when 1 = 1 then 1 end) + (case when 1 = 1 then 1 end)")
    }

    @Test
    fun matchesCursorRowcount() {
        assertThat(p).matches("cur%rowcount + 1")
    }

    @Test
    fun matchesHostCursorRowcount() {
        assertThat(p).matches(":cur%rowcount + 1")
    }

    @Test
    fun matchesSqlRowcount() {
        assertThat(p).matches("sql%rowcount + 1")
    }

    @Test
    fun matchesSqlBulkRowcount() {
        assertThat(p).matches("sql%bulk_rowcount(1) + 1")
    }

    @Test
    fun matchesHostVariableExpression() {
        assertThat(p).matches(":var + 1")
    }

    @Test
    fun matchesIndicatorVariableExpression() {
        assertThat(p).matches(":var:indicator + 1")
    }

    @Test
    fun matchesVariableExpression() {
        assertThat(p).matches("var + 1")
    }

    @Test
    fun matchesFunctionCallExpression() {
        assertThat(p).matches("func(var) + 1")
    }

    @Test
    fun matchesCollectionCount() {
        assertThat(p).matches("collection.count")
    }

    @Test
    fun matchesCollectionFirst() {
        assertThat(p).matches("collection.first + 1")
    }

    @Test
    fun matchesCollectionLast() {
        assertThat(p).matches("collection.last + 1")
    }

    @Test
    fun matchesCollectionLimit() {
        assertThat(p).matches("collection.limit + 1")
    }

    @Test
    fun matchesCollectionNext() {
        assertThat(p).matches("collection.next(1) + 1")
    }

    @Test
    fun matchesCollectionPrior() {
        assertThat(p).matches("collection.prior(1) + 1")
    }

}
