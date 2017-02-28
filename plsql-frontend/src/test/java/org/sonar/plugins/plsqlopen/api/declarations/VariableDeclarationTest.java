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

public class VariableDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.VARIABLE_DECLARATION);
    }

    @Test
    public void matchesSimpleDeclaration() {
        assertThat(p).matches("var number;");
    }
    
    @Test
    public void matchesDeclarationWithPrecision() {
        assertThat(p).matches("var number(1);");
    }
    
    @Test
    public void matchesDeclarationWithPrecisionAndScale() {
        assertThat(p).matches("var number(1,1);");
    }
    
    @Test
    public void matchesDeclarationWithInitialization() {
        assertThat(p).matches("var number := 1;");
        assertThat(p).matches("var varchar2(1) := 'a';");
        assertThat(p).matches("var boolean := true;");
        assertThat(p).matches("var boolean := var2;");
    }
    
    @Test
    public void matchesDeclarationWithDefaultValue() {
        assertThat(p).matches("var number default 1;");
        assertThat(p).matches("var varchar2(1) default 'a';");
        assertThat(p).matches("var boolean default true;");
        assertThat(p).matches("var boolean default var2;");
    }
    
    @Test
    public void matchesDeclarationWithNotNullConstraint() {
        assertThat(p).matches("var number not null := 1;");
        assertThat(p).matches("var number not null default 1;");
    }
        
    @Test
    public void matchesDeclarationExplicitNullable() {
        assertThat(p).matches("var number null := 1;");
        assertThat(p).matches("var number null default 1;");
    }
    
    @Test
    public void matchesTypeAnchoredDeclaration() {
        assertThat(p).matches("var custom%type;");
    }
    
    @Test
    public void matchesObjectDeclaration() {
        assertThat(p).matches("var custom;");
    }
    
    @Test
    public void matchesObjectDeclarationWithPackage() {
        assertThat(p).matches("var pack.custom;");
    }
    
    @Test
    public void matchesObjectDeclarationWithPackageAndSchema() {
        assertThat(p).matches("var sch.pack.custom;");
    }

    @Test
    public void matchesTableAnchoredDeclaration() {
        assertThat(p).matches("var tab%rowtype;");
    }
    
    @Test
    public void matchesTableColumnAnchoredDeclaration() {
        assertThat(p).matches("var tab.column%type;");
    }
    
    @Test
    public void matchesRefObjectDeclaration() {
        assertThat(p).matches("var ref custom;");
    }
    
    @Test
    public void matchesSimpleConstant() {
        assertThat(p).matches("var constant number := 1;");
        assertThat(p).matches("var constant number default 1;");
    }
    
    @Test
    public void matchesSimpleConstantWithConstraints() {
        assertThat(p).matches("var constant number not null := 1;");
        assertThat(p).matches("var constant number not null default 1;");
    }
    
    @Test
    public void matchesRemoteTableColumnAnchoredDeclaration() {
        assertThat(p).matches("var tab.col@sch.foo%type;");
    }
    
    @Test
    public void matchesExceptionDeclaration() {
        assertThat(p).matches("var exception;");
    }
    
}
