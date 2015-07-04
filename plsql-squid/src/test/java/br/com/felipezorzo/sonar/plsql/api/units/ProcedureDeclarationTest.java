package br.com.felipezorzo.sonar.plsql.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ProcedureDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.PROCEDURE_DECLARATION);
    }

    @Test
    public void matchesSimpleProcedure() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimpleProcedureAlternative() {
        assertThat(p).matches(""
                + "procedure test as\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithParameter() {
        assertThat(p).matches(""
                + "procedure test(parameter in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithMultipleParameters() {
        assertThat(p).matches(""
                + "procedure test(parameter1 in number, parameter2 in number) is\n"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithVariableDeclaration() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "var number;"
                + "begin\n"
                + "null;\n"
                + "end;");
    }
    
    @Test
    public void matchesProcedureWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "procedure test is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "null;\n"
                + "end;");
    }

}
