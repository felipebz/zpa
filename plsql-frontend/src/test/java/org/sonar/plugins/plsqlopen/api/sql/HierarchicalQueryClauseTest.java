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

public class HierarchicalQueryClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DmlGrammar.HIERARCHICAL_QUERY_CLAUSE);
    }
    
    @Test
    public void matchesSimpleHierarchical() {
        assertThat(p).matches("connect by foo = bar");
    }
    
    @Test
    public void matchesHierarchicalQueryConnectByFirst() {
        assertThat(p).matches("connect by foo = bar start with foo = bar");
    }
    
    @Test
    public void matchesHierarchicalQueryStartWithFirst() {
        assertThat(p).matches("start with foo = bar connect by foo = bar");
    }
    
    @Test
    public void notMatchesStartWithFirstWithoutConnectBy() {
        assertThat(p).notMatches("start with foo = bar");
    }

}
