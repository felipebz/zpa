/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class NumericExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesNumericAddition() {
        assertThat(p).matches("1 + 1");
        assertThat(p).matches("1+1");
    }
    
    @Test
    public void matchesNumericSubtraction() {
        assertThat(p).matches("1 - 1");
        assertThat(p).matches("1-1");
    }
    
    @Test
    public void matchesNumericMultiplication() {
        assertThat(p).matches("1 * 1");
    }
    
    @Test
    public void matchesNumericDivision() {
        assertThat(p).matches("1 / 1");
    }
    
    @Test
    public void matchesNumericExponentiation() {
        assertThat(p).matches("1 ** 1");
    }
    
    @Test
    public void matchesModulo() {
        assertThat(p).matches("1 mod 1");
    }
    
    @Test
    public void matchesMathematicalOperationBetweenTwoQueries() {
        assertThat(p).matches("(select 1 from dual) + (select 1 from dual)");
    }
    
    @Test
    public void matchesMathematicalOperationBetweenTwoCaseExpressions() {
        assertThat(p).matches("(case when 1 = 1 then 1 end) + (case when 1 = 1 then 1 end)");
    }
    
    @Test
    public void matchesCursorRowcount() {
        assertThat(p).matches("cur%rowcount + 1");
    }
    
    @Test
    public void matchesHostCursorRowcount() {
        assertThat(p).matches(":cur%rowcount + 1");
    }
    
    @Test
    public void matchesSqlRowcount() {
        assertThat(p).matches("sql%rowcount + 1");
    }
    
    @Test
    public void matchesSqlBulkRowcount() {
        assertThat(p).matches("sql%bulk_rowcount(1) + 1");
    }
    
    @Test
    public void matchesHostVariableExpression() {
        assertThat(p).matches(":var + 1");
    }
    
    @Test
    public void matchesIndicatorVariableExpression() {
        assertThat(p).matches(":var:indicator + 1");
    }
    
    @Test
    public void matchesVariableExpression() {
        assertThat(p).matches("var + 1");
    }
    
    @Test
    public void matchesFunctionCallExpression() {
        assertThat(p).matches("func(var) + 1");
    }
    
    @Test
    public void matchesCollectionCount() {
        assertThat(p).matches("collection.count");
    }
    
    @Test
    public void matchesCollectionFirst() {
        assertThat(p).matches("collection.first + 1");
    }
    
    @Test
    public void matchesCollectionLast() {
        assertThat(p).matches("collection.last + 1");
    }
    
    @Test
    public void matchesCollectionLimit() {
        assertThat(p).matches("collection.limit + 1");
    }
    
    @Test
    public void matchesCollectionNext() {
        assertThat(p).matches("collection.next(1) + 1");
    }
    
    @Test
    public void matchesCollectionPrior() {
        assertThat(p).matches("collection.prior(1) + 1");
    }
    
}
