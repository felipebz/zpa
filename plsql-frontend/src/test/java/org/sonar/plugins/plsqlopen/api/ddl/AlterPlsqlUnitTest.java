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

public class AlterPlsqlUnitTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.ALTER_PLSQL_UNIT);
    }
    
    @Test
    public void matchesAlterTriggerDisable() {
        assertThat(p).matches("alter trigger foo disable;");
    }
    
    @Test
    public void matchesAlterTriggerEnable() {
        assertThat(p).matches("alter trigger foo enable;");
    }
    
    @Test
    public void matchesAlterTriggerDisableWithSchema() {
        assertThat(p).matches("alter trigger foo.bar disable;");
    }
    
    @Test
    public void matchesAlterTriggerDisableWithQuotedIdentifier() {
        assertThat(p).matches("alter trigger \"foo\" rename to \"bar\";");
    }
    
    @Test
    public void matchesAlterTriggerCompile() {
        assertThat(p).matches("alter trigger foo compile;");
    }
    
    @Test
    public void matchesAlterProcedureCompile() {
        assertThat(p).matches("alter procedure foo compile;");
    }
    
    @Test
    public void matchesAlterFunctionCompile() {
        assertThat(p).matches("alter function foo compile;");
    }
    
    @Test
    public void matchesAlterPackageCompile() {
        assertThat(p).matches("alter package foo compile;");
    }
    
    @Test
    public void matchesAlterPackageCompileBody() {
        assertThat(p).matches("alter package foo compile body;");
    }
    
    @Test
    public void matchesAlterPackageCompileBodyDebug() {
        assertThat(p).matches("alter package foo compile debug body;");
    }
    
    @Test
    public void matchesAlterPackageCompilePackage() {
        assertThat(p).matches("alter package foo compile package;");
    }
    
    @Test
    public void matchesAlterPackageCompileSpecification() {
        assertThat(p).matches("alter package foo compile specification;");
    }
    
    @Test
    public void matchesAlterPackageCompileSpecificationReuseSettings() {
        assertThat(p).matches("alter package foo compile debug specification reuse settings;");
    }
}
