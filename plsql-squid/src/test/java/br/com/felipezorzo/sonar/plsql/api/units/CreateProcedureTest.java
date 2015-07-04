package br.com.felipezorzo.sonar.plsql.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

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
        assertThat(p).matches("create procedure test is language java 'javatest';");
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

}
