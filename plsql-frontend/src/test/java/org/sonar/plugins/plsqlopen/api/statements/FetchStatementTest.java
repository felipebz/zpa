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
package org.sonar.plugins.plsqlopen.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class FetchStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FETCH_STATEMENT);
    }
    
    @Test
    public void matchesSimpleFetchInto() {
        assertThat(p).matches("fetch foo into bar;");
    }
    
    @Test
    public void matchesFetchCursorInPackage() {
        assertThat(p).matches("fetch pack.foo into bar;");
    }
    
    @Test
    public void matchesFetchHostCursorInto() {
        assertThat(p).matches("fetch :foo into bar;");
    }
    
    @Test
    public void matchesFetchIntoMultipleVariables() {
        assertThat(p).matches("fetch foo into bar, baz;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectInto() {
        assertThat(p).matches("fetch foo bulk collect into bar;");
    }
    
    @Test
    public void matchesFetchHostCursorBulkCollectInto() {
        assertThat(p).matches("fetch :foo bulk collect into bar;");
    }
    
    @Test
    public void matchesFetchBulkCollectIntoMultipleVariables() {
        assertThat(p).matches("fetch foo bulk collect into bar, baz;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectIntoHostVariable() {
        assertThat(p).matches("fetch foo bulk collect into :bar;");
    }
    
    @Test
    public void matchesSimpleFetchBulkCollectIntoWithLimit() {
        assertThat(p).matches("fetch foo bulk collect into bar limit 10;");
    }
    
    @Test
    public void matchesLabeledFetchInto() {
        assertThat(p).matches("<<foo>> fetch foo into bar;");
    }

}
