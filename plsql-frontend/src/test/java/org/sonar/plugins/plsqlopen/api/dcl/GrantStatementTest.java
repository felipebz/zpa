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
package org.sonar.plugins.plsqlopen.api.dcl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DclGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class GrantStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DclGrammar.GRANT_STATEMENT);
    }
    
    @Test
    public void matchesSimpleSystemGrant() {
        assertThat(p).matches("grant connect to user1;");
    }
    
    @Test
    public void matchesGrantToPublic() {
        assertThat(p).matches("grant connect to public;");
    }
    
    @Test
    public void matchesSystemGrantWithLongPrivilegeName() {
        assertThat(p).matches("grant execute any procedure to user1;");
    }
    
    @Test
    public void matchesSimpleSystemGrantAllPrivileges() {
        assertThat(p).matches("grant all privileges to user1;");
    }
    
    @Test
    public void matchesMultipleRoleGrantToUser() {
        assertThat(p).matches("grant connect, resource to user1;");
    }
    
    @Test
    public void matchesSystemGrantWithGranteeIdentifiedBy() {
        assertThat(p).matches("grant connect to user1 identified by pass;");
    }
    
    @Test
    public void matchesSystemGrantWithMultipleGranteeIdentifiedBy() {
        assertThat(p).matches("grant connect to user1, user2 identified by pass1, pass2;");
    }
    
    @Test
    public void matchesSystemGrantWithAdminOption() {
        assertThat(p).matches("grant foo to bar with admin option;");
    }
    
    @Test
    public void matchesSystemGrantWithDelegateOption() {
        assertThat(p).matches("grant foo to bar with delegate option;");
    }
    
    @Test
    public void matchesSystemGrantInCurrentContainer() {
        assertThat(p).matches("grant foo to bar container = current;");
    }
    
    @Test
    public void matchesSystemGrantInAllContainers() {
        assertThat(p).matches("grant foo to bar container = all;");
    }
    
    @Test
    public void matchesLongSystemGrant() {
        assertThat(p).matches("grant foo to bar with admin option container = all;");
    }
    
    @Test
    public void matchesSimpleGrantOnObject() {
        assertThat(p).matches("grant execute on proc to user1;");
    }
    
    @Test
    public void matchesGrantOnObjectToPublic() {
        assertThat(p).matches("grant execute on proc to public;");
    }
    
    @Test
    public void matchesGrantWithColumnOnObject() {
        assertThat(p).matches("grant select (col) on tab to user1;");
    }
    
    @Test
    public void matchesGrantWithMultipleColumnsOnObject() {
        assertThat(p).matches("grant select (col, col2) on tab to user1;");
    }
    
    @Test
    public void matchesGrantOnObjectWithHierarchyOption() {
        assertThat(p).matches("grant execute on proc to user1 with hierarchy option;");
    }
    
    @Test
    public void matchesGrantOnObjectWithGrantOption() {
        assertThat(p).matches("grant execute on proc to user1 with grant option;");
    }
    
    @Test
    public void matchesGrantOnObjectWithHierarchyAndGrantOption() {
        assertThat(p).matches("grant execute on proc to user1 with hierarchy option with grant option;");
    }
    
    @Test
    public void matchesGrantRoleToFunction() {
        assertThat(p).matches("grant foo to function bar;");
    }
    
    @Test
    public void matchesGrantRoleToProcedure() {
        assertThat(p).matches("grant foo to procedure bar;");
    }
    
    @Test
    public void matchesGrantRoleToPackage() {
        assertThat(p).matches("grant foo to package bar;");
    }
    
    @Test
    public void matchesGrantRoleToMultiplePrograms() {
        assertThat(p).matches("grant foo, bar to package foo2, procedure bar2;");
    }

}
