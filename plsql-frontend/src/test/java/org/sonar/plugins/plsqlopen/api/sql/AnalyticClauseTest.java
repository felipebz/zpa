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
package org.sonar.plugins.plsqlopen.api.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class AnalyticClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DmlGrammar.ANALYTIC_CLAUSE);
    }

    @Test
    public void matchesSimpleOver() {
        assertThat(p).matches("over ()");
    }
    
    @Test
    public void matchesOverPartitionBy() {
        assertThat(p).matches("over (partition by foo)");
    }
    
    @Test
    public void matchesOverPartitionByWithMultipleExpressions() {
        assertThat(p).matches("over (partition by foo, bar, baz)");
    }
    
    @Test
    public void matchesOverOrderBy() {
        assertThat(p).matches("over (order by foo)");
    }
    
    @Test
    public void matchesOverWithWindowingUnboundedPreceding() {
        assertThat(p).matches("over (order by foo rows unbounded preceding)");
        assertThat(p).matches("over (order by foo range unbounded preceding)");
    }
    
    @Test
    public void matchesOverWithWindowingCurrentRow() {
        assertThat(p).matches("over (order by foo rows current row)");
    }
    
    @Test
    public void matchesOverWithWindowingExpressionPreceding() {
        assertThat(p).matches("over (order by foo rows 1 preceding)");
    }
    
    @Test
    public void matchesOverWithWindowingWithBetween() {
        assertThat(p).matches("over (order by foo rows between unbounded preceding and current row)");
    }
    
    @Test
    public void matchesLongAnalyticClause() {
        assertThat(p).matches("over (partition by foo order by foo rows between unbounded preceding and unbounded following)");
    }
    
    @Test
    public void matchesOverPartitionByExpression() {
        assertThat(p).matches("over (partition by foo + bar)");
    }

}
