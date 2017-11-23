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
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class TableColumnDefinitionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.TABLE_COLUMN_DEFINITION);
    }
    
    @Test
    public void matchesSimpleDefinition() {
        assertThat(p).matches("id number");
    }
    
    @Test
    public void matchesSort() {
        assertThat(p).matches("id number sort");
    }
    
    @Test
    public void matcheseDefault() {
        assertThat(p).matches("id number default 1");
    }
    
    @Test
    public void matchesSimpleEncrypt() {
        assertThat(p).matches("id number encrypt");
    }
    
    @Test @Ignore
    public void matchesEncryptWithSpecification() {
        assertThat(p).matches("id number encrypt using 'AES256'");
    }
    
    @Test
    public void matchesInlineConstraint() {
        assertThat(p).matches("id number constraint pktab primary key");
    }
    
    @Test
    public void matchesMultipleConstraints() {
        assertThat(p).matches("id number constraint pktab primary key check (id > 0)");
    }
    
    @Test @Ignore
    public void matchesInlineRefConstraint() {
        assertThat(p).matches("id number with rowid");
    }

}
