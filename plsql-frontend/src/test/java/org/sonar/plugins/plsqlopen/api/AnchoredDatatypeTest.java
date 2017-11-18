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
package org.sonar.plugins.plsqlopen.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

public class AnchoredDatatypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ANCHORED_DATATYPE);
    }

    @Test
    public void matchesSimpleTypeAttribute() {
        assertThat(p).matches("name%type");
    }
    
    @Test
    public void matchesTableColumn() {
        assertThat(p).matches("tab.name%type");
    }
    
    @Test
    public void matchesTableColumnWithExplicitSchema() {
        assertThat(p).matches("schema.tab.name%type");
    }
    
    @Test
    public void matchesSimpleRowtypeAttribute() {
        assertThat(p).matches("name%rowtype");
    }
    
    @Test
    public void matchesTypeAsColumnName() {
        assertThat(p).matches("tab.type%type");
    }

}
