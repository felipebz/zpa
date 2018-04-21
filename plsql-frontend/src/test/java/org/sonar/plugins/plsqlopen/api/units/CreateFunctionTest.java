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

public class CreateFunctionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_FUNCTION);
    }

    @Test
    public void matchesSimpleFunction() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimpleFunctionWithNameRefresh() {
    	assertThat(p).matches(""
    			+ "create function refresh return number is\n"
    			+ "begin\n"
    			+ "return 0;\n"
    			+ "end;");
    }
    
    @Test
    public void matchesSimpleFunctionAlternative() {
        assertThat(p).matches(""
                + "create function test return number as\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }

    @Test
    public void matchesEditionableFunction() {
        assertThat(p).matches(""
            + "create editionable function test return number is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }

    @Test
    public void matchesNonEditionableFunction() {
        assertThat(p).matches(""
            + "create noneditionable function test return number is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }

    @Test
    public void matchesFunctionWithSharingMetadata() {
        assertThat(p).matches(""
            + "create function test return number sharing = metadata is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }

    @Test
    public void matchesFunctionWithSharingNone() {
        assertThat(p).matches(""
            + "create editionable function test return number sharing = none is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }
    
    @Test
    public void matchesSimpleCreateOrReplaceFunction() {
        assertThat(p).matches(""
                + "create or replace function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithSchema() {
        assertThat(p).matches(""
                + "create function schema.test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
   
    @Test
    public void matchesFunctionWithParameter() {
        assertThat(p).matches(""
                + "create function test(parameter in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithMultipleParameters() {
        assertThat(p).matches(""
                + "create function test(parameter1 in number, parameter2 in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithAuthidCurrentUser() {
        assertThat(p).matches(""
                + "create function test return number authid current_user is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithAuthidDefiner() {
        assertThat(p).matches(""
                + "create function test return number authid definer is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }

    @Test
    public void matchesFunctionWithDefaultCollation() {
        assertThat(p).matches(""
            + "create function test return number default collation using_nls_comp is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }

    @Test
    public void matchesFunctionWithAccessibleBy() {
        assertThat(p).matches(""
            + "create function test return number accessible by (procedure proc) is\n"
            + "begin\n"
            + "return 0;\n"
            + "end;");
    }
    
    @Test
    public void matchesFunctionWithJavaCallSpec() {
        assertThat(p).matches("create function test return number is language java name 'javatest';");
    }
    
    @Test
    public void matchesFunctionWithVariableDeclaration() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "var number;"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "create function test return number is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesDeterministicFunction() {
        assertThat(p).matches(""
                + "create function test return number deterministic is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesPipelinedFunction() {
        assertThat(p).matches(""
                + "create function test return number pipelined is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesParallelEnableFunction() {
        assertThat(p).matches(""
                + "create function test return number parallel_enable is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesResultCacheFunction() {
        assertThat(p).matches(""
                + "create function test return number result_cache is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesResultCacheWithReliesOnFunction() {
        assertThat(p).matches(""
                + "create function test return number result_cache relies_on(tbl_test1, tbl_test2) is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithTimestamp() {
        assertThat(p).matches(""
                + "create function test timestamp '2015-01-01' return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesAggregateFunction() {
        assertThat(p).matches("create function test return varchar2 aggregate using foo.bar;");
    }

}
