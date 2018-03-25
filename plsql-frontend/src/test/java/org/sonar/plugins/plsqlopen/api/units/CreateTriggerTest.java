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
package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateTriggerTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_TRIGGER);
    }
    
    @Test
    public void matchesSimpleTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before insert on tab "
                + "begin null; end;");
    }
    
    @Test
    public void matchesSimpleCreateOrReplaceTrigger() {
        assertThat(p).matches(""
                + "create or replace trigger foo "
                + "before insert on tab "
                + "begin null; end;");
    }
    
    @Test
    public void matchesEditionableTrigger() {
        assertThat(p).matches(""
                + "create editionable trigger foo "
                + "before insert on tab "
                + "begin null; end;");
    }
    
    @Test
    public void matchesNoneditionableTrigger() {
        assertThat(p).matches(""
                + "create noneditionable trigger foo "
                + "before insert on tab "
                + "begin null; end;");
    }
    
    @Test
    public void matchesTriggerWithDeclareWithoutDeclarations() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before insert on tab "
                + "declare "
                + "begin null; end;");
    }
    
    @Test
    public void matchesSystemDdlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "before alter or analyze or associate statistics or audit or comment or create "
                + "on foo.schema "
                + "begin null; end;");
    }  
    
    @Test
    public void matchesSystemDatabaseTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "after startup or before logoff or after db_role_change "
                + "on pluggable database "
                + "begin null; end;");
    }   
    
    @Test
    public void matchesInsteadOfDmlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "instead of insert or update on foo.bar "
                + "for each row enable "
                + "begin null; end;");
    }   
        
    @Test
    public void matchesCompoundDmlTrigger() {
        assertThat(p).matches(""
                + "create trigger foo "
                + "for insert or update of bar on foo.bar "
                + "compound trigger "
                + "before each row is "
                + "begin null; end before each row;"
                + "after each row is "
                + "begin null; end after each row;"
                + "after statement is "
                + "begin null; end after statement;"
                + "end foo;");
    }  
}
