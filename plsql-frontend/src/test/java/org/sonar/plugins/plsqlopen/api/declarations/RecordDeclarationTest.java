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
package org.sonar.plugins.plsqlopen.api.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class RecordDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.RECORD_DECLARATION);
    }

    @Test
    public void matchesSimpleRecord() {
        assertThat(p).matches("type foo is record(x number);");
    }
    
    @Test
    public void matchesRecordWithMultipleFields() {
        assertThat(p).matches("type foo is record(x number, y number);");
    }
    
    @Test
    public void matchesRecordWithDefaultField() {
        assertThat(p).matches("type foo is record(x number default 1);");
    }
    
    @Test
    public void matchesRecordWithDefaultFieldAlternative() {
        assertThat(p).matches("type foo is record(x number := 1);");
    }
    
    @Test
    public void matchesRecordWithNotNullField() {
        assertThat(p).matches("type foo is record(x number not null default 1);");
    }

}
