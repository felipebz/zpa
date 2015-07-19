package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class FunctionDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.FUNCTION_DECLARATION);
    }

    @Test
    public void matchesSimpleFunction() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesSimpleFunctionAlternative() {
        assertThat(p).matches(""
                + "function test return number as\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithParameter() {
        assertThat(p).matches(""
                + "function test(parameter in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithMultipleParameters() {
        assertThat(p).matches(""
                + "function test(parameter1 in number, parameter2 in number) return number is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithVariableDeclaration() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "var number;"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesFunctionWithMultipleVariableDeclaration() {
        assertThat(p).matches(""
                + "function test return number is\n"
                + "var number;"
                + "var2 number;"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }
    
    @Test
    public void matchesDeterministicFunction() {
        assertThat(p).matches(""
                + "function test return number deterministic is\n"
                + "begin\n"
                + "return 0;\n"
                + "end;");
    }

}
