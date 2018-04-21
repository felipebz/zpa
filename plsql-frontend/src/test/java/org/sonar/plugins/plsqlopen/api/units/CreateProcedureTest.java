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

public class CreateProcedureTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_PROCEDURE);
    }

    @Test
    public void matchesSimpleProcedure() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimpleProcedureAlternative() {
        assertThat(p).matches(""
                + "create procedure test as\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }

    @Test
    public void matchesEditionableProcedure() {
        assertThat(p).matches(""
            + "create editionable procedure test is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesNonEditionableProcedure() {
        assertThat(p).matches(""
            + "create noneditionable procedure test is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesProcedureWithSharingMetadata() {
        assertThat(p).matches(""
            + "create procedure test sharing = metadata is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesProcedureWithSharingNone() {
        assertThat(p).matches(""
            + "create procedure test sharing = none is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesProcedureWithDefaultCollation() {
        assertThat(p).matches(""
            + "create procedure test default collation using_nls_comp is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesProcedureWithAccesibleBy() {
        assertThat(p).matches(""
            + "create procedure test accessible by (function func) is\n"
            + "begin\n"
            + "null;\n"
            + "end;");
    }

    @Test
    public void matchesSimpleCreateOrReplaceProcedure() {
        assertThat(p).matches(""
                + "create or replace procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithSchema() {
        assertThat(p).matches(""
                + "create procedure schema.test is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
   
    @Test
    public void matchesProcedureWithParameter() {
        assertThat(p).matches(""
                + "create procedure test(parameter in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithMultipleParameters() {
        assertThat(p).matches(""
                + "create procedure test(parameter1 in number, parameter2 in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create procedure test authid current_user is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create procedure test authid definer is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithJavaCallSpec() {
        assertThat(p).matches("create procedure test is language java name 'javatest';");
    }
    
    @Test
    public void matchesExternalProcedure() {
        assertThat(p).matches("create procedure test is external;");
    }
    
    @Test
    public void matchesProcedureWithVariableDeclaration() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "var number;"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "create procedure test is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithTimestamp() {
        assertThat(p).matches(""
                + "create procedure test timestamp '2015-01-01' is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }

}
