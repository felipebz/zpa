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
package org.sonar.plugins.plsqlopen.api.ddl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class OutOfLineConstraintTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.OUT_OF_LINE_CONSTRAINT);
    }
    
    @Test
    public void matchesUnique() {
        assertThat(p).matches("unique (foo)");
    }
    
    @Test
    public void matchesUniqueWithMoreColumns() {
        assertThat(p).matches("unique (foo, bar, baz)");
    }
    
    @Test
    public void matchesPrimaryKey() {
        assertThat(p).matches("primary key (foo)");
    }
    
    @Test
    public void matchesPrimaryKeyWithMoreColumns() {
        assertThat(p).matches("primary key (foo, bar, baz)");
    }
    
    @Test
    public void matchesForeignKey() {
        assertThat(p).matches("foreign key (col) references tab (col)");
    }
    
    @Test
    public void matchesReferencesWithMoreColumns() {
        assertThat(p).matches("foreign key (col, col2, col3) references tab (col, col2, col3)");
    }
    
    @Test
    public void matchesCheck() {
        assertThat(p).matches("check (x > 1)");
    }
    
    @Test
    public void matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key (foo)");
    }

}
